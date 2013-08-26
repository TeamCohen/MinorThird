package edu.cmu.minorthird.text;

import java.util.Iterator;



/** Maintains assertions about 'types' and 'properties' of Spans.
 * Assertions can be added or deleted.
 *
 * @author William Cohen
*/

public interface MutableTextLabels extends MonotonicTextLabels
{
	/** Make it the case that there are no spans of the given type
	 * contained by the given span, other than those already
	 * inserted to exist.
  */
	public void closeTypeInside(String type,Span s);

	/** Make it the case that there are no spans whatsoever of the given
	 * type contained by the given span, other than those described by
	 * the given span looper.
	 */
	public void defineTypeInside(String type,Span s,Iterator<Span> i);

	/** Initialize the textbase which is annotated by this TextLabels.
	 * This produces an error if the current textbase has already been set. 
	 */
	public void setTextBase(TextBase textBase);
}
