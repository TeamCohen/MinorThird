package edu.cmu.minorthird.text;

import java.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.text.gui.*;

import org.apache.log4j.*;

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

public class NestedTextLabels implements MonotonicTextLabels,Visible
{
	private static final Logger log = Logger.getLogger(NestedTextLabels.class);

	private MonotonicTextLabels outer;
	private TextLabels inner;
	private Set shadowedProperties = new HashSet();

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

  public boolean hasDictionary(String dictionary)
  {
    return inner.hasDictionary(dictionary) || outer.hasDictionary(dictionary);
  }

  public boolean isAnnotatedBy(String s) { return outer.isAnnotatedBy(s) || inner.isAnnotatedBy(s); }
		 
	public void setAnnotatedBy(String s) { outer.setAnnotatedBy(s); }

	public void setAnnotatorLoader(AnnotatorLoader newLoader) { outer.setAnnotatorLoader(newLoader); }

	public AnnotatorLoader getAnnotatorLoader() { return outer.getAnnotatorLoader(); }


	public void defineDictionary(String dictName,Set dict) {
		outer.defineDictionary(dictName, dict);
	}

	public boolean inDict(Token token, String dictionary) {
    boolean outDict = outer.hasDictionary(dictionary);
    boolean innerDict = inner.hasDictionary(dictionary);
    if (outDict)
      return outer.inDict(token, dictionary);
    else if (innerDict)
      return inner.inDict(token, dictionary);
    else
      throw new IllegalArgumentException("undefined dictionary " + dictionary);
	}

	/** Effectively, remove the property from this TextLabels. 
	 * Specifically ensure that for this property (a) calls to setProperty 
	 * do nothing but cause a warning (b) calls to getProperty return null.
	 */
	public void shadowProperty(String prop) {
		shadowedProperties.add(prop);
	}

	public void setProperty(Token token,String prop,String value) {
		if (shadowedProperties.contains(prop)) log.warn("Property "+prop+" has been shadowed");
		else outer.setProperty(token,prop,value); 
	}

	public String getProperty(Token token,String prop) {
		if (shadowedProperties.contains(prop)) return null;
		else {
			String r = outer.getProperty(token,prop);
			return r!=null ? r : inner.getProperty(token,prop);
		}
	}

	public Span.Looper getSpansWithProperty(String prop) {
		if (shadowedProperties.contains(prop)) return new BasicSpanLooper(Collections.EMPTY_SET.iterator());
		else if (!outer.getSpanProperties().contains(prop)) return inner.getSpansWithProperty(prop);
		else if (!inner.getSpanProperties().contains(prop)) return outer.getSpansWithProperty(prop);
		else return new MyUnionIterator( outer.getSpansWithProperty(prop), inner.getSpansWithProperty(prop) ); 
	}

	public Span.Looper getSpansWithProperty(String prop,String id) {
		if (shadowedProperties.contains(prop)) return new BasicSpanLooper(Collections.EMPTY_SET.iterator());
		else if (!outer.getSpanProperties().contains(prop)) return inner.getSpansWithProperty(prop,id);
		else if (!inner.getSpanProperties().contains(prop)) return outer.getSpansWithProperty(prop,id);
		else return new MyUnionIterator( outer.getSpansWithProperty(prop,id), inner.getSpansWithProperty(prop,id) ); 
	}

	public Set getTokenProperties() {
		Set set = setUnion( outer.getTokenProperties(), inner.getTokenProperties() );
		set.removeAll ( shadowedProperties );
		return set;
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

	public void require(String annotationType,String fileToLoad) { 
		BasicTextLabels.doRequire(this,annotationType,fileToLoad,outer.getAnnotatorLoader());
	}

	public void require(String annotationType,String fileToLoad,AnnotatorLoader loader) { 
		BasicTextLabels.doRequire(this,annotationType,fileToLoad,loader);
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

	public Viewer toGUI() 
	{
		return new ZoomingTextLabelsViewer(this);
	}

	public String toString() {
		return "[NestedLabels: "+outer+"; "+inner+"]";
	}
}
