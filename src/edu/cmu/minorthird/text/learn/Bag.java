package edu.cmu.minorthird.text.learn;

import java.util.*;

/** A Bag of Objects. Also known as a Multiset. 
 */

class Bag 
{
	static private Integer ONE = new Integer(1);
	static private Integer TWO = new Integer(2);
	static private Integer THREE = new Integer(3);

	private TreeMap map = new TreeMap();

	public boolean contains(Object o)
	{
		return map.keySet().contains(o);
	}

	/** Add another instance of the object. */ 
	public void add(Object o)
	{
		Integer count = (Integer)map.get(o);
		if (count==null) map.put(o,ONE);
		else if (count==ONE) map.put(o,TWO);
		else if (count==TWO) map.put(o,THREE);
		else map.put(o, new Integer(count.intValue()+1));
	}

	/** Add another instance of the object. */ 
	public void add(Object o, int numCopies)
	{
		Integer count = (Integer)map.get(o);
		if (count==null) map.put(o,new Integer(numCopies));
		else map.put(o, new Integer(count.intValue()+numCopies));
	}


	/** Iterator over all objects. */
	public Iterator iterator()
	{
		return map.keySet().iterator();
	}

	/** Get number of occurences of the object in the bag. */
	public int getCount(Object o)
	{
		Integer count = (Integer)map.get(o);
		if (count==null) return 0;
		else return count.intValue();
	}

	/** Convert to an ordinary set. */
	public TreeSet asSet()
	{
		Set s = map.keySet();
		if (s instanceof TreeSet) return (TreeSet)s;
		else {
			TreeSet t = new TreeSet();
			t.addAll(s);
			return t;
		}
	}

	public String toString()
	{
		return "[Bag "+map+"]";
	}
}
