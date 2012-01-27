package edu.cmu.minorthird.text.learn;

import java.io.Serializable;
import java.util.Iterator;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

/** 
 * An annotator that uses a learned Classifier to mark up document spans.
 */

public class ClassifierAnnotator extends AbstractAnnotator implements
		Serializable{

	static private final long serialVersionUID=20080306L;

	private SpanFeatureExtractor fe;

	private Classifier c;

	private String spanProp=null,spanType=null,candidateType=null;

	public ClassifierAnnotator(SpanFeatureExtractor fe,Classifier c,
			String spanType,String spanProp,String candidateType){
		this.fe=fe;
		this.c=c;
		this.spanType=spanType;
		this.spanProp=spanProp;
		this.candidateType=candidateType;
	}

	public ClassifierAnnotator(SpanFeatureExtractor fe,Classifier c,
			String spanType,String spanProp){
		this(fe,c,spanType,spanProp,null);
	}

	/** The feature extractor applied to candidate spans. */
	public SpanFeatureExtractor getFE(){
		return fe;
	}

	/* The classifier used on Instances extracted from candidate spans
	 * by the SpanFeatureExtractor getFE() */
	public Classifier getClassifier(){
		return c;
	}

	/** If non-null, the property used to encode the output of the classifier. */
	public String getSpanProperty(){
		return spanProp;
	}

	/** If non-null, the spanType used to encode the positive predictions of
	 * the classifier (which should be a BinaryClassifier). */
	public String getSpanType(){
		return spanType;
	}

	/** If non-null, the spanType corresponding to candidate spans to be
	 * classified.  If null, the document spans will be classified. */
	public String getCandidateType(){
		return candidateType;
	}

	@Override
	public void doAnnotate(MonotonicTextLabels labels){
		Iterator<Span> candidateLooper=
				candidateType!=null?labels.instanceIterator(candidateType):labels
						.getTextBase().documentSpanIterator();

		for(Iterator<Span> i=candidateLooper;i.hasNext();){
			Span s=i.next();
			ClassLabel classOfS=c.classification(fe.extractInstance(labels,s));
			if(spanProp!=null){
				//labels.setProperty(s, spanProp, classOfS.bestClassName(),new Details(classOfS.bestWeight()));
				labels.setProperty(s,spanProp,classOfS.bestClassName());
			}else if(spanType!=null&&classOfS.isPositive()){
				//labels.addToType(s,spanType,new Details(classOfS.posWeight()));
				labels.addToType(s,spanType);
			}
		}
	}

	@Override
	public String explainAnnotation(TextLabels labels,Span documentSpan){
		return c.explain(fe.extractInstance(labels,documentSpan));
	}
}
