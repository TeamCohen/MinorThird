package edu.cmu.minorthird.classify;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.classify.experiments.Evaluation.Matrix;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/** 
 * A Tweaked Learner, with an optimization of the precision vs. recall
 * 
 * @author Giora Unger
 * Created on May 19, 2005
 *
 * A learner whose score was optimized according to an F_beta() function,
 * for a given beta. This optimization is used to fine-tune the precision
 * vs. recall for the underlying classification algorithm.
 * Values of beta<1.0 favor precision over recall, while values of
 * beta>1.0 favor recall over precision. beta=1.0 grants equal weight 
 * to both precision and recall.  
 *  
 * <p>Reference: Jason D. M. Rennie,
 * <i>Derivation of the F-Measure</i>,
 * http://people.csail.mit.edu/jrennie/writing/fmeasure.pdf
 */
public class TweakedLearner extends BatchBinaryClassifierLearner{

	// inner learner given to this class during construction
	private BinaryClassifierLearner innerLearner;

	// the beta according to which F_beta is to be maximized
	private double beta;

	// dataset given 
	private Dataset m_dataset;

	// dataset schema
	private ExampleSchema schema;

	// flag indicating whether the given dataset is binary or not
	private boolean isBinary=true;

	// value to be returned if a non-binary dataset is given to 
	// precision() or recall() methods
	private static final int ILLEGAL_VALUE=-1;

	private static final double UNINITIALIZED=-1;

	// actual data structure in which the examples are stored, along with
	// additional fields required for executing the tweaking
	private List<Row> tweakingTable=new ArrayList<Row>();

	// confusion matrix used for efficiently perform the tweaking
	Matrix cm=null;

	// logger for this class
	private static Logger log=Logger.getLogger(TweakedLearner.class);

	/**
	 ******************************************************************** 
	 * Public methods  
	 ******************************************************************** 
	 */

	// TweakedLearner constructor
	public TweakedLearner(BinaryClassifierLearner innerLearner,double beta){
		this.beta=beta;
		this.innerLearner=innerLearner;
	}

	/*
	 * main method of the TweakedLearner class. Recieves a binary
	 * training dataset and then:
	 * 1. Trains on it, based on the innerLearner, namely its inherent
	 *    binary classifier.
	 * 2. Tweaks the classifier (or, more precisely, the model this
	 *    classifier came up with), so that F_beta is maximized. This is
	 *    done by finding a threshold, see details below.
	 * 3. Creates and returns a new TweakedClassifier, with the original
	 *    (inner) classifier and the threshold that was found.
	 */
	@Override
	public Classifier batchTrain(Dataset dataset){

		// make sure the dataset given is indeed binary
		this.schema=dataset.getSchema();
		isBinary=schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
		if(!isBinary) // make sure dataset is binary
		{
			throw new IllegalArgumentException(
					"Dataset given to TweakedLearner::batchTrain must be a binary dataset");
		}
		if(dataset.size()==0) // make sure dataset is not empty
		{
			throw new IllegalArgumentException(
					"Dataset given to TweakedLearner::batchTrain is empty");
		}
		this.m_dataset=dataset;

		// get the classifier resulting from training on the given dataset 
		BinaryClassifier bc=
				(BinaryClassifier)new DatasetClassifierTeacher(m_dataset)
						.train(innerLearner);

		// Initialize the data structure required for the tweaking. Please note
		// that the ExecuteTweaking() method assumes that the rows in this table
		// are sorted by descending score
		initializeTable();

		// Execute actual tweaking - figure out what threshold  works best on the 
		// given dataset w.r.t. F_beta
		double threshold=executeTweaking();

		return new TweakedClassifier(bc,threshold);
	}

	/**
	 ******************************************************************** 
	 * Getters & Setters  
	 ******************************************************************** 
	 */

	/**
	 * @return Returns the beta.
	 */
	public double getBeta(){
		return beta;
	}

	/**
	 * @param beta The beta to set.
	 */
	public void setBeta(double beta){
		this.beta=beta;
	}

