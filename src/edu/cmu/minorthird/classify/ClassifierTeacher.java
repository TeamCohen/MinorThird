package edu.cmu.minorthird.classify;

import java.util.Iterator;



/**
 * Implements the teacher's side of the learner-teacher protocol.
 *
 * @author William Cohen
 *
 */
public abstract class ClassifierTeacher
{
	/** Train a ClassifierLearner and return the learned Classifier, using
	 * some unspecified source of information to get labels.  
	 */
	final public Classifier train(ClassifierLearner learner) 
	{
		// initialize the learner for a new problem
		learner.reset();

		// tell learner the schema of examples
		learner.setSchema( schema() );

		// provide unlabeled examples to the learner, for unsupervised
		// training, semi-supervised training, or active learner
		learner.setInstancePool( instancePool() );

		// passive learning from already-available labeled data
		for (Iterator<Example> i=examplePool(); i.hasNext(); ) {
			learner.addExample( i.next() );
		}

		// active learning 
		while (learner.hasNextQuery() && hasAnswers()) {
			Instance query = learner.nextQuery();
			Example answeredQuery = labelInstance(query);
			if (answeredQuery!=null) {
				learner.addExample( answeredQuery );
			}
		}

		// signal that there's no more data available
		learner.completeTraining();

		// final result
		return learner.getClassifier();
	}

	//
	// subclasses implement these steps
	//

	/** The set of classes that will be used.
	 */ 
	abstract protected ExampleSchema schema();

	/** Labeled instances that will be sent to the learner
	 * via a call to addExample().
	 */
	abstract protected Iterator<Example> examplePool();

	/** Unlabeled instances, which will be provided to the learner via
	 * setInstancePool().  These can be used for semi-supervised
	 * learner, or to form queries for active learning.
. */
	abstract protected Iterator<Instance> instancePool();

	/** Label an Instance chosen by the learner.  Return null if the
	 * query can't be answered, otherwise return a labeled version of
	 * the instance (an Example). */
	abstract protected Example labelInstance(Instance query);

	/** Return true if this teacher can answer more queries. */
	abstract protected boolean hasAnswers();
}
