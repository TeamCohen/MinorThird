package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.Annotator;

/**
 * Learn an Annotator from AnnotationExample's.
 *
 * @author William Cohen
 */

public interface AnnotatorLearner
{
	/** Accept a pool of documents. */
	public void setDocumentPool(Span.Looper documents);
	
	/** Returns true if the learner has more queries to answer. */
	public boolean hasNextQuery(); 

	/** Returns an Span which the learner would like labeled. */
	public Span nextQuery();

	/** Accept the answer to the last query. */
	public void setAnswer(edu.cmu.minorthird.text.learn.AnnotationExample answeredQuery);

	/** Set the label used for annotations produced by the learner. */
	public void setAnnotationType(String s);

	/** Get the label used for annotations produced by the learner. */
	public String getAnnotationType();

	/** Return the learned annotator */
	public Annotator getAnnotator();

}
