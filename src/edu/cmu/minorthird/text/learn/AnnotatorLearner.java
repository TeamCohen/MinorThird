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
	public void reset();

	/** Accept a pool of documents. */
	public void setDocumentPool(Span.Looper documents);
	
	/** Returns true if the learner has more queries to answer. */
	public boolean hasNextQuery(); 

	/** Returns an Span which the learner would like labeled. */
	public Span nextQuery();

	/** Accept the answer to the last query. */
	public void setAnswer(AnnotationExample answeredQuery);

	/** Set the label used for annotations produced by the learner. */
	public void setAnnotationType(String s);

	/** Get the label used for annotations produced by the learner. */
	public String getAnnotationType();

	/** Return the learned annotator */
	public Annotator getAnnotator();

	/** Return the span feature extractor used by this annotator.  This could be null
	 * if no such feature extractor exists. 
	 */
	public SpanFeatureExtractor getSpanFeatureExtractor();

	/** Set the feature extractor used by this annotator.  This may 
	 * have no action if no such feature extractor exists.
	 */
	public void setSpanFeatureExtractor(SpanFeatureExtractor fe);
}
