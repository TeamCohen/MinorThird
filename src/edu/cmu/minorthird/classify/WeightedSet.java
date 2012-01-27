/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;

/** Set of objects, each with an associated weight */

public class WeightedSet<T> implements Serializable{
	
  static private final long serialVersionUID=20080116L;
  
	private Map<T,Double>map;

	public WeightedSet(){
		map=new TreeMap<T,Double>();
	}

	/** Add a new object. */
	public boolean add(T object,double weight){
		boolean result=map.get(object)!=null;
		map.put(object,weight);
		return result;
	}
	
	/** Get weight for an object. */
	public double getWeight(T object,double defaultWeight){
		Double d=map.get(object);
		return(d==null?defaultWeight:d.doubleValue());
	}
	
	public double getWeight(T object){
		return getWeight(object,0.0);
	}
	
	public Set<T> asSet() {
		return map.keySet();
	}
	
	public boolean contains(T object){
		return map.get(object)!=null;
	}
	
  public Iterator<T> iterator(){
    return map.keySet().iterator();
  }
  
  public int size(){
  	return map.size();
  }

	@Override
	public String toString(){
		return map.toString();
	}
	
}
