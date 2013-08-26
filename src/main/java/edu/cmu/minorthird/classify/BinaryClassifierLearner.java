package edu.cmu.minorthird.classify;



/**
 * Learn a BinaryClassifier.
 *
 * @author William Cohen
 */

public interface BinaryClassifierLearner extends ClassifierLearner
{
	/* Returned the learned classifier as a binary classifier.
	 */
	public BinaryClassifier getBinaryClassifier();
}
