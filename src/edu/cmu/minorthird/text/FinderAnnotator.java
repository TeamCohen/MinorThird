package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;

/**
 * Annotator based on a SpanFinder.
 *
 * @author William Cohen
 */

public class FinderAnnotator extends AbstractAnnotator implements SpanFinder
{
	private SpanFinder finder;
	private String annotationType;

	/** Create an annotator which annotates all spans found by the
	 * SpanFinder with the appropriate type. */
	public FinderAnnotator(SpanFinder finder,String annotationType) {
		this.finder = finder;
		this.annotationType = annotationType;
	}
	
	public Details getDetails(Span foundSpan) {
		return finder.getDetails(foundSpan);
	}

	public String toString() { return "[FinderAnnotator "+annotationType+" "+finder+"]"; }

	protected void doAnnotate(MonotonicTextLabels labels) {
		for (Span.Looper i=finder.findSpans(labels, labels.getTextBase().documentSpanIterator()); i.hasNext(); ) {
			labels.addToType( i.nextSpan(), annotationType );
		}
	}

	public String explainAnnotation(TextLabels labels, Span documentSpan) {
		return finder.explainFindSpans(labels, documentSpan);
	}

	//
	// implements SpanFinder by delegation
	//
	public Span.Looper findSpans(TextLabels labels, Span.Looper documentSpanLooper)
	{ 
		return finder.findSpans(labels, documentSpanLooper);
	}
  public Span.Looper findSpans(TextLabels labels,Span documentSpan)
	{ 
		return finder.findSpans(labels, documentSpan);
	}
	public String explainFindSpans(TextLabels labels, Span documentSpan)
	{ 
		return finder.explainFindSpans(labels, documentSpan);
	}
}
