package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.multi.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;

/** 
 * An annotator that uses a learned Classifier to mark up document spans.
 */

public class MultiClassifierAnnotator extends AbstractAnnotator implements Serializable 
{
    static private final long serialVersionUID = 1;
    private final int CURRENT_VERSION_NUMBER = 1;
    
    private SpanFeatureExtractor fe;
    private MultiClassifier mc;
    private String[] multiSpanProp=null;
	
	public MultiClassifierAnnotator(SpanFeatureExtractor fe,MultiClassifier mc,String[] multiSpanProp) 
	{ 
		this.fe=fe; this.mc=mc; this.multiSpanProp=multiSpanProp;
	}

	/** The feature extractor applied to candidate spans. */
	public SpanFeatureExtractor getFE() { return fe; }

	/* The classifier used on Instances extracted from candidate spans
	 * by the SpanFeatureExtractor getFE() */
	public MultiClassifier getMultiClassifier() { return mc; }
	
	/** If non-null, the property used to encode the output of the classifier. */
	public String[] getMultiSpanProperty() { return multiSpanProp; }

	public void doAnnotate(MonotonicTextLabels labels)
	{
		Span.Looper candidateLooper =  labels.getTextBase().documentSpanIterator();

		for (Span.Looper i=candidateLooper; i.hasNext(); ) {
			Span s = i.nextSpan();
			MultiClassLabel classOfS = mc.multiLabelClassification(fe.extractInstance(labels, s));
			String[] bestClassNames = classOfS.bestClassName();
			for(int j=0; j<bestClassNames.length; j++) {
			    labels.setProperty(s, multiSpanProp[j], "_predicted_"+bestClassNames[j]);			   
			}
		}
	}
	public String explainAnnotation(TextLabels labels,Span documentSpan)
	{
	    return mc.explain(fe.extractInstance(labels,documentSpan));
	}
}