package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;

/**
 * 'Generic' version of Collin's voted perceptron learner.
 *
 * @author William Cohen
 */

public class GenericCollinsLearner implements BatchSequenceClassifierLearner,SequenceConstants
{
	private static Logger log = Logger.getLogger(CollinsPerceptronLearner.class);
	private static final boolean DEBUG = log.isDebugEnabled();

	private OnlineClassifierLearner innerLearnerPrototype;
	private OnlineClassifierLearner[] innerLearner;
	private int historySize;
	private int numberOfEpochs;
	private String[] history;

	public GenericCollinsLearner()
	{
		this(new MarginPerceptron(0.0,false,true));
	}

	public GenericCollinsLearner(OnlineClassifierLearner innerLearner)
	{
		this(innerLearner,5);
	}

	public GenericCollinsLearner(int epochs)
	{
		this(new MarginPerceptron(0.0,false,true),epochs);
	}

	public GenericCollinsLearner(OnlineClassifierLearner innerLearner,int epochs)
	{
		this(innerLearner,3,epochs);
	}

	public GenericCollinsLearner(OnlineClassifierLearner innerLearner,int historySize,int epochs)
	{
		this.historySize = historySize;
		this.innerLearnerPrototype = innerLearner;
		this.numberOfEpochs = epochs;
		this.history = new String[historySize];
	}

	public void setSchema(ExampleSchema schema)	{	;	}

	//
	// accessors
	//
	public OnlineClassifierLearner getInnerLearner() { 
		return innerLearnerPrototype; 
	}
	public void setInnerLearner(OnlineClassifierLearner newInnerLearner) {
		this.innerLearnerPrototype = newInnerLearner;
	}
	public int getHistorySize() { return historySize; }
	public void setHistorySize(int newHistorySize) { this.historySize = newHistorySize; }
	public int getNumberOfEpochs() { return numberOfEpochs; }
	public void setNumberOfEpochs(int newNumberOfEpochs) { this.numberOfEpochs = newNumberOfEpochs; }
	

	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		ExampleSchema schema = dataset.getSchema();
		try {
			innerLearner = new OnlineClassifierLearner[ schema.getNumberOfClasses() ];		
			for (int i=0; i<schema.getNumberOfClasses(); i++) {
				innerLearner[i] = (OnlineClassifierLearner)innerLearnerPrototype.copy();
				innerLearner[i].reset();
			}
		} catch (CloneNotSupportedException ex) {
			throw new IllegalArgumentException("innerLearner must be cloneable");
		}

		ProgressCounter pc =
			new ProgressCounter("training sequential "+innerLearnerPrototype.toString(), 
													"sequence",numberOfEpochs*dataset.numberOfSequences());

		for (int epoch=0; epoch<numberOfEpochs; epoch++) 
		{
			dataset.shuffle();

			// statistics for curious researchers
			int sequenceErrors = 0;
			int transitionErrors = 0;
			int transitions = 0;

			for (Iterator i=dataset.sequenceIterator(); i.hasNext(); ) 
			{
				Example[] sequence = (Example[])i.next();
				Classifier c = new MultiClassClassifier(schema,innerLearner);
				ClassLabel[] viterbi = new BeamSearcher(c,historySize,schema).bestLabelSequence(sequence);
				if (DEBUG) log.debug("classifier: "+c);
				if (DEBUG) log.debug("viterbi:\n"+StringUtil.toString(viterbi));

				boolean errorOnThisSequence=false;

        // accumulate weights for transitions associated with each class k
				Hyperplane[] accumPos = new Hyperplane[schema.getNumberOfClasses()];
				Hyperplane[] accumNeg = new Hyperplane[schema.getNumberOfClasses()];
				for (int k=0; k<schema.getNumberOfClasses(); k++) {
					accumPos[k] = new Hyperplane();
					accumNeg[k] = new Hyperplane();
				}

				for (int j=0; j<sequence.length; j++) {
					// is the instance at sequence[j] associated with a difference in the sum
					// of feature values over the viterbi sequence and the actual one? 
					boolean differenceAtJ = !viterbi[j].isCorrect( sequence[j].getLabel() );
					//System.out.println("differenceAtJ for J="+j+" "+differenceAtJ+" - label");
					for (int k=1; j-k>=0 && !differenceAtJ && k<=historySize; k++) {
						if (!viterbi[j-k].isCorrect( sequence[j-k].getLabel() )) {
							//System.out.println("differenceAtJ for J="+j+" true: k="+k);
							differenceAtJ = true;
						}
					}
					if (differenceAtJ) {
						transitionErrors++;
						errorOnThisSequence=true;
						InstanceFromSequence.fillHistory( history, sequence, j );

						Instance correctXj = new InstanceFromSequence( sequence[j], history );
						int correctClassIndex = schema.getClassIndex( sequence[j].getLabel().bestClassName() );
						accumPos[correctClassIndex].increment( correctXj, +1.0 );
						accumNeg[correctClassIndex].increment( correctXj, -1.0 );
						if (DEBUG) log.debug("+ update "+sequence[j].getLabel().bestClassName()+" "+correctXj.getSource());

						InstanceFromSequence.fillHistory( history, viterbi, j );
						Instance wrongXj = new InstanceFromSequence( sequence[j], history );
						int wrongClassIndex = schema.getClassIndex( viterbi[j].bestClassName() );
						accumPos[wrongClassIndex].increment( wrongXj, -1.0 );
						accumNeg[wrongClassIndex].increment( wrongXj, +1.0 );
						if (DEBUG) log.debug("- update "+viterbi[j].bestClassName()+" "+wrongXj.getSource());
					}
				} // example sequence j
				if (errorOnThisSequence) {
					sequenceErrors++;
					String subPopId = sequence[0].getSubpopulationId();
					Object source = "no source";
					for (int k=0; k<schema.getNumberOfClasses(); k++) {
						//System.out.println("adding class="+k+" example: "+accumPos[k]);
						innerLearner[k].addExample( 
							new Example( new HyperplaneInstance(accumPos[k],subPopId,source), ClassLabel.positiveLabel(+1.0) ));
						innerLearner[k].addExample( 
							new Example( new HyperplaneInstance(accumNeg[k],subPopId,source), ClassLabel.negativeLabel(-1.0) ));
					}
				}
				transitions += sequence.length;
				pc.progress();
			} // sequence i

			System.out.println("Epoch "+epoch+": sequenceErr="+sequenceErrors
												 +" transitionErrors="+transitionErrors+"/"+transitions);

			if (transitionErrors==0) break;

		} // epoch
		pc.finished();
			
