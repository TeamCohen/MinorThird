package edu.cmu.minorthird.text;



/** Maintains assertions about 'types' and 'properties' of contiguous
 * Spans of these Seq's.  Assertions can never be deleted from a
 * MonotonicTextEnv, but they can be added.
 *
 * @author William Cohen
*/

public interface MonotonicTextEnv extends TextEnv
{
	/** Add a word to the dictionary named by the string 'dict'. */
	public void addWord(String word,String dict);

	/** Assert that TextToken textToken has the given value of the given property. */
	public void setProperty(Token token,String prop,String value);

	/** Assert that Span span has the given value of the given property */
	public void setProperty(Span span,String prop,String value);

	/** Assert that a span has a given type. */
	public void addToType(Span span,String type);

	/** Assert that a span has a given type, and associate that
	 * assertion with some detailed information. */
	public void addToType(Span span,String type,Details details);

	/** Declare a new type, without asserting any spans as members. */
	public void declareType(String type);

	/** Record that this TextEnv was annotated with some type of annotation. */
	public void setAnnotatedBy(String s);
}
