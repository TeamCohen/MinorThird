package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.text.*;
import java.util.Set;

/** A subset of another TextLabels that can be added to.
 * Additions are propogated back to the underlying MonotonicTextLabels
 * passed in as an argument.
 *
 * @author William Cohen
*/

public class MonotonicSubTextLabels extends SubTextLabels implements MonotonicTextLabels
{
	private MonotonicTextLabels monotonicLabels;

	public void setAnnotatorLoader(AnnotatorLoader newLoader) { monotonicLabels.setAnnotatorLoader(newLoader); }

	public AnnotatorLoader getAnnotatorLoader() { return monotonicLabels.getAnnotatorLoader(); }

	public MonotonicSubTextLabels(SubTextBase subBase,MonotonicTextLabels labels) {
		super(subBase,labels);
		this.monotonicLabels = labels;
	}

	public void defineDictionary(String dictName,Set dict) {
		monotonicLabels.defineDictionary( dictName, dict );
	}
		
	public void setProperty(Token token,String prop,String value) {
		monotonicLabels.setProperty(token,prop,value);
	}

	public void setProperty(Span span,String prop,String value) {
		if (subBase.contains(span))	
			monotonicLabels.setProperty(span,prop,value);
	}

	public void addToType(Span span,String type) {
		if (subBase.contains(span))	
			monotonicLabels.addToType(span,type);
	}

	public void addToType(Span span,String type,Details details) {
		if (subBase.contains(span))	
			monotonicLabels.addToType(span,type,details);
	}

	public void declareType(String type) {
		monotonicLabels.declareType(type);
	}
	
	public void require(String annotationType,String fileToLoad) { 
		monotonicLabels.require(annotationType,fileToLoad);
	}

	public void require(String annotationType,String fileToLoad,AnnotatorLoader loader) { 
		monotonicLabels.require(annotationType,fileToLoad,loader);
	}

}

