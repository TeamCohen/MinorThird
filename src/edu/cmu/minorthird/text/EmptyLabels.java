package edu.cmu.minorthird.text;

import java.util.Set;
import java.util.TreeSet;

/** An empty text labeling.
 *
 * @author William Cohen
*/

public class EmptyLabels implements TextLabels
{
	public static final TreeSet EMPTY_SET = new TreeSet();

	public EmptyLabels() {;}

	public boolean isAnnotatedBy(String s) { return false; }
	public void setAnnotatedBy(String s) { ; }
	public TextBase getTextBase() { throw new UnsupportedOperationException("no text base"); }
  public boolean hasDictionary(String dictionary) { return false; }
  public boolean inDict(Token token,String dict) { return false; }
	public String getProperty(Token token,String prop) { return null; }
	public Set getTokenProperties() { return EMPTY_SET; }
	public String getProperty(Span span,String prop) { return null; }
	public Set getSpanProperties() { return EMPTY_SET; }
	public Span.Looper getSpansWithProperty(String prop) { return nullLooper(); }
	public Span.Looper getSpansWithProperty(String prop,String id) { return nullLooper(); }
	public boolean hasType(Span span,String type) { return false; }
	public Span.Looper instanceIterator(String type) { return nullLooper(); }
	public Span.Looper instanceIterator(String type,String documentId) { return nullLooper(); }
	public Set getTypes() { return EMPTY_SET; }
	public boolean isType(String type) { return false; }
	public Span.Looper closureIterator(String type) { return nullLooper(); }
	public Span.Looper closureIterator(String type, String documentId) { return nullLooper(); }
	public String showTokenProp(TextBase base, String prop) { return ""; }
	public Details getDetails(Span span,String type) { return null; }
	public void require(String annotationType,String fileToLoad) { 
		throw new IllegalStateException("annotationType "+annotationType+" not present");
	}
	public void require(String annotationType,String fileToLoad,AnnotatorLoader loader) { 
		throw new IllegalStateException("annotationType "+annotationType+" can't be added");
	}
	private Span.Looper nullLooper() { return new BasicSpanLooper( EMPTY_SET ); }

	public String toString() { return "[EmptyLabels]"; }
}
