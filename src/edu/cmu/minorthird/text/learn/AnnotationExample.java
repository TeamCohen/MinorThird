package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;

/**
 * Feedback for an annotation learner.
 *
 * @author William Cohen
 */

public class AnnotationExample
{
	public static String OUTSIDE = "outside";
	public static String INSIDE = "inside";
	public static String START = "start";
	public static String END = "end";
	public static String NOT_START_OR_END = "notStartOrEnd";
	private String inputSpanType;
	private String inputSpanProp;

	private Span document;
	private TextLabels labels;

	/** 
	 * @param document give feedback to learner about this document
	 * @param labels feedback information is in these labels
	 * @param inputSpanType learner will learn how to extract spans of this type
	 * @param inputSpanProp learner will classify extracted spans according to this type
	 */
	public AnnotationExample(Span document,TextLabels labels,String inputSpanType,String inputSpanProp)
	{
		this.document = document;
		this.labels = labels;
		this.inputSpanType = inputSpanType;
		this.inputSpanProp = inputSpanProp;
	}

	public Span getDocumentSpan()
	{ return document; }

	public TextLabels getLabels()
	{ return labels; }

	public String getInputType()
	{ return inputSpanType; }

	public String getInputProp()
	{ return inputSpanProp; }

	public String toString() {
		return "[AnnEx: document="+document+"]";
	}

	//
	// convenience methods
	//
	public TextLabels labelTokensInsideOutside(String prop)
	{
		MonotonicTextLabels result = new NestedTextLabels(labels);
		String documentId = document.getDocumentId();
		labelTokens(result,result.closureIterator(inputSpanType,documentId),prop,OUTSIDE);
		labelTokens(result,result.instanceIterator(inputSpanType,documentId),prop,INSIDE);
		return result;
	}

	public TextLabels labelTokensStartEnd(String prop)
	{
		MonotonicTextLabels result = new NestedTextLabels(labels);
		String documentId = document.getDocumentId();
		labelTokens(result,result.closureIterator(inputSpanType,documentId),prop,NOT_START_OR_END);
		for (Span.Looper i=result.instanceIterator(inputSpanType,documentId); i.hasNext(); ) {
			Span s = i.nextSpan();
			if (s.size()>0) {
				result.setProperty( s.getToken(0), prop, START );
				result.setProperty( s.getToken(s.size()-1), prop, END );
			}
		}
		return result;
	}

	private void labelTokens(MonotonicTextLabels labels,Span.Looper i,String prop,String value)
	{
		while (i.hasNext()) {
			Span s = i.nextSpan();
			for (int j=0; j<s.size(); j++) {
				labels.setProperty( s.getToken(j), prop, value);
			}
		}
	}
}
