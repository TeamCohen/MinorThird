package edu.cmu.minorthird.text;

import java.util.Set;

/** Access assertions about 'types' and 'properties' of
 * contiguous Spans of these Seq's.  TextEnv's are immutable.
 *
 * @author William Cohen
*/

public interface TextEnv  
{
	/** See if the TextEnv contains a particular type of annotation */
	public boolean isAnnotatedBy(String s);

	/** Returns the TextBase which is annotated by this TextEnv, or null if that
	 * isn't set yet. */
	public TextBase getTextBase();

	/** Returns true if the value of the Token is in the named dictionary. */
	public boolean inDict(Token token,String dict);

	/** Get the property value associated with this TextToken.  */
	public String getProperty(Token token,String prop);

	/** Get a set of all properties.  */
	public Set getTokenProperties();

	/** Get the value of the named property which has been associated with this Span.  */
	public String getProperty(Span span,String prop);

	/** Get a set of all previously-defined properties.  */
	public Set getSpanProperties();

	/** Query if a span has a given type. */
	public boolean hasType(Span span,String type);

	/** Get all instances of a given type. */
	public Span.Looper instanceIterator(String type);

	/** Get all instances of a given type. */
	public Span.Looper instanceIterator(String type,String documentId);

	/** Return a set of all type names. */
	public Set getTypes();

	/** True if the given string names a type. */
	public boolean isType(String type);

	/** Returns the spans s for in the given type is 'closed'. If type T
	 * is close inside S, this means that one can apply the 'closed
	 * world assumption' and assume that the known set of spans of type
	 * T is complete, except for areas of the text that are not
	 * contained by any closure span S.
	 */
	public Span.Looper closureIterator(String type);

	/** Returns the spans S inside the given document in which the
	 * given type is 'closed'.
	 */
	public Span.Looper closureIterator(String type, String documentId);

	/** For debugging. Returns a dump of all strings that have tokens
	 * with the given property. */
	public String showTokenProp(TextBase base, String prop);

	/** Associate an assertion 'span S has type T' with a
	 * record of additional detailed information.  Returns null
	 * if the span doesn't have the stated type. */
	public Details getDetails(Span span,String type);
}
