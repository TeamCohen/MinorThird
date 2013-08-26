package edu.cmu.minorthird.text.learn;

import java.util.Iterator;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.util.ProgressCounter;

/**
 * Train a AnnotatorLearner and return the learned
 * Annotator, using some unspecified source of information to
 * get AnnotationExample's to train the learner.
 *
 * @author William Cohen
 */
public abstract class   AnnotatorTeacher
{
	final public Annotator train(AnnotatorLearner learner)
	{
		learner.reset();

		// unsupervised training
		learner.setDocumentPool( documentPool() );

		ProgressCounter pc =
			new ProgressCounter("presenting examples to AnnotatorLearner", "document");
		// active or passive learning from labeled data
		while (learner.hasNextQuery() && hasAnswers()) {
			Span query = learner.nextQuery();
			//answeredQuery different between commandline and mixup spanProp creation?
			AnnotationExample answeredQuery = labelInstance(query);
			if (answeredQuery!=null) {
			    //answeredquery does not include mixup created spanProp
				learner.setAnswer( answeredQuery );
				pc.progress();
			}
		}
		pc.finished();

		// final result
		return learner.getAnnotator();
	}

	/** Labels available for training, testing */
	abstract public TextLabels availableLabels();

	/** Unlabeled instances. */
	abstract public Iterator<Span> documentPool();

	/** Label an Span queried by the learner.  Return null if the query
	 * can't be answered, otherwise return an AnnotationExample. 
	 */
	abstract public AnnotationExample labelInstance(Span query);

	/** Return true if this teacher can answer more queries. */
	abstract public boolean hasAnswers();
}
