package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.Mixup;

import java.util.Collections;

/**
 * Finds spans using a mixup expression evaluated in a fixed labeling.
 *
 * @author William Cohen
 */

public class MixupFinder implements SpanFinder
{
	private static final TextLabels EMPTY_LABELS = new EmptyLabels();

	private Mixup mixup;
	public MixupFinder(Mixup mixup)
	{ this.mixup = mixup; }

	public Span.Looper findSpans(TextLabels labels,Span.Looper documentSpanLooper)
	{ return mixup.extract( labels, documentSpanLooper ); }

	public Span.Looper findSpans(TextLabels labels,Span documentSpan)
	{
		Span.Looper singletonLooper = new BasicSpanLooper( Collections.singleton(documentSpan).iterator() );
		return findSpans( labels, singletonLooper );
	}

	public Details getDetails(Span s)
	{ return new Details(1.0, mixup); }

	public String explainFindSpans(TextLabels labels,Span documentSpan)
	{ return "Spans found using mixup expression: "+mixup; }
}
