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

public class AbstractLooper<T> implements Iterator<T>{
	
	private Iterator<T> i;
	private int estSize=-1;
	
	public AbstractLooper(Iterator<T> i){
		this.i=i;
	}
	
	public AbstractLooper(Collection<T> c){
		this.i=c.iterator();
		estSize=c.size();
	}
	
	@Override
	public void remove(){
		i.remove();
	}
	
	@Override
	public boolean hasNext(){
		return i.hasNext();
	}
	
	@Override
	public T next(){
		return i.next();
	}
	
	/** Estimated number of items to be iterated over,
	 * or -1 if the number is unknown.
	 */

	public int estimatedSize(){
		return estSize;
	}
	
}

