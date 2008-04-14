package edu.cmu.minorthird.text;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/** An empty text labeling.
 *
 * @author William Cohen
 */

public class EmptyLabels implements TextLabels
{

	public boolean isAnnotatedBy(String s) { return false; }
	public void setAnnotatedBy(String s) { ; }
	public TextBase getTextBase() { throw new UnsupportedOperationException("no text base"); }
	public boolean hasDictionary(String dictionary) { return false; }
	public boolean inDict(Token token,String dict) { return false; }
	public String getProperty(Token token,String prop) { return null; }
	public Set<String> getTokenProperties() { return Collections.EMPTY_SET; }
	public String getProperty(Span span,String prop) { return null; }
	public Set<String> getSpanProperties() { return Collections.EMPTY_SET; }
	public Iterator<Span> getSpansWithProperty(String prop) { return Collections.EMPTY_SET.iterator(); }
	public Iterator<Span> getSpansWithProperty(String prop,String id) { return Collections.EMPTY_SET.iterator(); }
	public boolean hasType(Span span,String type) { return false; }
	public Iterator<Span> instanceIterator(String type) { return Collections.EMPTY_SET.iterator(); }
	public Iterator<Span> instanceIterator(String type,String documentId) { return Collections.EMPTY_SET.iterator(); }
	public Set<String> getTypes() { return Collections.EMPTY_SET; }
	public Set<Span> getTypeSet(String type,String documentId) {return Collections.EMPTY_SET; }
	public boolean isType(String type) { return false; }
	public Iterator<Span> closureIterator(String type) { return Collections.EMPTY_SET.iterator(); }
	public Iterator<Span> closureIterator(String type, String documentId) { return Collections.EMPTY_SET.iterator(); }
	public String showTokenProp(TextBase base, String prop) { return ""; }
	public Details getDetails(Span span,String type) { return null; }
	public void require(String annotationType,String fileToLoad) { 
		throw new IllegalStateException("annotationType "+annotationType+" not present");
	}
	public void require(String annotationType,String fileToLoad,AnnotatorLoader loader) { 
		throw new IllegalStateException("annotationType "+annotationType+" can't be added");
	}
	public void annotateWith(String annotationType, String fileToLoad) {
		throw new IllegalStateException("annotation with " + fileToLoad + " can't be added");
	}

	public String toString() { return "[EmptyLabels]"; }
	public void setProperty(Span span,String prop,String value) 
	{
		System.out.println("Not used");
	}
}
