package edu.cmu.minorthird.util;

import java.util.Iterator;
import java.util.ArrayList;

public class UnionIterator implements Iterator
{
	private Iterator i,j,current;
	public UnionIterator(Iterator i,Iterator j)
	{
		this.i = i;
		this.j = j;
	}
	public boolean hasNext() 
	{ 
		return i.hasNext() || j.hasNext(); 
	}
	public Object next() {
		if (i.hasNext()) {
			current = i;
			return i.next();
		} else {
			current = j;
			return j.next();
		}
	}
	public void remove() 
	{ 
		current.remove();
	}
	public static void main(String argv[])
	{
		ArrayList list = new ArrayList();
		for (int i=0; i<argv.length; i++) list.add(argv[i]);
		for (Iterator i=new UnionIterator(list.iterator(), list.iterator()); i.hasNext(); ) {
			System.out.println(i.next());
		}
	}
}
