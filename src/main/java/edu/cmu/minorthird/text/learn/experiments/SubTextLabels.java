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

	@Override
	public boolean isAnnotatedBy(String s){
		return labels.isAnnotatedBy(s);
	}

	public void setAnnotatedBy(String s){
		throw new UnsupportedOperationException(
				"can't do SubTextBase.setAnnotatedBy()");
	}

	@Override
	public TextBase getTextBase(){
		return subBase;
	}

	@Override
	public boolean hasDictionary(String dictionary){
		return labels.hasDictionary(dictionary);
	}

	@Override
	public boolean inDict(Token token,String dict){
		return labels.inDict(token,dict);
	}

	@Override
	public String getProperty(Token token,String prop){
		return labels.getProperty(token,prop);
	}

	@Override
	public Set<String> getTokenProperties(){
		return labels.getTokenProperties();
	}

	@Override
	public String getProperty(Span span,String prop){
		return subBase.contains(span)?labels.getProperty(span,prop):null;
	}

	@Override
	public Set<String> getSpanProperties(){
		return labels.getSpanProperties();
	}

	@Override
	public Iterator<Span> getSpansWithProperty(String prop){
		return filter(labels.getSpansWithProperty(prop));
	}

	@Override
	public Iterator<Span> getSpansWithProperty(String prop,String id){
		return filter(labels.getSpansWithProperty(prop,id));
	}

	@Override
	public boolean hasType(Span span,String type){
		return subBase.contains(span)?labels.hasType(span,type):false;
	}

	@Override
	public Iterator<Span> instanceIterator(String type){
		return filter(labels.instanceIterator(type));
	}

	@Override
	public Iterator<Span> instanceIterator(String type,String documentId){
		if(subBase.documentSpan(documentId)!=null)
			return labels.instanceIterator(type,documentId);
		else
			return Collections.<Span>emptySet().iterator();
	}

	@Override
	public Set<String> getTypes(){
		return labels.getTypes();
	}

	@Override
	public Set<Span> getTypeSet(String type,String documentId){
		return labels.getTypeSet(type,documentId);
	}

	@Override
	public boolean isType(String type){
		return labels.isType(type);
	}

	@Override
	public Iterator<Span> closureIterator(String type){
		return filter(labels.closureIterator(type));
	}

	@Override
	public Iterator<Span> closureIterator(String type,String documentId){
		if(subBase.documentSpan(documentId)!=null){
			return labels.closureIterator(type,documentId);
		}
		else{
			return Collections.<Span>emptySet().iterator();
		}
	}

	@Override
	public String showTokenProp(TextBase base,String prop){
		return labels.showTokenProp(base,prop);
	}

	@Override
	public Details getDetails(Span span,String type){
		return (subBase.contains(span))?labels.getDetails(span,type):null;
	}

	@Override
	public void require(String annotationType,String fileToLoad){
		labels.require(annotationType,fileToLoad);
	}

	@Override
	public void require(String annotationType,String fileToLoad,
			AnnotatorLoader loader){
		labels.require(annotationType,fileToLoad,loader);
	}

	/** Annotate labels with annotator named fileToLoad */
	@Override
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

	@Override
	public Viewer toGUI(){
		return new ZoomingTextLabelsViewer(this);
	}
}
