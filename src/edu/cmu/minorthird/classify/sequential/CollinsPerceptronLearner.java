package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.Serializable;
import java.util.Iterator;

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
	private int historySize;
	private int numberOfEpochs;
	private String[] history;

	public CollinsPerceptronLearner()
	{
		this(3,5);
	}

	public CollinsPerceptronLearner(int historySize,int numberOfEpochs)
	{
		this.historySize = historySize;
		this.numberOfEpochs = numberOfEpochs;
		this.history = new String[historySize];
	}

	public void setSchema(ExampleSchema schema)	{	;	}

	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		ExampleSchema schema = dataset.getSchema();
		MultiClassVPClassifier c = new MultiClassVPClassifier(schema);

		ProgressCounter pc =
			new ProgressCounter("training sequence perceptron","sequence",numberOfEpochs*dataset.numberOfSequences());

		for (int epoch=0; epoch<numberOfEpochs; epoch++) {

			for (Iterator i=dataset.sequenceIterator(); i.hasNext(); ) {

				Example[] sequence = (Example[])i.next();
				ClassLabel[] viterbi = new BeamSearcher(c,historySize,schema).bestLabelSequence(sequence);

				// At this point, Collin's paper says to add Phi(sequence) -
				// Phi(viterbi) to the current weight vector W.  We're doing
				// this, with two twists: (a) the features in our instance
				// vectors phi(sequence,i) vectors are not paired with class
				// labels. Instead, we compute class-label independent features
				// and then attach the class label when we 'update' c. 
				// (b) rather than computing Phi(sequence), Phi(viterbi), and
				// subtracting, we compute the difference directly.  

				for (int j=0; j<sequence.length; j++) {
					// is the instance at sequence[j] associated with a difference in the sum
					// of feature values over the viterbi sequence and the actual one? 
					boolean differenceAtJ = !viterbi[j].isCorrect( sequence[j].getLabel() );
					for (int k=1; j-k>=0 && !differenceAtJ && k<=historySize; k++) {
						if (!viterbi[j-k].isCorrect( sequence[j-k].getLabel() )) {
							differenceAtJ = true;
						}
					}

					if (differenceAtJ) { // i.e., if phi(sequence,j) != phi(viterbi,j)
						InstanceFromSequence.fillHistory( history, sequence, j );
						Instance correctXj = new InstanceFromSequence( sequence[j], history );
						c.update( sequence[j].getLabel().bestClassName(), correctXj, 1.0 ); 
						InstanceFromSequence.fillHistory( history, viterbi, j );
						Instance wrongXj = new InstanceFromSequence( sequence[j], history );
						c.update( viterbi[j].bestClassName(), wrongXj, -1.0 ); 
					}
				} // example sequence j

				// our computation of Phi(sequence)-Phi(viterbi) is complete - the voting scheme
				// for voted perceptron needs this...
				c.completeUpdate(); 

				pc.progress();
			} // sequence i
		} // epoch
		pc.finished();
		c.setVoteMode(true);
		// we can use a CMM here, since the classifier is constructed to the same
		// beam search will work
		return new CMM(c, historySize, schema );
	}

	public static class MultiClassVPClassifier implements Classifier,Visible,Serializable
	{
		private int serialVersionUID = 1;
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

		public ClassLabel classification(Instance instance)
		{
			Hyperplane[] h = voteMode ? s_t : w_t ;
			ClassLabel label = new ClassLabel();
			for (int i=0; i<numClasses; i++) {			
				label.add( schema.getClassName(i), h[i].score(instance) );
			}
			return label; 
		}

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

		public Viewer toGUI()
		{
			Viewer gui = new ComponentViewer() {
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
	}
}

