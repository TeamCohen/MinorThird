package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.Mixup;

import java.util.Collections;

/**
 * Finds spans using a mixup expression evaluated in a fixed environment.
 *
 * @author William Cohen
 */

public class MixupFinder implements SpanFinder
{
	private static final TextEnv EMPTY_ENV = new EmptyEnv();

	private Mixup mixup;
	public MixupFinder(Mixup mixup)
	{ 
		this.mixup = mixup; 
	}
	public Span.Looper findSpans(TextEnv env,Span.Looper documentSpanLooper)
	{
		return mixup.extract( env, documentSpanLooper );
	}
	public Span.Looper findSpans(TextEnv env,Span documentSpan)
	{
		Span.Looper singletonLooper = new BasicSpanLooper( Collections.singleton(documentSpan).iterator() );
		return findSpans( env, singletonLooper );
	}
	public Details getDetails(Span s)
	{
		return new Details(1.0, mixup);
	}
	public String explainFindSpans(TextEnv env,Span documentSpan)
	{
		return "Spans found using mixup expression: "+mixup;
	}
}
