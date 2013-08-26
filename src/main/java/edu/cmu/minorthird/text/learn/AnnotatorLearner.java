package edu.cmu.minorthird.text.learn;

import java.util.Iterator;

import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.Annotator;

/**
 * Learn an Annotator from AnnotationExample's.
 *
 * @author William Cohen
 */

public abstract class AnnotatorLearner
{

	abstract public void reset();

	/** Accept a pool of documents. */
	abstract public void setDocumentPool(Iterator<Span> documents);
	
	/** Returns true if the learner has more queries to answer. */
	abstract public boolean hasNextQuery(); 

	/** Returns an Span which the learner would like labeled. */
	abstract public Span nextQuery();

	/** Accept the answer to the last query. */
	abstract public void setAnswer(AnnotationExample answeredQuery);

	/** Set the label used for annotations produced by the learner. */
	abstract public void setAnnotationType(String s);

	/** Get the label used for annotations produced by the learner. */
	abstract public String getAnnotationType();

	/** Return the learned annotator */
	abstract public Annotator getAnnotator();

	/** Return the span feature extractor used by this annotator.  This could be null
	 * if no such feature extractor exists. 
	 */
	abstract public SpanFeatureExtractor getSpanFeatureExtractor();

	/** Set the feature extractor used by this annotator.  This may 
	 * have no action if no such feature extractor exists.
	 */
	abstract public void setSpanFeatureExtractor(SpanFeatureExtractor fe);
    
    public String getAnnotationTypeHelp() { return "Get the label used for annotations produced by the learner"; }
    public String getSpanFeatureExtractorHelp() { return "<html> Set the feature extractor used by this learner <br> "; }
}
