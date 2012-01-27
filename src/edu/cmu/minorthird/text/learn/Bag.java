package edu.cmu.minorthird.text.learn;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** A Bag of Objects. Also known as a Multiset. 
 */

class Bag<T>{

	static private Integer ONE=new Integer(1);

	static private Integer TWO=new Integer(2);

	static private Integer THREE=new Integer(3);

	private SortedMap<T,Integer> map=new TreeMap<T,Integer>();

	public boolean contains(T o){
		return map.keySet().contains(o);
	}

	/** Add another instance of the object. */
	public void add(T o){
		Integer count=map.get(o);
		if(count==null)
			map.put(o,ONE);
		else if(count==ONE)
			map.put(o,TWO);
		else if(count==TWO)
			map.put(o,THREE);
		else
			map.put(o,new Integer(count.intValue()+1));
	}

	/** Add another instance of the object. */
	public void add(T o,int numCopies){
		Integer count=map.get(o);
		if(count==null)
			map.put(o,new Integer(numCopies));
		else
			map.put(o,new Integer(count.intValue()+numCopies));
	}

	/** Iterator over all objects. */
	public Iterator<T> iterator(){
		return map.keySet().iterator();
	}

	/** Get number of occurences of the object in the bag. */
	public int getCount(Object o){
		Integer count=map.get(o);
		if(count==null)
			return 0;
		else
			return count.intValue();
	}

	/** Convert to an ordinary set. */
	public SortedSet<T> asSet(){
		Set<T> s=map.keySet();
		if(s instanceof TreeSet)
			return (TreeSet<T>)s;
		else{
			SortedSet<T> t=new TreeSet<T>();
			t.addAll(s);
			return t;
		}
	}

	@Override
	public String toString(){
		return "[Bag "+map+"]";
	}
}
