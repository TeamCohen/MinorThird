package edu.cmu.minorthird.classify;



/**
 * Abstract ClassifierLearner which instantiates the teacher-learner protocol
 * so as to implement a standard batch learner.
 *
 * @author William Cohen
 */

public abstract class BatchClassifierLearner implements ClassifierLearner
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
	final public void setInstancePool(Instance.Looper i) { ; }
	final public boolean hasNextQuery() { return false; }
	final public Instance nextQuery() { return null; }
	final public void addExample(Example answeredQuery) { dataset.add(answeredQuery); classifier=null; }
	final public void completeTraining() { classifier = batchTrain(dataset); }

	final public Classifier getClassifier() {
		if (classifier==null) classifier = batchTrain(dataset);
		return classifier;
	}

	/** subclasses  should use this method to implement a batch supervised learning algorithm. 
	 */
	abstract public Classifier batchTrain(Dataset dataset);
}
