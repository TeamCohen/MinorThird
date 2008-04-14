package edu.cmu.minorthird.text.learn.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.minorthird.text.AnnotatorLoader;
import edu.cmu.minorthird.text.Details;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.Token;
import edu.cmu.minorthird.text.gui.ZoomingTextLabelsViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * A subset of another TextLabels.
 * 
 * @author William Cohen
 */

public class SubTextLabels implements TextLabels,Visible{

	protected SubTextBase subBase;

	protected TextLabels labels;

	public SubTextLabels(SubTextBase subBase,TextLabels labels){
		this.subBase=subBase;
		this.labels=labels;
	}

	public boolean isAnnotatedBy(String s){
		return labels.isAnnotatedBy(s);
	}

	public void setAnnotatedBy(String s){
		throw new UnsupportedOperationException(
				"can't do SubTextBase.setAnnotatedBy()");
	}

	public TextBase getTextBase(){
		return subBase;
	}

	public boolean hasDictionary(String dictionary){
		return labels.hasDictionary(dictionary);
	}

	public boolean inDict(Token token,String dict){
		return labels.inDict(token,dict);
	}

	public String getProperty(Token token,String prop){
		return labels.getProperty(token,prop);
	}

	public Set<String> getTokenProperties(){
		return labels.getTokenProperties();
	}

	public String getProperty(Span span,String prop){
		return subBase.contains(span)?labels.getProperty(span,prop):null;
	}

	public Set<String> getSpanProperties(){
		return labels.getSpanProperties();
	}

	public Iterator<Span> getSpansWithProperty(String prop){
		return filter(labels.getSpansWithProperty(prop));
	}

	public Iterator<Span> getSpansWithProperty(String prop,String id){
		return filter(labels.getSpansWithProperty(prop,id));
	}

	public boolean hasType(Span span,String type){
		return subBase.contains(span)?labels.hasType(span,type):false;
	}

	public Iterator<Span> instanceIterator(String type){
		return filter(labels.instanceIterator(type));
	}

	public Iterator<Span> instanceIterator(String type,String documentId){
		if(subBase.documentSpan(documentId)!=null)
			return labels.instanceIterator(type,documentId);
		else
			return Collections.EMPTY_SET.iterator();
	}

	public Set<String> getTypes(){
		return labels.getTypes();
	}

	public Set<Span> getTypeSet(String type,String documentId){
		return labels.getTypeSet(type,documentId);
	}

	public boolean isType(String type){
		return labels.isType(type);
	}

	public Iterator<Span> closureIterator(String type){
		return filter(labels.closureIterator(type));
	}

	public Iterator<Span> closureIterator(String type,String documentId){
		if(subBase.documentSpan(documentId)!=null)
			return labels.closureIterator(type,documentId);
		else
			return Collections.EMPTY_SET.iterator();
	}

	public String showTokenProp(TextBase base,String prop){
		return labels.showTokenProp(base,prop);
	}

	public Details getDetails(Span span,String type){
		return (subBase.contains(span))?labels.getDetails(span,type):null;
	}

	public void require(String annotationType,String fileToLoad){
		labels.require(annotationType,fileToLoad);
	}

	public void require(String annotationType,String fileToLoad,
			AnnotatorLoader loader){
		labels.require(annotationType,fileToLoad,loader);
	}

	/** Annotate labels with annotator named fileToLoad */
	public void annotateWith(String annotationType,String fileToLoad){
		labels.annotateWith(annotationType,fileToLoad);
	}

	private Iterator<Span> filter(Iterator<Span> i){
		List<Span> list=new ArrayList<Span>();
		while(i.hasNext()){
			Span span=i.next();
			if(subBase.contains(span))
				list.add(span);
		}
		return list.iterator();
	}

	/** Assert that Span span has the given value of the given property */
	public void setProperty(Span span,String prop,String value){
		System.out.println("Not used");
	}

	public Viewer toGUI(){
		return new ZoomingTextLabelsViewer(this);
	}
}
