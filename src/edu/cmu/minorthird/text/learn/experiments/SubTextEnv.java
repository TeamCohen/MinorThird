package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.text.*;

import java.util.*;

/** A subset of another TextEnv.
 *
 * @author William Cohen
*/

public class SubTextEnv implements TextEnv
{
	protected SubTextBase subBase;
	protected TextEnv env;

	public SubTextEnv(SubTextBase subBase,TextEnv env) {
		this.subBase = subBase; this.env = env;
	}

	public boolean isAnnotatedBy(String s) {
		return env.isAnnotatedBy(s);
	}

	public void setAnnotatedBy(String s) {
		throw new UnsupportedOperationException("can't do SubTextBase.setAnnotatedBy()");
	}

	public TextBase getTextBase() {
		return subBase;
	}

	public boolean inDict(Token token,String dict) {
		return env.inDict(token,dict);
	}

	public String getProperty(Token token,String prop) {
		return env.getProperty(token,prop);
	}

	public Set getTokenProperties() {
		return env.getTokenProperties();
	}		

	public String getProperty(Span span,String prop) {
		return subBase.contains(span) ? env.getProperty(span,prop) : null;
	}

	public Set getSpanProperties() {
		return env.getSpanProperties();
	}

	public boolean hasType(Span span,String type) {
		return subBase.contains(span) ? env.hasType(span,type) : false;
	}

	public Span.Looper instanceIterator(String type) {
		return filter( env.instanceIterator(type) );
	}

	public Span.Looper instanceIterator(String type,String documentId) {
		if (subBase.documentSpan(documentId)!=null)
			return env.instanceIterator(type,documentId);
		else
			return new BasicSpanLooper( Collections.EMPTY_SET.iterator() );
	}

	public Set getTypes() {
		return env.getTypes();
	}

	public boolean isType(String type) {
		return env.isType(type);
	}

	public Span.Looper closureIterator(String type) {
		return filter( env.closureIterator(type) );
	}

	public Span.Looper closureIterator(String type, String documentId) {
		if (subBase.documentSpan(documentId)!=null)
			return env.closureIterator(type,documentId);
		else
			return new BasicSpanLooper( Collections.EMPTY_SET.iterator() );
	}

	public String showTokenProp(TextBase base, String prop) {
		return env.showTokenProp( base, prop);
	}

	public Details getDetails(Span span,String type) {
		return (subBase.contains(span)) ? env.getDetails(span,type): null;
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
}

