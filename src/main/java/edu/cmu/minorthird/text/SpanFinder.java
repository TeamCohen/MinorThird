package edu.cmu.minorthird.text;

import java.util.Iterator;

/**
 * Finds subspans of document spans.  This is much like an annotator,
 * but a little more lightweight.
 *
 * @author William Cohen
 */

public interface SpanFinder
{
	/** Find subspans of each span produced by the documentSpanLooper. */
	public Iterator<Span> findSpans(TextLabels labels, Iterator<Span> documentSpanLooper);

	/** Find subspans of the given document span. */
  public Iterator<Span> findSpans(TextLabels labels, Span documentSpan);

	/** Return 'details' about some span found by the previous
	 * call to findSpans(Span documentSpan). */
	public Details getDetails(Span foundSpan);

	/** Explain how spans were found. */
	public String explainFindSpans(TextLabels labels, Span documentSpan);
}
