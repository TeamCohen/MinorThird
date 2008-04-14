package edu.cmu.minorthird.text;

import java.util.List;
import java.util.Set;

/** Maintains assertions about 'types' and 'properties' of contiguous
 * Spans of these Seq's.  Assertions can never be deleted from a
 * MonotonicTextLabels, but they can be added.
 *
 * @author William Cohen
 */

public interface MonotonicTextLabels extends TextLabels{

	/** Associate a dictionary with this labeling. */
	public void defineDictionary(String dictName,Set<String> dictionary);

	/** Associate a dictionary from this file */
	public void defineDictionary(String dictName,List<String> fileNames,
			boolean ignoreCase);

	/** Return a trie if defined */
	public Trie getTrie();

	/** Define a trie */
	public void defineTrie(List<String> phraseList);

	/** Assert that TextToken textToken has the given value of the given property. */
	public void setProperty(Token token,String prop,String value);

	/** Assert that a token has a given property value, and associate that
	 * with some detailed information.
	 * If details==null, this should have the same effect as setProperty(span,prop,value).
	 */
	public void setProperty(Token token,String prop,String value,Details details);

	/** Assert that Span span has the given value of the given property */
	public void setProperty(Span span,String prop,String value);

	/** Assert that Span span has the given value of the given property, 
	 * and associate that with some detailed information
	 */
	public void setProperty(Span span,String prop,String value,Details details);

	/** Assert that a span has a given type. */
	public void addToType(Span span,String type);

	/** Assert that a span has a given type, and associate that
	 * assertion with some detailed information. 
	 * If details==null, this should have the same effect as addToType(span,type).
	 */
	public void addToType(Span span,String type,Details details);

	/** Declare a new type, without asserting any spans as members. */
	public void declareType(String type);

	/** Record that this TextLabels was annotated with some type of annotation. */
	public void setAnnotatedBy(String s);

	/** Specify the AnnotatorLoader used to find Annotations when a 'require'
	 * call is made. */
	public void setAnnotatorLoader(AnnotatorLoader loader);

	/** Get the current AnnotatorLoader. */
	public AnnotatorLoader getAnnotatorLoader();
}
