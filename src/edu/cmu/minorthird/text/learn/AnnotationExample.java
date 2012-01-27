package edu.cmu.minorthird.text.learn;

import java.util.Iterator;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.classify.*;
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

	@Override
	public String toString() {
		return "[AnnEx: document="+document+"]";
	}

	/** Return the name of the class associated with this span.  If
	 * inputSpanType is defined, the class name will be POS or NEG;
	 * otherwise, if inputSpanProp is defined, the class name will be
	 * the property value assigned, or NEG.
	 */
	public String getClassName(Span span)
	{
		String className = ExampleSchema.NEG_CLASS_NAME;
		if (getInputType()!=null) {
			if (getLabels().hasType(span,getInputType())) 
				className = ExampleSchema.POS_CLASS_NAME;
		} else if (getInputProp()!=null) {
			String propValue = getLabels().getProperty(span, getInputProp());
				if (propValue!=null) 
					className = propValue;
		} else {
			throw new IllegalStateException("inputType && inputProp undefined for answeredQuery: "+this);
		}
		return className;
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
		for (Iterator<Span> i=result.instanceIterator(inputSpanType,documentId); i.hasNext(); ) {
			Span s = i.next();
			if (s.size()>0) {
				result.setProperty( s.getToken(0), prop, START );
				result.setProperty( s.getToken(s.size()-1), prop, END );
			}
		}
		return result;
	}

	private void labelTokens(MonotonicTextLabels labels,Iterator<Span> i,String prop,String value)
	{
		while (i.hasNext()) {
			Span s = i.next();
			for (int j=0; j<s.size(); j++) {
				labels.setProperty( s.getToken(j), prop, value);
			}
		}
	}
}
