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

	protected void doAnnotate(MonotonicTextEnv env) {
		for (Span.Looper i=finder.findSpans(env, env.getTextBase().documentSpanIterator()); i.hasNext(); ) {
			env.addToType( i.nextSpan(), annotationType );
		}
	}

	public String explainAnnotation(TextEnv env,Span documentSpan) {
		return finder.explainFindSpans(env,documentSpan);
	}

	//
	// implements SpanFinder by delegation
	//
	public Span.Looper findSpans(TextEnv env,Span.Looper documentSpanLooper)
	{ 
		return finder.findSpans(env,documentSpanLooper);
	}
  public Span.Looper findSpans(TextEnv env,Span documentSpan)
	{ 
		return finder.findSpans(env,documentSpan);
	}
	public String explainFindSpans(TextEnv env,Span documentSpan)
	{ 
		return finder.explainFindSpans(env,documentSpan);
	}
}
