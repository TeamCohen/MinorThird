/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;

/** Set of objects, each with an associated weight */

public class WeightedSet implements Serializable
{
  static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;
	private Map map = new TreeMap();

	public WeightedSet() {;}

	/** Add a new object.  Objects with weights of zero are ignored. */
	public boolean add(Object object, double weight) {
		boolean result = map.get(object)!=null;
		map.put(object, new Double(weight));
		return result;
	}
	/** Get weight for an object. */
	public double getWeight(Object object,double defaultWeight) {
		Double d = (Double)map.get(object);
		return (d==null ? defaultWeight : d.doubleValue());
	}
	public double getWeight(Object object) {
		return getWeight(object,0.0);
	}
	public Set asSet() {
		return map.keySet();
	}
	public boolean contains(Object object) {
		return map.get(object)!=null;
	}
  public Iterator iterator() {
    return map.keySet().iterator();
  }

	public String toString() { return map.toString(); }
}
