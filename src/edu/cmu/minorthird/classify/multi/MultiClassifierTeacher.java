package edu.cmu.minorthird.classify.multi;

import java.util.Iterator;

import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Instance;

/**
 * Interface for something that trains multi label classifiers.
 *
 * @author Cameron Williams
 */

public abstract class MultiClassifierTeacher
{
	/** Train a ClassifierLearner and return the learned Classifier, using
	 * some unspecified source of information to get labels.  
	 */
	final public MultiClassifier train(ClassifierLearner learner) 
	{
		MultiLearner multiLearner = new MultiLearner(learner);

		// initialize the multiLearner for a new problem
		multiLearner.reset();

		// tell multiLearner the schema of examples
		multiLearner.setMultiSchema( schema() );

		// provide unlabeled examples to the multiLearner, for unsupervised
		// training, semi-supervised training, or active multiLearner
		multiLearner.setInstancePool( instancePool() );

		// passive learning from already-available labeled data
		for (Iterator<MultiExample> i=examplePool(); i.hasNext(); ) {
		    multiLearner.addMultiExample( i.next() );
		}

		// active learning 
		while (multiLearner.hasNextQuery() && hasAnswers()) {
		    Instance query = multiLearner.nextQuery();
		    MultiExample answeredQuery = labelInstance(query);
		    if (answeredQuery!=null) {
			multiLearner.addMultiExample( answeredQuery );
		    }
		}

		// signal that there's no more data available
		multiLearner.completeTraining();

		// final result
		return multiLearner.getMultiClassifier();
	}

	//
	// subclasses implement these steps
	//

	/** The set of classes that will be used.
	 */ 
	abstract protected MultiExampleSchema schema();

	/** Labeled instances that will be sent to the multiLearner
	 * via a call to addExample().
	 */
	abstract protected Iterator<MultiExample> examplePool();

	/** Unlabeled instances, which will be provided to the multiLearner via
	 * setInstancePool().  These can be used for semi-supervised
	 * multiLearner, or to form queries for active learning.
	 . */
	abstract protected Iterator<Instance> instancePool();

	/** Label an Instance chosen by the multiLearner.  Return null if the
	 * query can't be answered, otherwise return a labeled version of
	 * the instance (an Example). */
	abstract protected MultiExample labelInstance(Instance query);

	/** Return true if this teacher can answer more queries. */
	abstract protected boolean hasAnswers();
}