	/**
	 * @return Returns the innerLearner.
	 */
	public BinaryClassifierLearner getInnerLearner(){
		return innerLearner;
	}

	/**
	 * @param learner The innerLearner to set.
	 */
	public void setInnerLearner(BinaryClassifierLearner learner){
		this.innerLearner=learner;
	}

	/**
	 ******************************************************************** 
	 * Private methods  
	 ******************************************************************** 
	 */

	// This method initializes tweakingTable, which the data structure used for tweaking
	// It loops over the examples in the given dataset, and insert them into the
	// the table. According to the needs of the tweaking process, the rows are then 
	// sorted by descending score (also called posWeight).
	private void initializeTable(){
		int counter=0;
		for(Iterator<Example> i=m_dataset.iterator();i.hasNext();counter++){
			Example ex=i.next();
			ClassLabel predicted=
					innerLearner.getBinaryClassifier().classification(ex);
			// add example into the tweaking data structure. note that the tweaked
			// prediction given during initialization is NEG for all examples !
			tweakingTable.add(new Row(ex.asInstance(),ex.getLabel(),predicted,
					ClassLabel.negativeLabel(-1.0)));

			// debug code
			//double score = innerLearner.getBinaryClassifier().score(ex);
			/*
			log.debug("Example number: "+ counter + 
					", posWeight: " + predicted.posWeight() + 
					", Score: " + score + 
					", Label: " + ex.getLabel());
			 */
		}

		// sort the table, after it was filled, by descending score
		sortByScore();
	}

	/*
	 *  This method is the very heart of the tweaking process. It assumes that 
	 * the tweakingTable data structure was initilized and filled, with a row 
	 * for every example. It further assumes that all the examples were given an 
	 * initial tweak_prediction of NEG and that they were sorted by descending score 
	 * The method then:
	 * 1. Initialize a confusion matrix, based on a NEG prediction to all examples.
	 * 2. For every example, starting with the on ewith highest positive score:
	 *    a. set the tweak_prediction to POS
	 *    b. update the confusion matrix accordingly
	 *    c. calculate precision, recall and F_beta with the new confusion matrix
	 *       and fill these values in the tweakingtable data structure
	 *   Please note, that in any such iteration, all the examples/rows above
	 *   the current example (including itself) have a POS prediction, while all the
	 *   examples/rows below the current example have a NEG prediction. 
	 *   That is, we prectically evaluate the F_beta when the "dividing line" is on 
	 *   the current example
	 * 3. After all the rows/exmaples are handled, choose the row with the maximal F_beta
	 * 4. Select the score of this row, or more precisely the average between this score
	 *    and the next row's score, to be the threshold.
	 * 5. Return this number as the threshold constituting the new TweakedClassifier.
	 *      
	 */
	private double executeTweaking(){
		double threshold=UNINITIALIZED;
		initConfusionMatrix();

		// for every row, find and fill the precision, recall and F_beta
		// Note, that each row examined is first set to POS
		for(int i=0;i<tweakingTable.size();++i){
			// set dummy prediction of current example to POS
			getRow(i).tweak_predicted=ClassLabel.positiveLabel(1.0);
			// update the confusion matrix based on this prediction change
			updateConfusionMatrix(i);

			// calculate the precision, recall and F_beta with the updated confusion matrix
			getRow(i).precision=getCurrentPrecision();
			getRow(i).recall=getCurrentRecall();
			getRow(i).F_beta=calculateFBeta(getRow(i).precision,getRow(i).recall);

			/*
			log.debug("row " + i + ", precision: " + getRow(i).precision
					+ ", recall: " + getRow(i).recall + ", F_beta: " + getRow(i).F_beta 
					+ ", score: " + getRow(i).orig_predicted.posWeight());
			 */
		}
		// choose the threshold row, that is with maximal F_beta
		// translate its score into the returned threshold
		int index=maxFBetaEntry();

		// if the row that was found is the last row in the table (VERY unlikely),
		// set the threshold to be its score 
		if((index+1)==tweakingTable.size()){
			threshold=getRow(index).orig_predicted.posWeight();
		}else // otherwise, set it to be the average between this row's score and 
		{ // the next row's score
			double maxRowScore=getRow(index).orig_predicted.posWeight();
			double nextRowScore=getRow(index+1).orig_predicted.posWeight();
			threshold=(maxRowScore+nextRowScore)/2;
		}
		log.debug("Threshold found: "+threshold+" (in row "+index+")");

		return threshold; // return the threshold that was found
	}

