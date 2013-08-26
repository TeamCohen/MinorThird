package edu.cmu.minorthird.text.learn;

import java.io.Serializable;
import java.util.Iterator;

import edu.cmu.minorthird.classify.multi.MultiClassLabel;
import edu.cmu.minorthird.classify.multi.MultiClassifier;
import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

/**
 * An annotator that uses a learned Classifier to mark up document spans.
 */

public class MultiClassifierAnnotator extends AbstractAnnotator implements
		Serializable{

	static private final long serialVersionUID=20080306L;

	private SpanFeatureExtractor fe;

	private MultiClassifier mc;

	private String[] multiSpanProp=null;

	public MultiClassifierAnnotator(SpanFeatureExtractor fe,MultiClassifier mc,
			String[] multiSpanProp){
		this.fe=fe;
		this.mc=mc;
		this.multiSpanProp=multiSpanProp;
	}

	/** The feature extractor applied to candidate spans. */
	public SpanFeatureExtractor getFE(){
		return fe;
	}

	/*
	 * The classifier used on Instances extracted from candidate spans by the
	 * SpanFeatureExtractor getFE()
	 */
	public MultiClassifier getMultiClassifier(){
		return mc;
	}

	/** If non-null, the property used to encode the output of the classifier. */
	public String[] getMultiSpanProperty(){
		return multiSpanProp;
	}

	@Override
	public void doAnnotate(MonotonicTextLabels labels){
		Iterator<Span> candidateLooper=labels.getTextBase().documentSpanIterator();

		for(Iterator<Span> i=candidateLooper;i.hasNext();){
			Span s=i.next();
			MultiClassLabel classOfS=
					mc.multiLabelClassification(fe.extractInstance(labels,s));
			String[] bestClassNames=classOfS.bestClassName();
			for(int j=0;j<bestClassNames.length;j++){
				labels.setProperty(s,multiSpanProp[j],"_predicted_"+bestClassNames[j]);
			}
		}
	}

	@Override
	public String explainAnnotation(TextLabels labels,Span documentSpan){
		return mc.explain(fe.extractInstance(labels,documentSpan));
	}
}