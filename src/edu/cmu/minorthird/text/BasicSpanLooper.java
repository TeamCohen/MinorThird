package edu.cmu.minorthird.text;

import java.util.*;

/** An iterator over Span objects. 
 *
 * @author William Cohen
*/

public class BasicSpanLooper implements Span.Looper
{
	private static final Set EMPTY_SET = new TreeSet();
	private Iterator i;
	private int estSize = -1;
	public BasicSpanLooper(Iterator i) { this.i = i!=null ? i : EMPTY_SET.iterator(); }
	public BasicSpanLooper(Collection c) { this.i = c.iterator(); estSize = c.size(); }
	public void remove() { i.remove(); }
	public boolean hasNext() { return i.hasNext(); }
	public Object next() { return i.next(); }
	public Span nextSpan() { return (Span)next(); }
	public int estimatedSize() { return estSize; }
}
