package edu.cmu.minorthird.text;

import java.util.HashSet;
import java.util.Set;

/** A TextLabels which is defined by two TextLabels's.
 *
 * <p> Operationally, new assertions are passed to the 'outer' TextLabels.
 * Assertions about property definitions from the outer TextLabels shadow
 * assertions made in the inner TextLabels, and other assertions are added
 * to assertions in the inner TextLabels.
 *
 * <p> Pragmatically, this means that if you create a NestedTextLabels
 * from outerLabels and innerLabels, where outerLabels is empty, the
 * NestedTextLabels will initially look like innerLabels.  But if you modify
 * it, innerLabels will not be changed, so you can at any point easily
 * revert to the old innerLabels TextLabels.
 *
 *
 * @author William Cohen
*/

public class NestedTextLabels implements MonotonicTextLabels
{
	private static final Set EMPTY_SET = new HashSet();

	private MonotonicTextLabels outer;
	private TextLabels inner;
//	private Set shadowedDocumentTypePair = new HashSet();

	/** Create a NestedTextLabels. */
	public NestedTextLabels(MonotonicTextLabels outer,TextLabels inner) {
		if (outer.getTextBase()!=inner.getTextBase()) 
			throw new IllegalArgumentException("mismatched text bases?");
		this.outer = outer; 
		this.inner = inner; 
	}

	/** Create a NestedTextLabels with an empty outer labeling. */
	public NestedTextLabels(TextLabels inner) {
		this.outer = new BasicTextLabels(inner.getTextBase());
		this.inner = inner; 
	}

	public TextBase getTextBase() {
		return inner.getTextBase();
	}

	public boolean isAnnotatedBy(String s) { return outer.isAnnotatedBy(s) || inner.isAnnotatedBy(s); }
		 
	public void setAnnotatedBy(String s) { outer.setAnnotatedBy(s); }

	public void defineDictionary(String dictName,Set dict) {
		outer.defineDictionary(dictName, dict);
	}

	public boolean inDict(Token token,String dict) {
		return outer.inDict(token,dict) || inner.inDict(token,dict); 
	}

	public void setProperty(Token token,String prop,String value) {
		outer.setProperty(token,prop,value); 
	}

	public String getProperty(Token token,String prop) {
		String r = outer.getProperty(token,prop);
		return r!=null ? r : inner.getProperty(token,prop);
	}

	public Set getTokenProperties() {
		return setUnion( outer.getTokenProperties(), inner.getTokenProperties() );
	}

	public void setProperty(Span span,String prop,String value){
		outer.setProperty(span,prop,value);
	}

	public String getProperty(Span span,String prop) {
		String r = outer.getProperty(span,prop);
		return r!=null ? r : inner.getProperty(span,prop);
	}

	public Set getSpanProperties() {
		return setUnion( outer.getSpanProperties(), inner.getSpanProperties() );
	}

	public void addToType(Span span,String type) {
		if (!inner.hasType(span,type)) outer.addToType(span,type);
	}

	public void addToType(Span span,String type,Details details) {
		if (!inner.hasType(span,type)) outer.addToType(span,type,details);
	}

	public boolean hasType(Span span,String type)	{
		return outer.hasType(span,type) || inner.hasType(span,type);
	}

	public Span.Looper instanceIterator(String type) {
		if (!outer.isType(type)) return inner.instanceIterator(type);
		else if (!inner.isType(type)) return outer.instanceIterator(type);
		else return new MyUnionIterator( outer.instanceIterator(type), inner.instanceIterator(type) );
	}

	public Span.Looper instanceIterator(String type,String documentId) {
		if (!outer.isType(type)) return inner.instanceIterator(type,documentId);
		else if (!inner.isType(type)) return outer.instanceIterator(type,documentId);
		else return 
					 new MyUnionIterator(
						 outer.instanceIterator(type,documentId),
						 inner.instanceIterator(type,documentId) );
	}

	public Span.Looper closureIterator(String type) {
		if (!outer.isType(type)) return inner.closureIterator(type);
		else if (!inner.isType(type)) return outer.closureIterator(type);
		else return new MyUnionIterator( outer.closureIterator(type), inner.closureIterator(type) );
	}

	public Span.Looper closureIterator(String type,String documentId) {
		if (!outer.isType(type)) return inner.closureIterator(type,documentId);
		else if (!inner.isType(type)) return outer.closureIterator(type,documentId);
		else return 
					 new MyUnionIterator(
						 outer.closureIterator(type,documentId),
						 inner.closureIterator(type,documentId) );
	}


	public Set getTypes() {
		return setUnion( outer.getTypes(), inner.getTypes() );
	}

	public boolean isType(String type) {
		return outer.isType(type) || inner.isType(type);
	}

	public void declareType(String type){
		if (!isType(type)) outer.declareType(type);
	}

	public Details getDetails(Span span,String type) {
		Details result = outer.getDetails(span,type);
		if (result!=null) return result;
		return inner.getDetails(span,type);
	}

	public String showTokenProp(TextBase base, String prop) {
		return "outer: "+outer.showTokenProp(base,prop)+" inner: "+inner.showTokenProp(base,prop);
	}

	//
	// private routines and classes
	//

	private Set setUnion(Set a,Set b) {
		if (a.isEmpty()) return b;
		else {
			Set u = new HashSet();
			u.addAll( a );
			u.addAll( b );
			return u;
		}
	}

	private class MyUnionIterator implements Span.Looper {
		Span.Looper i,j, currentLooper;
		int estSize = -1;
		public MyUnionIterator( Span.Looper i, Span.Looper j) {
			this.i = i; this.j = j;
			currentLooper = i;
			if (i.estimatedSize()>=0 && j.estimatedSize()>=0) 
				estSize = i.estimatedSize() + j.estimatedSize();
		}
		public void remove() { currentLooper.remove(); }
		public boolean hasNext() { 
			return currentLooper.hasNext() || ( currentLooper==i && j.hasNext() );
		}
		public Object next() {
			if (currentLooper==i && !currentLooper.hasNext()) currentLooper = j;
			return currentLooper.next(); 
		}
		public Span nextSpan() { return (Span)next(); }
		public int estimatedSize() { return estSize; }
	}

	public String toString() {
		return "[NestedLabels: "+outer+"; "+inner+"]";
	}
}
