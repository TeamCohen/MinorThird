/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;

/**
 * An inverted index, mapping features to examples which contain the
 * features.
 *
 * @author Cameron Williams, Frank Lin
 */

public class MultiDatasetIndex implements Serializable{

	static final long serialVersionUID=20080131L;

	private SortedMap<Feature,List<MultiExample>> indexByFeature;

	private SortedMap<String,List<MultiExample>> indexByClass;

	private int sumFeatureValues;

	private int exampleCount;

	public MultiDatasetIndex(){
		indexByFeature=new TreeMap<Feature,List<MultiExample>>();
		indexByClass=new TreeMap<String,List<MultiExample>>();
		sumFeatureValues=0;
	}

	/** Construct an index of a dataset. */
	public MultiDatasetIndex(MultiDataset data){
		this();
		for(Iterator<MultiExample> i=data.multiIterator();i.hasNext();){
			addMultiExample(i.next());
		}
	}

	/** Add a single example to the index. */
	public void addMultiExample(MultiExample e){
		classIndex(e.getMultiLabel().bestClassName().toString()).add(e);
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
	public MultiExample getMultiExample(Feature f,int i){
		return featureIndex(f).get(i);
	}

	/** Get i-th example with given class label. */
	public MultiExample getMultiExample(String label,int i){
		return classIndex(label).get(i);
	}

	/** Get all examples with a feature in common with the given instance. */
	public Iterator<MultiExample> getNeighbors(Instance instance){
		Set<MultiExample> set=new HashSet<MultiExample>();
		for(Iterator<Feature> i=instance.featureIterator();i.hasNext();){
			Feature feature=i.next();
			for(Iterator<MultiExample> j=featureIndex(feature).iterator();j.hasNext();){
				MultiExample e=j.next();
				set.add(e);
			}
		}
		return set.iterator();
	}

	//
	// statistics about the dataset
	//

	/** Number of features indexed. */
	public int numberOfFeatures(){
		return indexByFeature.keySet().size();
	}

	/** Average number of non-zero feature values in examples. */
	public double averageFeaturesPerExample(){
		return sumFeatureValues/((double)exampleCount);
	}

	//
	// subroutines
	//

	protected List<MultiExample> featureIndex(Feature feature){
		List<MultiExample> result=indexByFeature.get(feature);
		if(result==null){
			indexByFeature.put(feature,result=new ArrayList<MultiExample>());
		}
		return result;
	}

	protected List<MultiExample> classIndex(String label){
		List<MultiExample> result=indexByClass.get(label);
		if(result==null){
			indexByClass.put(label,result=new ArrayList<MultiExample>());
		}
		return result;
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("[index");
		for(Iterator<Feature> i=featureIterator();i.hasNext();){
			Feature f=i.next();
			buf.append("\n"+f+":");
			for(int j=0;j<size(f);j++){
				buf.append("\n\t"+getMultiExample(f,j).toString());
			}
		}
		for(Iterator<String> i=indexByClass.keySet().iterator();i.hasNext();){
			String label=i.next();
			buf.append("\n"+label+":");
			for(int j=0;j<size(label);j++){
				buf.append("\n\t"+getMultiExample(label,j).toString());
			}
		}
		buf.append("\nindex]");
		return buf.toString();
	}

	//
	// main
	//

	static public void main(String[] args){
		System.out.println("MultiDatasetIndex");
	}
}
