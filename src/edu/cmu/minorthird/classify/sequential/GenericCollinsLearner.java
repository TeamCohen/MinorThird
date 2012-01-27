/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.util.*;

import java.util.*;
import org.apache.log4j.*;

/**
 * 'Generic' version of Collin's voted perceptron learner.
 *
 * <p>As of May 9, 2004, this is a different algorithm, which is much
 * more like Collin's original method.  The 'old' implementation is in
 * GenericCollinsLearnerV1.
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

	@Override
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
	@Override
	public int getHistorySize() { return historySize; }
	public void setHistorySize(int newHistorySize) { this.historySize = newHistorySize; }
	public int getNumberOfEpochs() { return numberOfEpochs; }
	public void setNumberOfEpochs(int newNumberOfEpochs) { this.numberOfEpochs = newNumberOfEpochs; }
	

	@Override
	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		ExampleSchema schema = dataset.getSchema();
		innerLearner = SequenceUtils.duplicatePrototypeLearner(innerLearnerPrototype,schema.getNumberOfClasses());

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

			for (Iterator<Example[]> i=dataset.sequenceIterator(); i.hasNext(); ) 
			{
				Example[] sequence = i.next();
				Classifier c = new SequenceUtils.MultiClassClassifier(schema,innerLearner);
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
						if (DEBUG) log.debug("+ update "+sequence[j].getLabel().bestClassName()+" "+correctXj.getSource()+";"+correctXj);

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

		Classifier c = new SequenceUtils.MultiClassClassifier(schema,innerLearner);

		// we can use a CMM here, since the classifier is constructed so
		// that the same beam search will work
		return new CMM(c, historySize, schema );
	}
}

