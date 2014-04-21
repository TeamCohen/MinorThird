package edu.cmu.minorthird.classify.sequential;



/**
 * A SequenceClassifierLearner that trains itself in batch mode.
 *
 * @author William Cohen
 */

public interface BatchSequenceClassifierLearner extends SequenceClassifierLearner
{
	public SequenceClassifier batchTrain(SequenceDataset dataset);
}

