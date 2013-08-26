/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * An inverted index, mapping features to examples which contain the
 * features.
 *
 * @author William Cohen
 */

public class DatasetIndex implements Serializable{

	static final long serialVersionUID=20080128L;

	private SortedMap<Feature,List<Example>> indexByFeature;
	private SortedMap<String,List<Example>> indexByClass;

	private int sumFeatureValues;
	private int exampleCount;

	public DatasetIndex(){
		indexByFeature=new TreeMap<Feature,List<Example>>();
		indexByClass=new TreeMap<String,List<Example>>();
		sumFeatureValues=0;
		exampleCount=0;
	}

	/** Construct an index of a dataset. */
	public DatasetIndex(Dataset data){
		this();
		for(Iterator<Example> i=data.iterator();i.hasNext();){
			addExample(i.next());
		}
	}

	/** Add a single example to the index. */
	public void addExample(Example e){
		classIndex(e.getLabel().bestClassName()).add(e);
		for(Iterator<Feature> j=e.featureIterator();j.hasNext();){
			Feature f=j.next();
			featureIndex(f).add(e);
			sumFeatureValues++;
		}
		exampleCount++;
	}

	/** Iterate over all features indexed. */
	public Iterator<Feature> featureIterator(){
		return indexByFeature.keySet().iterator();
	}

	/** Number of examples containing non-zero values for feature f. */
	public int size(Feature f){
		return featureIndex(f).size();
	}

	/** Number of examples with the given class label. */
	public int size(String label){
		return classIndex(label).size();
	}

	/** Get i-th example containing feature f. */
	public Example getExample(Feature f,int i){
		return featureIndex(f).get(i);
	}

	/** Get i-th example with given class label. */
	public Example getExample(String label,int i){
		return classIndex(label).get(i);
	}

	/** Get all examples with a feature in common with the given instance. */
	public Iterator<Example> getNeighbors(Instance instance){
		Set<Example> set=new HashSet<Example>();
		for(Iterator<Feature> i=instance.featureIterator();i.hasNext();){
			Feature f=i.next();
			for(Iterator<Example> j=featureIndex(f).iterator();j.hasNext();){
				set.add(j.next());
			}
		}
		return set.iterator();
	}

	// statistics about the dataset
	
	/** Number of features indexed. */
	public int numberOfFeatures(){
		return indexByFeature.keySet().size();
	}

	/** Average number of non-zero feature values in examples. */
	public double averageFeaturesPerExample(){
		return sumFeatureValues/((double)exampleCount);
	}

	// subroutines

	protected List<Example> featureIndex(Feature f){
		List<Example> result=indexByFeature.get(f);
		if(result==null){
			result=new ArrayList<Example>();
			indexByFeature.put(f,result);
		}
		return result;
	}

	protected List<Example> classIndex(String label){
		List<Example> result=indexByClass.get(label);
		if(result==null){
			result=new ArrayList<Example>();
			indexByClass.put(label,result);
		}
		return result;
	}

	@Override
	public String toString(){
		StringBuilder buf=new StringBuilder("[index");
		for(Iterator<Feature> i=featureIterator();i.hasNext();){
			Feature f=i.next();
			buf.append("\n"+f+":");
			for(int j=0;j<size(f);j++){
				buf.append("\n\t"+getExample(f,j).toString());
			}
		}
		for(Iterator<String> i=indexByClass.keySet().iterator();i.hasNext();){
			String label=i.next();
			buf.append("\n"+label+":");
			for(int j=0;j<size(label);j++){
				buf.append("\n\t"+getExample(label,j).toString());
			}
		}
		buf.append("\nindex]");
		return buf.toString();
	}

	// main

	static public void main(String[] args){
		System.out.println(new DatasetIndex(SampleDatasets.sampleData("toy",false)));
	}
}
