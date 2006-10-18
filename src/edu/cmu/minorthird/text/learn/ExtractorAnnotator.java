package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;

/** 
 * An annotator that uses a learned extractor to mark up document spans.
 */

public interface ExtractorAnnotator extends Annotator
{
	/** The spanType used to encode the spans extracted by the
	 * ExtractorAnnotator. */
	public String getSpanType();
}
