/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.util.Iterator;



/**
 * Abstract ClassifierLearner which instantiates the teacher-learner protocol
 * so as to implement a standard on-line learner.
 *
 * @author William Cohen
 */

public abstract class OnlineClassifierLearner implements ClassifierLearner
{
	@Override
	final public void setInstancePool(Iterator<Instance> i) { ; }
	@Override
	final public boolean hasNextQuery() { return false; }
	@Override
	final public Instance nextQuery() { return null; }
	@Override
	public ClassifierLearner copy() { 
	    ClassifierLearner learner = null;
	    try {
		learner =(ClassifierLearner)(this.clone());
		learner.reset();
	    } catch (Exception e) {
		System.out.println("Can't CLONE!!");
		e.printStackTrace();
	    }
	    return learner;
	}

	/** A promise from the caller that no further examples will be added.
	 * Override this method if it's appropriate.
	 */
	@Override
	public void completeTraining() {;}

	/** Subclasses should use this method to perform whatever
	 * incremental update is needed after in response to a new
	 * example.
	 */
	@Override
	abstract public void addExample(Example answeredQuery);

	/** Subclasses should use this method to return the current
	 * classifier.
	 */
	@Override
	abstract public Classifier getClassifier();

	/** 'forget' everything about the last learning task, and
	 * start a new task. Subclasses need to implement this
	 * method */
	@Override
	abstract public void reset();

}