	/** 
	 * Initializes the confusion matrix. This method is called in the first step
	 * of the tweaking process. Please note that at this step, all the examples
	 * are set to have a tweak_predited field of NEG class.
	 */
	private void initConfusionMatrix(){
		String[] classes=getClasses();
		// count up the errors
		double[][] confused=new double[classes.length][classes.length];
		for(int i=0;i<tweakingTable.size();i++){
			Row row=getRow(i);
			confused[classIndexOf(row.actual)][classIndexOf(row.tweak_predicted)]++;
		}
		cm=new Matrix(confused);
	}

	/*
	 * During the tweaking process, in each iteration a single example
	 * is handled, so that its tweaked_prediction is changed from NEG to POS
	 * This method receives the index (in th etweakingTable) of the current example
	 * and updates the confusion matrix accordingly
	 */
	private void updateConfusionMatrix(int index){
		Row row=getRow(index);
		int actual=classIndexOf(row.actual);
		int p=classIndexOf(ExampleSchema.POS_CLASS_NAME);
		int n=classIndexOf(ExampleSchema.NEG_CLASS_NAME);

		// the confusion matrix (cm) is built as [actual][predicted]
		cm.values[actual][p]++;
		cm.values[actual][n]--;
	}

	// This method simply returns, given precision and recall, the value
	// of F_beta. It uses the "beta" data member of this class, to decide
	// which function is to be calculated
	// The formula used is:
	// F_beta = (beta+1) * precision * recall / 
	// 			(beta * precision) + recall 
	//
	// See also:
	// <p>Reference: Jason D. M. Rennie,
	// <i>Derivation of the F-Measure</i>,
	// http://people.csail.mit.edu/jrennie/writing/fmeasure.pdf
	private double calculateFBeta(double precision,double recall){
		double divisor=((beta*precision)+recall);

		// in case a division by zero will occur, return F_beta=0.0 (instead of NaN)
		if(divisor==0.0){
			log.warn("TweakedLearner::calculateFBeta, divisor of F_beta is zero !!!");
			return 0.0;
		}
		// in case a division by NaN, return F_beta=0.0 (instead of NaN)
		if((new Double(divisor)).isNaN()){
			log
					.warn("TweakedLearner::calculateFBeta, divisor of F_beta is a NaN !!!");
			return 0.0;
		}

		return(((beta+1)*precision*recall)/divisor);
	}

	// This method returns the precision based on the current confusion matrix. 
	// Note that during the tweaking process the confusion matrix is iteratively updated 
	// Precision is defined as:
	// true_positive / (true_positive + false_positive)
	private double getCurrentPrecision(){
		if(!isBinary)
			return ILLEGAL_VALUE; // to be on the safe side

		int p=classIndexOf(ExampleSchema.POS_CLASS_NAME);
		int n=classIndexOf(ExampleSchema.NEG_CLASS_NAME);

		// the confusion matrix (cm) is built as [actual][predicted]
		return cm.values[p][p]/(cm.values[p][p]+cm.values[n][p]);
	}

	// This method returns the recall based on the current confusion matrix. 
	// Note that during the tweaking process the confusion matrix is iteratively updated 
	// Recall is defined as:
	// true_positive / (true_positive + false_negative)
	private double getCurrentRecall(){
		if(!isBinary)
			return ILLEGAL_VALUE; // to be on the safe side

		int p=classIndexOf(ExampleSchema.POS_CLASS_NAME);
		int n=classIndexOf(ExampleSchema.NEG_CLASS_NAME);

		// the confusion matrix (cm) is built as [actual][predicted]
		return cm.values[p][p]/(cm.values[p][p]+cm.values[p][n]);
	}

