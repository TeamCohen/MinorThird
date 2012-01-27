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

	@Override
	public boolean isAnnotatedBy(String s) { return false; }
	public void setAnnotatedBy(String s) { ; }
	@Override
	public TextBase getTextBase() { throw new UnsupportedOperationException("no text base"); }
	@Override
	public boolean hasDictionary(String dictionary) { return false; }
	@Override
	public boolean inDict(Token token,String dict) { return false; }
	@Override
	public String getProperty(Token token,String prop) { return null; }
	@Override
	public Set<String> getTokenProperties() { return Collections.EMPTY_SET; }
	@Override
	public String getProperty(Span span,String prop) { return null; }
	@Override
	public Set<String> getSpanProperties() { return Collections.EMPTY_SET; }
	@Override
	public Iterator<Span> getSpansWithProperty(String prop) { return Collections.EMPTY_SET.iterator(); }
	@Override
	public Iterator<Span> getSpansWithProperty(String prop,String id) { return Collections.EMPTY_SET.iterator(); }
	@Override
	public boolean hasType(Span span,String type) { return false; }
	@Override
	public Iterator<Span> instanceIterator(String type) { return Collections.EMPTY_SET.iterator(); }
	@Override
	public Iterator<Span> instanceIterator(String type,String documentId) { return Collections.EMPTY_SET.iterator(); }
	@Override
	public Set<String> getTypes() { return Collections.EMPTY_SET; }
	@Override
	public Set<Span> getTypeSet(String type,String documentId) {return Collections.EMPTY_SET; }
	@Override
	public boolean isType(String type) { return false; }
	@Override
	public Iterator<Span> closureIterator(String type) { return Collections.EMPTY_SET.iterator(); }
	@Override
	public Iterator<Span> closureIterator(String type, String documentId) { return Collections.EMPTY_SET.iterator(); }
	@Override
	public String showTokenProp(TextBase base, String prop) { return ""; }
	@Override
	public Details getDetails(Span span,String type) { return null; }
	@Override
	public void require(String annotationType,String fileToLoad) { 
		throw new IllegalStateException("annotationType "+annotationType+" not present");
	}
	@Override
	public void require(String annotationType,String fileToLoad,AnnotatorLoader loader) { 
		throw new IllegalStateException("annotationType "+annotationType+" can't be added");
	}
	@Override
	public void annotateWith(String annotationType, String fileToLoad) {
		throw new IllegalStateException("annotation with " + fileToLoad + " can't be added");
	}

	@Override
	public String toString() { return "[EmptyLabels]"; }
	public void setProperty(Span span,String prop,String value) 
	{
		System.out.println("Not used");
	}
}
