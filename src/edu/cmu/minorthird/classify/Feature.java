/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.util.AbstractLooper;
import gnu.trove.THashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.Serializable;
import java.util.*;

/** A name for a feature.  Features are hierarchical, so names are
 * structured: each name is an array of names from the heirarchy.
 *
 * @author William Cohen
*/

public class Feature implements Comparable,Serializable
{
	static public final long serialVersionUID = 1;
    public final int CURRENT_VERSION_NUMBER = 1;
    
	private String[] name;
	private int id = -1;

   /** Create a feature. */
	public Feature(String[] name) {
		this.name = name;
	}

   /** Create a feature. */
	public Feature(String[] name,int id) {
		this.name = name;
		this.id = id;
	}

	/** Create a feature. The parts of this name are delimited by periods. */
	public Feature(String name) {
		this(name.split("\\."));
	}

	/** Create a feature. The parts of this name are delimited by periods. */
	public Feature(String name,int id) {
		this(name.split("\\."),id);
	}

	/** Implements Comparable */
	public int compareTo(Object object) {
		Feature b = (Feature)object;
		int cmp = name.length - b.name.length;
		if (cmp!=0) return cmp;
		for (int i=0; i<name.length; i++) {
			cmp = name[i].compareTo(b.name[i]);
			if (cmp!=0) return cmp;
		}
		return 0;
	}

	public boolean equals(Object object) {
		return compareTo(object)==0;
	}

	public int hashCode() {
		int h = 783233;
		for (int i=0; i<name.length; i++) h = h^name[i].hashCode();
		return h;
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<name.length; i++) {
			if (buf.length()>0) buf.append('.');
			buf.append(name[i]);
		}
		return buf.toString();
	}

	public int size()	{	return name.length;	}

	public String getPart(int i) {return name[i];	}

	public String[] getName() { return name; }

  /**
   * This is used for algorithms which only support a vector of features (such as SVM).
   * The Factory is used to map each feature to a integer in an increasing by 1 sequence.
   * The Factory ensures that the same feature name gets the same number within a single run.
   *
   * @return id integer for the feature
   */
  public int numericName()
  {
    return Factory.getID(this);
  }

  static public class Looper extends AbstractLooper {
		public Looper(Iterator i) { super(i); }
		public Looper(Collection c) { super(c); }
		public Feature nextFeature() { return (Feature)next(); }
	}

  /**
   * Creates Features.
   * This ensures that only a single feature instance exists with a particular name.
   *
   * It also maintains feature->id mappings for numeric feature based learners
   */
  public static class Factory
  {
    private static THashMap featureSet = new THashMap();
    private static TObjectIntHashMap featureIds = new TObjectIntHashMap();
    private static int nextID = 1;

		public static int getMaxFeatureIndex() { return nextID-1; }			 

		/** kludge to keep factory from growing w/o bound for multiple learning problems. */
		public static void reset()
		{
			featureSet = new THashMap();
			featureIds = new TObjectIntHashMap();
			nextID = 1;			
		}

    public static boolean contains(Feature f)
    { return featureSet.contains(f); }

    public static Feature getFeature(String fullName)
    {
      Feature f = new Feature(fullName);
      return getFeature(f);
    }

    public static Feature getFeature(String[] name)
    {
      Feature f = new Feature(name);
      return getFeature(f);
    }

    private static Feature getFeature(Feature f)
    {
      Feature mapped = (Feature)featureSet.get(f);

      if (mapped != null)
        return mapped;
      else
      {
        featureSet.put(f, f);
        return f;
      }
    }

    public static int getID(Feature feature)
    {
      int id = featureIds.get(feature);
      if (id <= 0)
      {
        featureIds.put(feature,  nextID);
        id = nextID++;
      }

      return id;
    }
  }

}