	/**
	 ******************************************************************** 
	 * Private convenience methods  
	 ******************************************************************** 
	 */
	// sort the tweakingTable, after it was filled, by descending score
	private void sortByScore(){
		Collections.sort(tweakingTable,new Comparator<Row>(){
			@Override
			public int compare(Row a,Row b){
				return MathUtil.sign(b.orig_predicted.posWeight()-a.orig_predicted.posWeight());
			}
		});
	}

	/*
	 * Returns the index (in tweakingTable) of the Row with maximal F_beta value
	 */
	private int maxFBetaEntry(){
		double maxFBeta=ILLEGAL_VALUE; // initialize
		int maxIndex=(int)UNINITIALIZED; // index of the row with maximal F_beta

		for(int i=0;i<tweakingTable.size();++i){
			if(getRow(i).F_beta>maxFBeta){
				maxFBeta=getRow(i).F_beta;
				maxIndex=i;
			}
		}

		if(maxFBeta==ILLEGAL_VALUE){
			log
					.error("In TweakedLearner::maxFBetaEntry, maxFBeta has an illegal value");
		}

		return maxIndex;
	}

	private Row getRow(int i){
		return tweakingTable.get(i);
	}

	private String[] getClasses(){
		return schema.validClassNames();
	}

	private int classIndexOf(ClassLabel classLabel){
		return classIndexOf(classLabel.bestClassName());
	}

	private int classIndexOf(String classLabelName){
		return schema.getClassIndex(classLabelName);
	}

	// debug method - simply dumps the tweakingTable data structure to stdout
//	private void printTable(){
//		for(int i=0;i<tweakingTable.size();++i){
//			System.out.println("Row number "+i+": "+getRow(i));
//		}
//	}

	/**
	 ******************************************************************** 
	 ******************************************************************** 
	 * This class represents the information, about a single example,
	 * needed for executing the tweaking:
	 * 1. The example itself
	 * 2. Its true label/class. Indeed this field can be accessed every time
	 *    by using example.getLabel(), but for convenience it is stored in the table
	 * 3. The predicted class (orig_predicted), as given by the original 
	 * 	  (untweaked) classifier.
	 * 4. A dummy prediction (tweak_predicted), which is used in the actual 
	 *    tweaking process. During construction, all rows are initialized as NEG examples, 
	 *    commensurate with the way the tweaking process is executed.
	 *  
	 * Please note that during the tweaking process, examples that were predicted
	 * by the original (untweaked) classifier as POS can have a prediction of NEG,
	 * and vice versa.
	 * 
	 * Note also, that the actual score for an example is given using 
	 * predicted.posWeight(), where posWeight>0 means the original prediction 
	 * of the untweaked classifier was that this example is of a POSITIVE class,
	 * and posWeight<0 means NEGATIVE class.
	 * 
	 * In addition, for the actual tweaking process, 3 fields are 
	 * maintained for each example/row:
	 * 5. Precision
	 * 6. Recall
	 * 7. F_beta value
	 * 
	 * See the documentation of the actual tweaking method, ExecuteTweaking(),
	 * for further details
	 ******************************************************************** 
	 ******************************************************************** 
	 */
	private static class Row implements Serializable{

		private static final long serialVersionUID=-4069980043842319180L;

		transient public Instance instance=null; // the example

		public ClassLabel actual; // true label

		public ClassLabel orig_predicted; // predicted label - see documentation above

		public ClassLabel tweak_predicted; // temporary prediction, for tweaking process

		public double precision=UNINITIALIZED;

		public double recall=UNINITIALIZED;

		public double F_beta=UNINITIALIZED;