		for (int k=0; k<schema.getNumberOfClasses(); k++) {
			innerLearner[k].completeTraining();
		}

		// we can use a CMM here, since the classifier is constructed to the same
		// beam search will work
		Classifier c = new MultiClassClassifier(schema,innerLearner);

		return new CMM(c, historySize, schema );
	}

	private static class HyperplaneInstance implements Instance
	{
		private Hyperplane hyperplane;
		private String subpopulationId;
		private Object source;
		public HyperplaneInstance(Hyperplane hyperplane,String subpopulationId,Object source) { 
			// compensate for automatic increment of bias term by linear learners
			// for some reason it seems to work better to have the bias be linear in length
			hyperplane.incrementBias(-1.0);
			this.hyperplane = hyperplane; 
			this.subpopulationId = subpopulationId;
			this.source = source;
		}
		public Viewer toGUI() { return hyperplane.toGUI(); }
		public double getWeight(Feature f) { return hyperplane.featureScore(f); }
		public Feature.Looper binaryFeatureIterator() { return new Feature.Looper(Collections.EMPTY_SET); }
		public Feature.Looper numericFeatureIterator() { return hyperplane.featureIterator(); }
		public Feature.Looper featureIterator() { return hyperplane.featureIterator(); }
		public double getWeight() { return 1.0; }
		public Object getSource() { return source; }
		public String getSubpopulationId() { return subpopulationId; }
		// iterate overall hyperplane features except the bias feature
		private class MyIterator implements Iterator
		{
			private Iterator i;
			private Object myNext = null; // buffers the next nonbias feature produced by i
			public MyIterator() { this.i = hyperplane.featureIterator(); advance(); }
			private void advance() 
			{
				if (!i.hasNext()) myNext = null;
				else { 
					myNext = i.next();
					if (myNext.equals(Hyperplane.BIAS_TERM)) advance();
				}
			}
			public void remove() { throw new UnsupportedOperationException("can't remove"); }
			public boolean hasNext() { return myNext!=null; }
			public Object next() { Object result=myNext; advance(); return result; }
		}
	}

	public static class MultiClassClassifier implements Classifier,Visible,Serializable
	{
		private int serialVersionUID = 1;
		private ExampleSchema schema;
		private Classifier[] innerClassifier;
		private int numClasses;

		public MultiClassClassifier(ExampleSchema schema,Classifier[] learners) {
			this.schema = schema;
			this.numClasses = schema.getNumberOfClasses();
			innerClassifier = learners;
		}
		public MultiClassClassifier(ExampleSchema schema,OnlineClassifierLearner[] innerLearner)
		{
			this.schema = schema;
			this.numClasses = schema.getNumberOfClasses();
			innerClassifier = new Classifier[ numClasses ];
			for (int i=0; i<numClasses; i++) {
				innerClassifier[i] = innerLearner[i].getClassifier();
			}
		}
		public ClassLabel classification(Instance instance)
		{
			ClassLabel label = new ClassLabel();
			for (int i=0; i<numClasses; i++) {			
				label.add( schema.getClassName(i), innerClassifier[i].classification(instance).posWeight() );
			}
			return label; 
		}
		public String explain(Instance instance)
		{
			StringBuffer buf = new StringBuffer("");
			for (int i=0; i<numClasses; i++) {			
				buf.append("Classifier for class "+schema.getClassName(i)+":\n");
				buf.append( innerClassifier[i].explain(instance) );
				buf.append("\n");
			}
			return buf.toString();
		}

		public Viewer toGUI()
		{
			Viewer gui = new ComponentViewer() {
					public JComponent componentFor(Object o) {
						MultiClassClassifier c = (MultiClassClassifier)o;
						JPanel main = new JPanel();
						for (int i=0; i<numClasses; i++) {
							JPanel classPanel = new JPanel();
							classPanel.setBorder(new TitledBorder("Class "+c.schema.getClassName(i)));
							Viewer subviewer = new SmartVanillaViewer( c.innerClassifier[i] );
							subviewer.setSuperView( this );
							classPanel.add( subviewer );
							main.add(classPanel);
						}
						return new JScrollPane(main);
					}
				};
			gui.setContent(this);
			return gui;
		}
		public String toString() 
		{
			return "[MultiClassClassifier:"+StringUtil.toString(innerClassifier,"\n","\n]","\n - ");
		}
	}
}

