
package edu.cmu.minorthird.classify.semisupervised;

import edu.cmu.minorthird.classify.*;


/**
 * Abstract ClassifierLearner which instantiates the teacher-learner protocol
 * so as to implement a standard batch learner.
 *
 * @author Willia, Cohen
 * @author Edoardo Airoldi
 *
 */

public abstract class SemiSupervisedBatchClassifierLearner implements ClassifierLearner
{
	private Dataset dataset = new BasicDataset();

	/** This variable saves the last classifier produced by batchTrain.
	 * If it is non-null, then it will be returned by class to
	 * getClassifier().  Implementations of batchTrain should save the
	 * returned classifier to avoid extra work.
	 */
	protected Classifier classifier = null;

	final public void reset() {
		dataset = new BasicDataset();
		classifier = null;
	}
	final public boolean hasNextQuery() { return false; }
	final public Instance nextQuery() { return null; }
	final public void addExample(Example answeredQuery) { dataset.add(answeredQuery); classifier=null; }

	final public Classifier getClassifier() {
		if (classifier==null) classifier = batchTrain(dataset);
		return classifier;
	}

  /** subclasses should use this method to get the unlabeled examples available for
   * semi-supervised learning.
   */
  abstract public void setInstancePool(Instance.Looper i);

	/** subclasses should use this method to implement a batch supervised learning algorithm.
	 */
	abstract public Classifier batchTrain(Dataset dataset);
}
