package edu.cmu.minorthird.classify.sequential;

import java.io.Serializable;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.OnlineBinaryClassifierLearner;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Generic version of Collin's voted perceptron learner.
 *
 * @author William Cohen
 */

public class GenericCollinsLearnerV1 implements BatchSequenceClassifierLearner,SequenceConstants
{
	private OnlineBinaryClassifierLearner innerLearnerPrototype;
	private OnlineBinaryClassifierLearner[] innerLearner;
	private int historySize;
	private int numberOfEpochs;
	private String[] history;

	public GenericCollinsLearnerV1()
	{
		this(3,5);
	}

	public GenericCollinsLearnerV1(OnlineBinaryClassifierLearner innerLearner,int historySize)
	{
		this(innerLearner,historySize,5);
	}

	public GenericCollinsLearnerV1(int historySize,int epochs)
	{
		this(new VotedPerceptron(),historySize,epochs);
	}

	public GenericCollinsLearnerV1(OnlineBinaryClassifierLearner innerLearner,int historySize,int epochs)
	{
		this.historySize = historySize;
		this.innerLearnerPrototype = innerLearner;
		this.numberOfEpochs = epochs;
		this.history = new String[historySize];
	}

	@Override
	public void setSchema(ExampleSchema schema)	{	;	}

	//
	// accessors
	//
	public OnlineBinaryClassifierLearner getInnerLearner() { 
		return innerLearnerPrototype; 
	}
	public void setInnerLearner(OnlineBinaryClassifierLearner newInnerLearner) {
		this.innerLearnerPrototype = newInnerLearner;
	}
	@Override
	public int getHistorySize() { return historySize; }
	public void setHistorySize(int newHistorySize) { this.historySize = newHistorySize; }
	public int getNumberOfEpochs() { return numberOfEpochs; }
	public void setNumberOfEpochs(int newNumberOfEpochs) { this.numberOfEpochs = newNumberOfEpochs; }
	

	@Override
	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		ExampleSchema schema = dataset.getSchema();
		try {
			innerLearner = new OnlineBinaryClassifierLearner[ schema.getNumberOfClasses() ];		
			for (int i=0; i<schema.getNumberOfClasses(); i++) {
				innerLearner[i] = (OnlineBinaryClassifierLearner)innerLearnerPrototype.copy();
				innerLearner[i].reset();
			}
		} catch (Exception ex) {
			throw new IllegalArgumentException("innerLearner must be cloneable");
		}

		ProgressCounter pc =
			new ProgressCounter("training sequential "+innerLearnerPrototype.toString(), 
													"sequence",numberOfEpochs*dataset.numberOfSequences());

		for (int epoch=0; epoch<numberOfEpochs; epoch++) {

			// statistics for curious researchers
			int sequenceErrors = 0;
			int transitionErrors = 0;
			int transitions = 0;

			for (Iterator<Example[]> i=dataset.sequenceIterator(); i.hasNext(); ) {

				Example[] sequence = i.next();
				Classifier c = new MultiClassClassifier(schema,innerLearner);
				ClassLabel[] viterbi = new BeamSearcher(c,historySize,schema).bestLabelSequence(sequence);

				boolean errorOnThisSequence=false;
				for (int j=0; j<sequence.length; j++) {
					// is the instance at sequence[j] associated with a difference in the sum
					// of feature values over the viterbi sequence and the actual one? 
					boolean differenceAtJ = !viterbi[j].isCorrect( sequence[j].getLabel() );
					for (int k=1; j-k>=0 && !differenceAtJ && k<=historySize; k++) {
						if (!viterbi[j-k].isCorrect( sequence[j-k].getLabel() )) {
							differenceAtJ = true;
						}
					}
					if (differenceAtJ) {
						transitionErrors++;
						errorOnThisSequence=true;
						InstanceFromSequence.fillHistory( history, sequence, j );
						Instance correctXj = new InstanceFromSequence( sequence[j], history );
						int correctClassIndex = schema.getClassIndex( sequence[j].getLabel().bestClassName() );
						innerLearner[correctClassIndex].addExample( new Example( correctXj, ClassLabel.binaryLabel(1.0) ) );
						
						InstanceFromSequence.fillHistory( history, viterbi, j );
						Instance wrongXj = new InstanceFromSequence( sequence[j], history );
						int wrongClassIndex = schema.getClassIndex( viterbi[j].bestClassName() );
						innerLearner[wrongClassIndex].addExample( new Example( wrongXj, ClassLabel.binaryLabel(-1.0)) );
					}
				} // example sequence j
				if (errorOnThisSequence) sequenceErrors++;
				transitions += sequence.length;
				pc.progress();
			} // sequence i

			System.out.println("Epoch "+epoch+": sequenceErr="+sequenceErrors
												 +" transitionErrors="+transitionErrors+"/"+transitions);

			if (transitionErrors==0) break;

		} // epoch
		pc.finished();
			
		// we can use a CMM here, since the classifier is constructed to the same
		// beam search will work
		Classifier c = new MultiClassClassifier(schema,innerLearner);

		return new CMM(c, historySize, schema );
	}

	public static class MultiClassClassifier implements Classifier,Visible,Serializable
	{
		static private final long serialVersionUID = 1;
		private ExampleSchema schema;
		private BinaryClassifier[] innerClassifier;
		private int numClasses;

	        public MultiClassClassifier(ExampleSchema schema,BinaryClassifier[] learners) {
			this.schema = schema;
			this.numClasses = schema.getNumberOfClasses();
			innerClassifier = learners;
	        }
		public MultiClassClassifier(ExampleSchema schema,OnlineBinaryClassifierLearner[] innerLearner)
		{
			this.schema = schema;
			this.numClasses = schema.getNumberOfClasses();
			innerClassifier = new BinaryClassifier[ numClasses ];
			for (int i=0; i<numClasses; i++) {
				innerClassifier[i] = innerLearner[i].getBinaryClassifier();
			}
		}
		@Override
		public ClassLabel classification(Instance instance)
		{
			ClassLabel label = new ClassLabel();
			for (int i=0; i<numClasses; i++) {			
				label.add( schema.getClassName(i), innerClassifier[i].score(instance) );
			}
			return label; 
		}
		@Override
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
	    @Override
			public Explanation getExplanation(Instance instance) {
		Explanation.Node top = new Explanation.Node("GenericCollins Explanation");

		for (int i=0; i<numClasses; i++) {			
		    Explanation.Node classifier = new Explanation.Node("Classifier for class "+schema.getClassName(i));
		    Explanation.Node classEx = innerClassifier[i].getExplanation(instance).getTopNode();
		    classifier.add(classEx);
		    top.add(classifier);
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
	}
}

