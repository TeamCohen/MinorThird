package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.util.gui.*;

import java.util.*;

/** A subset of another TextLabels.
 *
 * @author William Cohen
*/

public class SubTextLabels implements TextLabels,Visible
{
	protected SubTextBase subBase;
	protected TextLabels labels;

	public SubTextLabels(SubTextBase subBase,TextLabels labels) {
		this.subBase = subBase; this.labels = labels;
	}

	public boolean isAnnotatedBy(String s) {
		return labels.isAnnotatedBy(s);
	}

	public void setAnnotatedBy(String s) {
		throw new UnsupportedOperationException("can't do SubTextBase.setAnnotatedBy()");
	}

	public TextBase getTextBase() {
		return subBase;
	}

  public boolean hasDictionary(String dictionary)
  {
    return labels.hasDictionary(dictionary);
  }

  public boolean inDict(Token token,String dict) {
		return labels.inDict(token,dict);
	}

	public String getProperty(Token token,String prop) {
		return labels.getProperty(token,prop);
	}

	public Set getTokenProperties() {
		return labels.getTokenProperties();
	}		

	public String getProperty(Span span,String prop) {
		return subBase.contains(span) ? labels.getProperty(span,prop) : null;
	}

	public Set getSpanProperties() {
		return labels.getSpanProperties();
	}

	public Span.Looper getSpansWithProperty(String prop) {
		return filter( labels.getSpansWithProperty(prop) );
	}

	public Span.Looper getSpansWithProperty(String prop,String id) {
		return filter( labels.getSpansWithProperty(prop,id) );
	}

	public boolean hasType(Span span,String type) {
		return subBase.contains(span) ? labels.hasType(span,type) : false;
	}

	public Span.Looper instanceIterator(String type) {
		return filter( labels.instanceIterator(type) );
	}

	public Span.Looper instanceIterator(String type,String documentId) {
		if (subBase.documentSpan(documentId)!=null)
			return labels.instanceIterator(type,documentId);
		else
			return new BasicSpanLooper( Collections.EMPTY_SET.iterator() );
	}

	public Set getTypes() {
		return labels.getTypes();
	}

	public boolean isType(String type) {
		return labels.isType(type);
	}

	public Span.Looper closureIterator(String type) {
		return filter( labels.closureIterator(type) );
	}

	public Span.Looper closureIterator(String type, String documentId) {
		if (subBase.documentSpan(documentId)!=null)
			return labels.closureIterator(type,documentId);
		else
			return new BasicSpanLooper( Collections.EMPTY_SET.iterator() );
	}

	public String showTokenProp(TextBase base, String prop) {
		return labels.showTokenProp( base, prop);
	}

	public Details getDetails(Span span,String type) {
		return (subBase.contains(span)) ? labels.getDetails(span,type): null;
	}

	public void require(String annotationType,String fileToLoad) { 
		labels.require(annotationType,fileToLoad);
	}

	public void require(String annotationType,String fileToLoad,AnnotatorLoader loader) { 
		labels.require(annotationType,fileToLoad,loader);
	}

	private Span.Looper filter(Span.Looper i) {
		List list = new ArrayList();
		while (i.hasNext()) {
			Span span = i.nextSpan();
			if (subBase.contains(span)) 
				list.add( span );
		}
		return new BasicSpanLooper( list.iterator() );
	}

	public Viewer toGUI() 
	{
		return new ZoomingTextLabelsViewer(this);
	}
}

