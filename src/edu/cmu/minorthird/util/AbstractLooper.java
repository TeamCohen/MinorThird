package edu.cmu.minorthird.util;

/**
 * Basic class for a 'Looper' over Examples.
 * Finish the implementation by adding
 * a method: 
 * <p>
 * <code>public Foo nextFoo() { return(Foo)next(); }
 * </code>
 *
 * @author William Cohen
 */

import java.util.*;

public class AbstractLooper implements Iterator
{
	private Iterator i;
	private int estSize = -1;
	public AbstractLooper(Iterator i) { this.i = i; }
	public AbstractLooper(Collection c) { this.i = c.iterator(); estSize = c.size(); }
	public void remove() { i.remove(); }
	public boolean hasNext() { return i.hasNext(); }
	public Object next() { return i.next(); }
	/** Estimated number of items to be iterated over,
	 * or -1 if the number is unknown.
	 */
	public int estimatedSize() { return estSize; }
}

