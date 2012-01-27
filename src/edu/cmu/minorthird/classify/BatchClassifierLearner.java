package edu.cmu.minorthird.classify;

import java.util.Iterator;



/**
 * Abstract ClassifierLearner which instantiates the teacher-learner protocol
 * so as to implement a standard batch learner.
 *
 * @author William Cohen
 */

public abstract class BatchClassifierLearner implements ClassifierLearner
{
	public Dataset dataset = new BasicDataset();

	/** This variable saves the last classifier produced by batchTrain.
	 * If it is non-null, then it will be returned by class to
	 * getClassifier().  Implementations of batchTrain should save the
	 * returned classifier to avoid extra work.
	 */
	protected Classifier classifier = null;


    @Override
		public ClassifierLearner copy() {
	BatchClassifierLearner bcl = null;//(ClassifierLearner)(new Object());
	try {
	    bcl =(BatchClassifierLearner)(this.clone());
	    //bcl = this;
	    bcl.dataset = new BasicDataset();
	    bcl.classifier = null;
	} catch (Exception e) {
	    System.out.println("Can't CLONE!!");
	    e.printStackTrace();
	}
	return bcl;
    }

	@Override
	final public void reset() { 
		dataset = new BasicDataset(); 
		classifier = null; 
	}
	@Override
	final public void setInstancePool(Iterator<Instance> i) { ; }
	@Override
	final public boolean hasNextQuery() { return false; }
	@Override
	final public Instance nextQuery() { return null; }
	@Override
	final public void addExample(Example answeredQuery) { dataset.add(answeredQuery); classifier=null; }
	@Override
	final public void completeTraining() { classifier = batchTrain(dataset); }

	@Override
	final public Classifier getClassifier() {
		if (classifier==null) classifier = batchTrain(dataset);
		return classifier;
	}

	/** subclasses  should use this method to implement a batch supervised learning algorithm. 
	 */
	abstract public Classifier batchTrain(Dataset dataset);
}
