/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;



/**
 * Abstract ClassifierLearner which instantiates the teacher-learner protocol
 * so as to implement a standard on-line learner.
 *
 * @author William Cohen
 */

public abstract class OnlineClassifierLearner implements ClassifierLearner
{
	final public void setInstancePool(Instance.Looper i) { ; }
	final public boolean hasNextQuery() { return false; }
	final public Instance nextQuery() { return null; }
	final public ClassifierLearner copy() throws CloneNotSupportedException { 
		return (ClassifierLearner)clone(); 
	}

	/** Override this method if appropriate.
	 */
	public void completeTraining() {;}

	/** Subclasses should use this method to to whatever incremental update is
	 * needed after in response to a new example. 
	 */
	abstract public void addExample(Example answeredQuery);

	/** Subclasses should use this method to return the current classifier. 
	 */
	abstract public Classifier getClassifier();

	/** Subclasses need to implement this method */
	abstract public void reset();

}
