package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.Serializable;
import java.util.Iterator;
import org.apache.log4j.*;

/**
 * Sequential learner based on the perceptron algorithm, as described
 * in Discriminative Training Methods for Hidden Markov Models: Theory
 * and Experiments with Perceptron Algorithms, Mike Collins, EMNLP
 * 2002.
 *
 * @author William Cohen
 */

public class CollinsPerceptronLearner implements BatchSequenceClassifierLearner,SequenceConstants
{
	protected static Logger log = Logger.getLogger(CollinsPerceptronLearner.class);
	protected static final boolean DEBUG = log.isDebugEnabled();

	protected int historySize;
	protected int numberOfEpochs;
	protected String[] history;

	public CollinsPerceptronLearner()
	{
		this(3,5);
	}
	public CollinsPerceptronLearner(int numberOfEpochs)
	{
		this(3,numberOfEpochs);
	}
	public CollinsPerceptronLearner(int historySize,int numberOfEpochs)
	{
		this.historySize = historySize;
		this.numberOfEpochs = numberOfEpochs;
		this.history = new String[historySize];
	}

	public int getNumberOfEpochs() { return numberOfEpochs; }
	public void setNumberOfEpochs(int newNumberOfEpochs) { this.numberOfEpochs = newNumberOfEpochs; }
	@Override
	public int getHistorySize() { return historySize; }
	public void setHistorySize(int newHistorySize) { this.historySize = newHistorySize; }
	// Help Button
	public String getHistorySizeHelp() { return "Number of tokens to look back on. <br>The predicted labels for the history are used as features to help classify the current token." ;}

	@Override
	public void setSchema(ExampleSchema schema)	{	;	}

	@Override
	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		ExampleSchema schema = dataset.getSchema();
		MultiClassVPClassifier c = new MultiClassVPClassifier(schema);

		ProgressCounter pc =
			new ProgressCounter("training sequence perceptron","sequence",numberOfEpochs*dataset.numberOfSequences());

		for (int epoch=0; epoch<numberOfEpochs; epoch++) 
		{
			dataset.shuffle();

			// statistics for curious researchers
			int sequenceErrors = 0;
			int transitionErrors = 0;
			int transitions = 0;

			for (Iterator<Example[]> i=dataset.sequenceIterator(); i.hasNext(); ) 
			{
				Example[] sequence = i.next();
				ClassLabel[] viterbi = new BeamSearcher(c,historySize,schema).bestLabelSequence(sequence);

				if (DEBUG) log.debug("classifier: "+c);
				if (DEBUG) log.debug("viterbi:\n"+StringUtil.toString(viterbi));

				// At this point, Collin's paper says to add Phi(sequence) -
				// Phi(viterbi) to the current weight vector W.  We're doing
				// this, with two twists: (a) the features in our instance
				// vectors phi(sequence,i) vectors are not paired with class
				// labels. Instead, we compute class-label independent features
				// and then attach the class label when we 'update' c. 
				// (b) rather than computing Phi(sequence), Phi(viterbi), and
				// subtracting, we compute the difference directly.  

				boolean errorOnThisSequence=false;
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

					if (differenceAtJ) { // i.e., if phi(sequence,j) != phi(viterbi,j)
						transitionErrors++;
						errorOnThisSequence=true;
						InstanceFromSequence.fillHistory( history, sequence, j );
						Instance correctXj = new InstanceFromSequence( sequence[j], history );
						c.update( sequence[j].getLabel().bestClassName(), correctXj, 1.0 ); 
						if (DEBUG) log.debug("+ update "+sequence[j].getLabel().bestClassName()+" "+correctXj.getSource());
						InstanceFromSequence.fillHistory( history, viterbi, j );
						Instance wrongXj = new InstanceFromSequence( sequence[j], history );
						c.update( viterbi[j].bestClassName(), wrongXj, -1.0 ); 
						if (DEBUG) log.debug("- update "+viterbi[j].bestClassName()+" "+wrongXj.getSource());
					}
				} // example sequence j

				// our computation of Phi(sequence)-Phi(viterbi) is complete - the voting scheme
				// for voted perceptron needs this...
				c.completeUpdate(); 

				if (errorOnThisSequence) sequenceErrors++;
				transitions += sequence.length;

				pc.progress();
			} // sequence i

