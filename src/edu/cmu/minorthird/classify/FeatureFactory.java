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

	public CompactInstance compress(Instance instance){
		if(instance instanceof CompactInstance&&((CompactInstance)instance).getFactory()==this){
			return (CompactInstance)instance;
		}
		else{
			return new CompactInstance(instance);
		}
	}

	/**
	 * Return a version of the Example in which all features have been
	 * translated to canonical versions from the feature factory.
	 */

	public Example compress(Example example){
		return new Example(compress(example.asInstance()),example.getLabel(),example.getWeight());
	}

	/**
	 * Return a version of the MultiExample in which all features have been
	 * translated to canonical versions from the feature factory.
	 */
	public MultiExample compress(MultiExample example){
		return new MultiExample(compress(example.asInstance()),example.getMultiLabel(),example.getWeight());
	}

	@Override
	public String toString(){
		StringBuilder b=new StringBuilder();
		b.append(super.toString()).append(" : [");
		for(int i=0;i<idFeatureMap.size();i++){
			b.append(i).append("=").append(idFeatureMap.get(i));
			if(i<idFeatureMap.size()-1){
				b.append(" ");
			}
		}
		b.append("]");
		return b.toString();
	}

	/**
	 * A compact but immutable implementation of an instance.
	 * @author wcohen, ksteppe
	 */

	protected class CompactInstance extends AbstractInstance implements Serializable{

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
			for(Iterator<Feature> it=instance.binaryFeatureIterator();it.hasNext();){
				set.add(getFeature(it.next()));
			}
			binaryFeatures=set.toArray(new Feature[set.size()]);

			set.clear();

			// iterate over numeric features and store in array
			for(Iterator<Feature> it=instance.numericFeatureIterator();it.hasNext();){
				set.add(getFeature(it.next()));
			}
			numericFeatures=set.toArray(new Feature[set.size()]);

			// store numeric feature weights
			weights=new double[numericFeatures.length];
			for(int i=0;i<numericFeatures.length;i++){
				weights[i]=instance.getWeight(numericFeatures[i]);
			}

		}

		// returns the factory that compressed this instance
		public FeatureFactory getFactory(){return FeatureFactory.this;}

		// using binary search to find feature weight; should it be more efficient?
		@Override
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

		@Override
		public Iterator<Feature> binaryFeatureIterator(){
			return new FeatureArrayIterator(binaryFeatures);
		}

		@Override
		public Iterator<Feature> numericFeatureIterator(){
			return new FeatureArrayIterator(numericFeatures);
		}

		@Override
		public Iterator<Feature> featureIterator(){
			return new UnionFeatureArrayIterator(binaryFeatures,numericFeatures);
		}
		
		@Override
		public int numFeatures(){
			return binaryFeatures.length+numericFeatures.length;
		}

		@Override
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
		public class FeatureArrayIterator implements Iterator<Feature>{

			private int current;
			private Feature[] features;

			public FeatureArrayIterator(Feature[] features){
				current=0;
				this.features=features;
			}

			@Override
			public boolean hasNext(){
				return current<features.length;
			}

			@Override
			public Feature next(){
				return features[current++];
			}

			@Override
			public void remove(){
				throw new Error("method CompactInstance.FeatureArrayLooper: remove not implemented.");
			}

		}

		/** sequential composite of n FeatureArrayLoopers (constructor for 2) */
		public class UnionFeatureArrayIterator extends FeatureArrayIterator{

			public UnionFeatureArrayIterator(Feature[] features,Feature[] moreFeatures){
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
