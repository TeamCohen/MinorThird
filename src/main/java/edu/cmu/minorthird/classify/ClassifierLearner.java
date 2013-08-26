package edu.cmu.minorthird.classify;

import java.util.Iterator;

/**
 * Learn an Classifier.  This describes the learner's side of the
 * protocol used to communicate between "learners" and "teachers".
 *
 * @author William Cohen
 */

public interface ClassifierLearner extends Cloneable{

	/** 
	 * Accept an ExampleSchema - constraints on what the
	 * Examples will be.
	 */
	public void setSchema(ExampleSchema schema);
	
	/** 
	 * Returns the ExampleSchema - constraints on what the
	 * Examples will be.
	 */
	public ExampleSchema getSchema();

	/**
	 * Forget everything and prepare for a new learning session.
	 */
	public void reset();

	/**
	 * Make a copy of the learner.
	 * Note: This will reset the learner, erasing previous data!
	 */
	public ClassifierLearner copy();

	/**
	 * Accept a set of unlabeled instances. These might be used in
	 * formulating queries in active learning, or for semi-supervised
	 * learning.  Queries are made with the methods hasNextQuery(),
	 * nextQuery(), and setAnswer().  
	 * <p>
	 * Learners need not make use of the instance pool.
	 */
	public void setInstancePool(Iterator<Instance> i);

	/**
	 * Returns true if the learner has more queries to answer. 
	 * <p>
	 * Learners may always return 'false', if they are not active.
	 */
	public boolean hasNextQuery(); 

	/**
	 * Returns an Instance for which the learner would like a label. 
	 * <p>
	 * This will only be called if hasNextQuery() returns true.
	 */
	public Instance nextQuery(); 

	/**
	 * Accept a labeled example. The example might be the answer to the last query, 
	 * or it may be an example chosen by the teacher.
	 * <p>
	 * All learners must provide a non-trivial implementation of addExample.
	 */
	public void addExample(Example answeredQuery);

	/**
	 * Accept a signal that no more training data is available.  This
	 * would trigger any additional computation that might be useful
	 * to speed up or improve the results of getClassifier().
	 */
	public void completeTraining();

	/**
	 * Return the learned classifier.  The classifier should take advantage of
	 * all information sent by the teacher to date.  Teachers can assume that
	 * multiple calls to getClassifier() without intervening calls to addExample()
	 * will return the same object, and do little computation.  Teachers can
	 * not assume that this object is immutable: for instance, in the case of
	 * an on-line learning method, the classifier that is returned might
	 * change after more examples are learned.
	 * <p>
	 * All learners must implement this method.
	 */
	public Classifier getClassifier();

}
