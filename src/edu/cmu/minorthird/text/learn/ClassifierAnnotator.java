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
	private String spanProp=null, spanType=null, candidateType=null;
	
	public 
	ClassifierAnnotator(SpanFeatureExtractor fe,Classifier c,String spanType,String spanProp,String candidateType) 
	{ 
		this.fe=fe; this.c=c; this.spanType=spanType; this.spanProp=spanProp; this.candidateType=candidateType;
	}
	public 
	ClassifierAnnotator(SpanFeatureExtractor fe,Classifier c,String spanType,String spanProp) 
	{ 
		this(fe,c,spanType,spanProp,null);
	}

	/** The feature extractor applied to candidate spans. */
	public SpanFeatureExtractor getFE() { return fe; }

	/* The classifier used on Instances extracted from candidate spans
	 * by the SpanFeatureExtractor getFE() */
	public Classifier getClassifier() { return c; }
	
	/** If non-null, the property used to encode the output of the classifier. */
	public String getSpanProperty() { return spanProp; }

	/** If non-null, the spanType used to encode the positive predictions of
	 * the classifier (which should be a BinaryClassifier). */
	public String getSpanType() { return spanType; }

	/** If non-null, the spanType corresponding to candidate spans to be
	 * classified.  If null, the document spans will be classified. */
	public String getCandidateType() { return candidateType; }

	public void doAnnotate(MonotonicTextLabels labels)
	{
		Span.Looper candidateLooper = 
			candidateType!=null ? 
			labels.instanceIterator(candidateType) : labels.getTextBase().documentSpanIterator();

		for (Span.Looper i=candidateLooper; i.hasNext(); ) {
			Span s = i.nextSpan();
			ClassLabel classOfS = c.classification(fe.extractInstance(labels, s));
			if (spanProp!=null) labels.setProperty(s, spanProp, classOfS.bestClassName());
			if (spanType!=null && classOfS.isPositive()) labels.addToType(s,spanType);
		}
	}
	public String explainAnnotation(TextLabels labels,Span documentSpan)
	{
		return c.explain(fe.extractInstance(documentSpan));
	}
}