		public Row(Instance i,ClassLabel a,ClassLabel orig_p,ClassLabel tweak_p){
			instance=i;
			actual=a;
			orig_predicted=orig_p;
			tweak_predicted=tweak_p;
		}

		@Override
		public String toString(){
			return orig_predicted+"\t"+actual+"\t"+instance;
		}
	}

	/** 
	 ******************************************************************** 
	 ******************************************************************** 
	 * A Tweaked Classifier, with an optimization of the precision vs. recall
	 * Please note that this is an internal class of the TweakedLearner class.
	 * It is constructed and returned by the TweakedLearner, based on 
	 * an original untweaked binary clasifer, and a threshold which was found
	 * to optimized precision vs. recall
	 * 
	 * @author Giora Unger
	 * Created on May 19, 2005
	 ******************************************************************** 
	 ******************************************************************** 
	 */
	public static class TweakedClassifier extends BinaryClassifier implements
			Serializable,Visible{

		static private final long serialVersionUID=20080128L;

		private double m_threshold;

		private BinaryClassifier m_classifier;

		public TweakedClassifier(BinaryClassifier classifier,double threshold){
			m_classifier=classifier;
			m_threshold=threshold;
		}

		@Override
		public double score(Instance instance){
			return m_classifier.score(instance)-m_threshold;
		}

		/* (non-Javadoc)
		 * @see edu.cmu.minorthird.util.gui.Visible#toGUI()
		 * 
		 * Shows the original (untweaked) classifier, and the threshold 
		 * that was found
		 * Code was copied from file CMM.java and adjusted 
		 */
		@Override
		public Viewer toGUI(){
			final Viewer v=new ComponentViewer(){
				static final long serialVersionUID=20080128L;
				@Override
				public JComponent componentFor(Object o){
					TweakedClassifier c=(TweakedClassifier)o;
					JPanel mainPanel=new JPanel();
					mainPanel.setLayout(new BorderLayout());
					mainPanel.add(new JLabel("Optimal threshold for TweakedClassifier="+
							c.m_threshold),BorderLayout.NORTH);
					mainPanel.add(new JLabel("Original classifier before tweaking:"),
							BorderLayout.CENTER);
					Viewer subView=new SmartVanillaViewer(c.m_classifier);
					subView.setSuperView(this);
					mainPanel.add(subView,BorderLayout.SOUTH);
					mainPanel.setBorder(new TitledBorder("TweakedClassifier class"));
					return new JScrollPane(mainPanel);
				}
			};
			v.setContent(this);
			return v;
		}

		/* (non-Javadoc)
		 * @see edu.cmu.minorthird.classify.Classifier#explain(edu.cmu.minorthird.classify.Instance)
		 */
		@Override
		public String explain(Instance instance){
			StringBuffer buf=new StringBuffer("");
			buf.append("Explanation of original untweaked classifier:\n");
			buf.append(m_classifier.explain(instance));
			buf.append("\nAdjusted score after tweaking = "+score(instance));
			return buf.toString();
		}

		@Override
		public Explanation getExplanation(Instance instance){
			Explanation.Node top=new Explanation.Node("TweakedLearner Explanation");
			Explanation.Node orig=
					new Explanation.Node("Explanation of original untweaked classifier");
			Explanation.Node origEx=
					m_classifier.getExplanation(instance).getTopNode();
			orig.add(origEx);
			top.add(orig);
			Explanation.Node adjusted=
					new Explanation.Node("\nAdjusted score after tweaking = "+
							score(instance));
			top.add(adjusted);
			Explanation ex=new Explanation(top);
			return ex;
		}
	}

	/**
	 ******************************************************************** 
	 ******************************************************************** 
	 * Main method for testing purposes 
	 ******************************************************************** 
	 ******************************************************************** 
	 */
	public static void main(String[] args){
		System.out.println("Started the test program for TweakedLearner");
		NaiveBayes nb=new NaiveBayes();
		new TweakedLearner(nb,3.0);
		System.out.println("Created a TweakedLearner");
	}

}