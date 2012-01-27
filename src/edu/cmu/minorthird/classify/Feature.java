/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;

/**
 * A name for a feature.  Features are hierarchical, so names are
 * structured: each name is an array of names from the heirarchy.
 *
 * @author William Cohen
 */

public class Feature implements Comparable<Feature>,Serializable{

	static public final long serialVersionUID=20080115L;

	private String[] name;
	private int id;

	/** Create a feature with its feature ID. */
	public Feature(String[] name,int id){
		this.name=name;
		this.id=id;
	}

	/** Create a feature. */
	public Feature(String[] name){
		this(name,-1);
	}

	/** Create a feature. The parts of this name are delimited by periods. */
	public Feature(String name,int id){
		this(name.trim().split("\\."),id);
	}

	/** Create a feature. The parts of this name are delimited by periods. */
	public Feature(String name){
		this(name,-1);
	}

	/** Implements Comparable */
	@Override
	public int compareTo(Feature other){
		int cmp=name.length-other.name.length;
		if(cmp!=0){
			return cmp;
		}
		else{
			for(int i=0;i<name.length;i++){
				cmp=name[i].compareTo(other.name[i]);
				if(cmp!=0){
					return cmp;
				}
			}
			return 0;
		}
	}

	/** Overrides equals */
	@Override
	public boolean equals(Object object){
		if(object instanceof Feature){
			return compareTo((Feature)object)==0;
		}
		else{
			return false;
		}
	}

	@Override
	public int hashCode(){
		int h=783233;
		for(int i=0;i<name.length;i++){
			h=h^name[i].hashCode();
		}
		return h;
	}

	@Override
	public String toString(){
		if(name.length==1){
			return name[0];
		}
		else{
			StringBuilder b=new StringBuilder();
			for(int i=0;i<name.length;i++){
				if(b.length()>0){
					b.append('.');
				}
				b.append(name[i]);
			}
			return b.toString();
		}
	}

	public int size(){
		return name.length;
	}

	public String getPart(int i){
		return name[i];
	}

	public String[] getName(){
		return name;
	}

	public int getID(){
		return id;
	}

	/**
	 * This is used for algorithms which only support a vector of features (such as SVM).
	 * The Factory is used to map each feature to a integer in an increasing by 1 sequence.
	 * The Factory ensures that the same feature name gets the same number within a single run.
	 *
	 * @return id integer for the feature
	 */
	public int numericName(){
		return id;
	}

//	static public class Looper extends AbstractLooper<Feature>{
//		public Looper(Iterator<Feature> i){super(i);}
//		public Looper(Collection<Feature> c){super(c);}
//		public Feature nextFeature(){return next();}
//	}

//	/**
//	* Creates Features.
//	* This ensures that only a single feature instance exists with a particular name.
//	*
//	* It also maintains feature->id mappings for numeric feature based learners
//	*
//	* @deprecated Use {@link edu.cmu.minorthird.classify.FeatureFactory} instead
//	*/
//	public static class Factory
//	{
//	private static THashMap featureSet = new THashMap();
//	private static TObjectIntHashMap featureIds = new TObjectIntHashMap();
//	private static int nextID = 1;

//	public static int getMaxFeatureIndex() { return nextID-1; }			 

//	/** kludge to keep factory from growing w/o bound for multiple learning problems. */
//	public static void reset()
//	{
//	featureSet = new THashMap();
//	featureIds = new TObjectIntHashMap();
//	nextID = 1;
//	}

//	public static boolean contains(Feature f)
//	{ return featureSet.contains(f); }

//	public static Feature getFeature(String fullName)
//	{
//	Feature f = new Feature(fullName);
//	return getFeature(f);
//	}

//	public static Feature getFeature(String[] name)
//	{
//	Feature f = new Feature(name);
//	return getFeature(f);
//	}

//	private static Feature getFeature(Feature f)
//	{
//	Feature mapped = (Feature)featureSet.get(f);

//	if (mapped != null)
//	return mapped;
//	else
//	{
//	featureSet.put(f, f);
//	return f;
//	}
//	}

//	/**
//	* @deprecated Use {@link edu.cmu.minorthird.classify.FeatureIdFactory} instead.
//	*/
//	public static int getID(Feature feature)
//	{
//	int id = featureIds.get(feature);
//	if (id <= 0)
//	{
//	featureIds.put(feature,  nextID);
//	id = nextID++;
//	}

//	return id;
//	}
//	}

}

