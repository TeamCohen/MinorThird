package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.BasicSpanLooper;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

import java.util.TreeSet;

/**
 * Abstract implementation of a SpanFinder.
 *
 * @author William Cohen
 */

public abstract class AbstractSpanFinder implements SpanFinder
{
	/** Find subspans of each span produced by the documentSpanLooper. */
	final public Span.Looper findSpans(TextLabels labels, Span.Looper documentSpanLooper) {
		TreeSet set = new TreeSet();
		while (documentSpanLooper.hasNext()) {
			for (Span.Looper i = findSpans(labels, documentSpanLooper.nextSpan()); i.hasNext(); ) {
				set.add( i.nextSpan() );
			}
		}
		return new BasicSpanLooper( set.iterator() );
	}

	/** Find subspans of the given document span. */
	abstract public Span.Looper findSpans(TextLabels labels, Span documentSpan);

	/** Explain how spans were found. */
	abstract public String explainFindSpans(TextLabels labels, Span documentSpan);
}
