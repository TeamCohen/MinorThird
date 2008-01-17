/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.minorthird.classify.multi.MultiExample;

/**
 * Creates Features, and maintains a mapping between Features and
 * numeric ids.  Also ensures that only a single feature instance
 * exists with a particular name.
 */

public class FeatureFactory implements Serializable{

	static private final long serialVersionUID=20080115L;

	// maps features to canonical features
	private HashMap<Feature,Feature> canonicalMap=new HashMap<Feature,Feature>();
	// maps canonical features to feature ID's
	private HashMap<Feature,Integer> featureIdMap=new HashMap<Feature,Integer>();
	// maps feature ID's to canonical features
	private List<Feature> idFeatureMap=new ArrayList<Feature>();

	public int getMaxFeatureIndex(){
		return idFeatureMap.size()-1;
	}

	public boolean contains(Feature f){
		return canonicalMap.containsKey(f);
	}

	// get the canonical version of the feature
	public Feature getFeature(Feature f){
		Feature canonical=canonicalMap.get(f);
		if(canonical==null){
			// not yet stored, so create a canonical version
			canonical=new Feature(f.getName(),idFeatureMap.size());
			canonicalMap.put(f,canonical);
			featureIdMap.put(canonical,idFeatureMap.size());
			idFeatureMap.add(canonical);
		}
		return canonical;
	}

	public Feature getFeature(String fullName){
		Feature f=new Feature(fullName);
		return getFeature(f);
	}

	public Feature getFeature(String[] name){
		Feature f=new Feature(name);
		return getFeature(f);
	}

	public Feature getFeature(int id){
		if(id<0||id>=idFeatureMap.size()){
			return null;
		}
		else{
			return idFeatureMap.get(id);
		}
	}

	public int getID(Feature feature){
		return featureIdMap.get(feature);
	}

	/**
	 * Return a version of the instance in which all features have been
	 * translated to canonical versions from the feature factory
	 */

	public Instance compress(Instance instance){
		return new CompactInstance(instance);
	}

	/**
	 * Return a version of the example in which all features have been
	 * translated to canonical versions from the feature factory.
	 */

	public Example compress(Example example){
		if(example.asInstance() instanceof CompactInstance){
			CompactInstance instance=(CompactInstance)example.asInstance();
			if(instance.getFactory()==this){
				// if the example is a CompactInstance already and is from the same factory, no need to compact it again
				return example;
			}
		}
		Instance compactInstance=new CompactInstance(example.asInstance());
		return new Example(compactInstance,example.getLabel(),example.getWeight());
	}

	/**
	 * Return a version of the example in which all features have been
	 * translated to canonical versions from the feature factory.
	 */
	public MultiExample compressMulti(MultiExample example){
		if(example.asInstance() instanceof CompactInstance){
			CompactInstance instance=(CompactInstance)example.asInstance();
			if(instance.getFactory()==this){
				// if the example is a CompactInstance already and is from the same factory, no need to compact it again
				return example;
			}
		}
		Instance compactInstance=new CompactInstance(example.asInstance());
		return new MultiExample(compactInstance,example.getMultiLabel(),example.getWeight());
	}

	public String toString(){
		StringBuilder b=new StringBuilder();
		b.append(super.toString()).append(":\n");
		for(int i=0;i<idFeatureMap.size();i++){
			b.append(i).append(" ").append(idFeatureMap.get(i)).append("\n");
		}
		return b.toString();
	}

	/**
	 * A compact but immutable implementation of an instance.
	 * @author wcohen, ksteppe
	 */

	private class CompactInstance extends AbstractInstance implements Serializable{

		static final long serialVersionUID=20071015L;

		private Feature[] binaryFeatures;

		private Feature[] numericFeatures;
		private double[] weights;

		/**
		 * Create a compact instance from some other instance
		 * @param instance Instance object to generate from
		 */
		public CompactInstance(Instance instance){

			// copy over the source and subpopulation id
			this.source=instance.getSource();
			this.subpopulationId=instance.getSubpopulationId();

			// create a sorted set for holding and sorting the features 
			SortedSet<Feature> set=new TreeSet<Feature>();

			// iterate over binary features and store in array
			for(Feature.Looper it=instance.binaryFeatureIterator();it.hasNext();){
				set.add(getFeature(it.nextFeature()));
			}
			binaryFeatures=(Feature[])set.toArray(new Feature[set.size()]);

			set.clear();

			// iterate over numeric features and store in array
			for(Feature.Looper it=instance.numericFeatureIterator();it.hasNext();){
				set.add(getFeature(it.nextFeature()));
			}
			numericFeatures=(Feature[])set.toArray(new Feature[set.size()]);

			// store numeric feature weights
			weights=new double[numericFeatures.length];
			for(int i=0;i<numericFeatures.length;i++){
				weights[i]=instance.getWeight(numericFeatures[i]);
			}

		}

		// returns the factory that compressed this instance
		public FeatureFactory getFactory(){return FeatureFactory.this;}

		// using binary search to find feature weight; should it be more efficient?
		public double getWeight(Feature f){
			// search through binary features first
			if(Arrays.binarySearch(binaryFeatures,f)>-1){
				return 1;
			}
			// then search through numeric features
			int index=Arrays.binarySearch(numericFeatures,f);
			if(index>-1){
				return weights[index];
			}
			else{
				return 0;
			}
		}

		public Feature.Looper binaryFeatureIterator(){
			return new FeatureArrayLooper(binaryFeatures);
		}

		public Feature.Looper numericFeatureIterator(){
			return new FeatureArrayLooper(numericFeatures);
		}

		public Feature.Looper featureIterator(){
			return new UnionFeatureArrayLooper(binaryFeatures,numericFeatures);
		}

		public String toString(){
			StringBuilder b=new StringBuilder();
			b.append("[compact instance/").append(subpopulationId).append(":");
			for(int i=0;i<binaryFeatures.length;i++){
				b.append(" ").append(binaryFeatures[i]);
			}
			for(int i=0;i<numericFeatures.length;i++){
				b.append(" ").append(numericFeatures[i]).append(":").append(getWeight(numericFeatures[i]));
			}
			b.append("]");
			return b.toString();
		}

		/** a looper over a feature array with Feature.Looper type */
		public class FeatureArrayLooper extends Feature.Looper{

			private int curIndex;
			private Feature[] featureArray;

			public FeatureArrayLooper(Feature[] features){
				super((Iterator<Feature>)null);
				curIndex=0;
				this.featureArray=features;
			}

			public boolean hasNext(){
				return curIndex<featureArray.length;
			}

			public Feature nextFeature(){
				return featureArray[curIndex++];
			}

			public int estimatedSize(){
				return featureArray.length;
			}

			public Feature next(){
				return nextFeature();
			}

			public void remove(){
				throw new Error("method CompactInstance.FeatureArrayLooper: remove not implemented.");
			}

		}

		/** sequential composite of n FeatureArrayLoopers (constructor for 2) */
		public class UnionFeatureArrayLooper extends FeatureArrayLooper{

			public UnionFeatureArrayLooper(Feature[] features,Feature[] moreFeatures){
				super(combine(features,moreFeatures));
			}

		}

	}

	private static Feature[] combine(Feature[] a1,Feature[] a2){
		Feature[] combined=new Feature[a1.length+a2.length];
		for(int i=0;i<a1.length;i++){
			combined[i]=a1[i];
		}
		for(int i=0;i<a2.length;i++){
			combined[a1.length+i]=a2[i];
		}
		return combined;
	}

}
