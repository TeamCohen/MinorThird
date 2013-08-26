package edu.cmu.minorthird.classify.sequential;



/**
 * Interface for something that trains sequence classifiers.
 *
 * @author William Cohen
 */

public interface SequenceClassifierTeacher
{
	public SequenceClassifier train(SequenceClassifierLearner learner);
}

