package edu.cmu.minorthird.classify.StackedGraphicalLearning;

import edu.cmu.minorthird.classify.*;
/**
 * Abstract ClassifierLearner which instantiates the teacher-learner protocol
 * so as to implement a stacked batch learner.
 *
 * @author Zhenzhen Kou
 */

public abstract class StackedBatchClassifierLearner implements ClassifierLearner
{
	public RealRelationalDataset RelDataset = new RealRelationalDataset();

	/** This variable saves the last classifier produced by batchTrain.
	 * If it is non-null, then it will be returned by class to
	 * getClassifier().  Implementations of batchTrain should save the
	 * returned classifier to avoid extra work.
	 */
	protected Classifier classifier = null;


    public ClassifierLearner copy() {
	StackedBatchClassifierLearner bcl = null;//(ClassifierLearner)(new Object());
	try {
	    bcl =(StackedBatchClassifierLearner)(this.clone());
	    //bcl = this;
	    bcl.RelDataset = new RealRelationalDataset();
	    bcl.classifier = null;
	} catch (Exception e) {
	    System.out.println("Can't CLONE!!");
	    e.printStackTrace();
	}
	return (ClassifierLearner)bcl;
    }

	final public void reset() { 
		RelDataset = new RealRelationalDataset(); 
		classifier = null; 
	}
	final public void setInstancePool(Instance.Looper i) { ; }
	final public boolean hasNextQuery() { return false; }
	final public Instance nextQuery() { return null; }
	final public void addExample(Example answeredQuery) { RelDataset.add(answeredQuery); classifier=null; }
	final public void completeTraining() { classifier = batchTrain(RelDataset); }

	final public Classifier getClassifier() {
		if (classifier==null) classifier = batchTrain(RelDataset);
		return classifier;
	}

	/** subclasses  should use this method to implement a batch supervised learning algorithm. 
	 */
	abstract public Classifier batchTrain(RealRelationalDataset RelDataset);
	
	
}
