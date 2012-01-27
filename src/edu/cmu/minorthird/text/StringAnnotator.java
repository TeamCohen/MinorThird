package edu.cmu.minorthird.text;

import java.util.Iterator;


/**
 * An abstract annotator that is based on marking up substrings within
 * a string, using the CharAnnotation class.
 * 
 * This is a bad class, can only define types and not properties
 *
 * @author ksteppe
 */

public abstract class StringAnnotator extends AbstractAnnotator{

	protected String providedAnnotation=null;

	@Override
	protected void doAnnotate(MonotonicTextLabels labels){
		//add the annotations into labels
		TextBase textBase=labels.getTextBase();
		for(Iterator<Span> it=textBase.documentSpanIterator();it.hasNext();){
			Span span=it.next();
			String spanString=span.asString();

			CharAnnotation[] annotations=annotateString(spanString);

			if(annotations!=null){
				for(int i=0;i<annotations.length;i++){
					CharAnnotation ann=annotations[i];
					int lo=ann.getOffset();
					Span newSpan=span.charIndexSubSpan(lo,lo+ann.getLength());
					labels.addToType(newSpan,ann.getType());
					labels.setProperty(newSpan,ann.getType(),"1");
					for(int j=0;j<newSpan.size();j++){
						labels.setProperty(newSpan.getToken(j),ann.getType(),"1");
					}
				}
			}
		}
		if(providedAnnotation!=null)
			labels.setAnnotatedBy(providedAnnotation);
	}

	protected String[] closedTypes(){
		return null;
	}

	/** Override this class to provide the actual annotations for a span.
	 */
	protected abstract CharAnnotation[] annotateString(String spanString);

}
