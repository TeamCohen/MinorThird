package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.*;

/**
 * Annotator based on a SpanFinder.
 *
 * @author William Cohen
 */

public class FinderAnnotator extends AbstractAnnotator implements SpanFinder
{
	private SpanFinder finder;
	private String annotationType;
	private boolean flattenSpans;

	/** Create an annotator which annotates all spans found by the
	 * SpanFinder with the appropriate type. If flattenSpans==true,
	 * then overlapping spans will be combined. */
	public FinderAnnotator(SpanFinder finder,String annotationType,boolean flattenSpans) {
		this.finder = finder;
		this.annotationType = annotationType;
		this.flattenSpans = flattenSpans;
	}
	
	/** Create an annotator which annotates all spans found by the
	 * SpanFinder with the appropriate type. */
	public FinderAnnotator(SpanFinder finder,String annotationType) {
		this(finder,annotationType,false);
	}

	public Details getDetails(Span foundSpan) {
		return finder.getDetails(foundSpan);
	}

	public String toString() { return "[FinderAnnotator "+annotationType+" "+finder+"]"; }

	protected void doAnnotate(MonotonicTextLabels labels) 
	{
		if (!flattenSpans) {
			for (Span.Looper i=finder.findSpans(labels, labels.getTextBase().documentSpanIterator()); i.hasNext(); ) {
				labels.addToType( i.nextSpan(), annotationType );
			}
		} else {
			MonotonicTextLabels tmpLabels = new NestedTextLabels(labels);
			String tmpPropName = "_foundByFinder";
			while (tmpLabels.getTokenProperties().contains(tmpPropName)) {
				tmpPropName = tmpPropName + "_";
			}
			String tmpSpanName = "_flattened";
			while (tmpLabels.getTokenProperties().contains(tmpSpanName)) {
				tmpSpanName = tmpSpanName + "_";
			}
			for (Span.Looper i=finder.findSpans(labels, labels.getTextBase().documentSpanIterator()); i.hasNext(); ) {
				Span s = i.nextSpan();
				for (int j=0; j<s.size(); j++) {
					tmpLabels.setProperty( s.getToken(j), tmpPropName, "t");
				}
			}
			try {
				MixupProgram p = new MixupProgram(new String[]{
					"defSpanType "+tmpSpanName+" =: ... [L "+tmpPropName+":t+ R] ..."});
				p.eval(tmpLabels,tmpLabels.getTextBase());
			} catch (Mixup.ParseException e) {
				throw new IllegalStateException("parse error: "+e);
			}
			for (Span.Looper i=tmpLabels.instanceIterator(tmpSpanName); i.hasNext(); ) {
				labels.addToType( i.nextSpan(), annotationType );
			}
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