			System.out.println("Epoch "+epoch+": sequenceErr="+sequenceErrors
					+" transitionErrors="+transitionErrors+"/"+transitions);

			if (transitionErrors==0) break;

		} // epoch
		pc.finished();
		c.setVoteMode(true);
		// we can use a CMM here, since the classifier is constructed to the same
		// beam search will work
		return new CMM(c, historySize, schema );
	}

	public static class MultiClassVPClassifier implements Classifier,Visible,Serializable
	{
		static private final long serialVersionUID = 1;
		private ExampleSchema schema;
		private Hyperplane[] s_t, w_t;
		private int numClasses; 
		private boolean voteMode = false;

		public MultiClassVPClassifier(ExampleSchema schema) 
		{
			this.schema = schema;
			this.numClasses = schema.getNumberOfClasses();
			reset();
		}

		public void setVoteMode(boolean flag) { voteMode=flag; }

		public Hyperplane[] getHyperplanes() { return voteMode? s_t : w_t ; }
		public ExampleSchema getSchema() { return schema; }

		public void update(String className, Instance instance, double delta)
		{
			int index = schema.getClassIndex(className);
			w_t[index].increment( instance, delta ); 
		}

		public void completeUpdate()
		{
			for (int i=0; i<numClasses; i++) {
				s_t[i].increment( w_t[i], 1.0 );
			}
		}

		@Override
		public ClassLabel classification(Instance instance)
		{
			Hyperplane[] h = voteMode ? s_t : w_t ;
			ClassLabel label = new ClassLabel();
			for (int i=0; i<numClasses; i++) {			
				label.add( schema.getClassName(i), h[i].score(instance) );
			}
			return label; 
		}

		@Override
		public String explain(Instance instance)
		{
			Hyperplane[] h = voteMode ? s_t : w_t ;
			StringBuffer buf = new StringBuffer("");
			for (int i=0; i<numClasses; i++) {			
				buf.append("Hyperplane for class "+schema.getClassName(i)+":\n");
				buf.append( h[i].explain(instance) );
				buf.append("\n");
			}
			return buf.toString();
		}
		@Override
		public Explanation getExplanation(Instance instance) {
			Hyperplane[] h = voteMode ? s_t : w_t ;
			Explanation.Node top = new Explanation.Node("CollinsPerceptron Explanation");
			for (int i=0; i<numClasses; i++) {			
				Explanation.Node hyp = new Explanation.Node("Hyperplane for class "+schema.getClassName(i)+":\n");
				Explanation.Node explanation = h[i].getExplanation(instance).getTopNode();
				hyp.add(explanation);
				top.add(hyp);
			}
			Explanation ex = new Explanation(top);
			return ex;
		}

		@Override
		public Viewer toGUI()
		{
			Viewer gui = new ComponentViewer() {
				static final long serialVersionUID=20080207L;
				@Override
				public JComponent componentFor(Object o) {
					MultiClassVPClassifier c = (MultiClassVPClassifier)o;
					JPanel main = new JPanel();
					for (int i=0; i<numClasses; i++) {
						JPanel classPanel = new JPanel();
						classPanel.setBorder(new TitledBorder("Class "+c.schema.getClassName(i)));
						Viewer subviewer = voteMode ? s_t[i].toGUI() : w_t[i].toGUI();
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

		public void reset()	
		{
			s_t = new Hyperplane[numClasses];
			w_t = new Hyperplane[numClasses];
			for (int i=0; i<numClasses; i++) {
				s_t[i] = new Hyperplane();
				w_t[i] = new Hyperplane();
			}
		}

		@Override
		public String toString() 
		{
			return "[MultiClassVPClassifier:"+StringUtil.toString(w_t,"\n","\n]","\n - ");
		}
	}
}

