package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.classify.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;

/** 
 * An annotator that uses a learned Classifier to mark up document spans.
 */

public class ClassifierAnnotator extends AbstractAnnotator implements Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;
	
	private SpanFeatureExtractor fe;
	private Classifier c;
	private String spanProp=null, spanType=null;
	
	public ClassifierAnnotator(SpanFeatureExtractor fe,Classifier c,String spanType,String spanProp) 
	{ 
		this.fe=fe; this.c=c; this.spanType=spanType; this.spanProp=spanProp;
	}
	public SpanFeatureExtractor getFE() { return fe; }
	public Classifier getClassifier() { return c; }
	public String getSpanProperty() { return spanProp; }
	public String getSpanType() { return spanType; }

	public void doAnnotate(MonotonicTextLabels labels)
	{
		for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
			Span s = i.nextSpan();
			ClassLabel classOfS = c.classification(fe.extractInstance(s));
			if (spanProp!=null) labels.setProperty(s, spanProp, classOfS.bestClassName());
			if (spanType!=null && classOfS.isPositive()) labels.addToType(s,spanType);
		}
	}
	public String explainAnnotation(TextLabels labels,Span documentSpan)
	{
		return c.explain(fe.extractInstance(documentSpan));
	}
}
