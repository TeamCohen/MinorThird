/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.util.AbstractLooper;
import gnu.trove.THashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.Serializable;
import java.util.*;

/**
 * Creates Features, and maintains a mapping between Features and
 * numeric ids.  Also ensures that only a single feature instance
 * exists with a particular name.
 *
 */

public class FeatureFactory implements Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;


	private THashMap featureMap = new THashMap();                    // maps features to canonical version
	private int nextID = 1;

	public int getMaxFeatureIndex() { return nextID-1; }			 

	public boolean contains(Feature f)	{ return featureMap.contains(f); }
	
	public Feature getFeature(String fullName)
	{
		Feature f = new Feature(fullName);
		return getFeature(f);
	}
	
	public Feature getFeature(String[] name)
	{
		Feature f = new Feature(name,-1);
		return getFeature(f);
	}
	
	// get the canonical version of the feature
	public Feature getFeature(Feature f)
	{
		Feature canonical = (Feature)featureMap.get(f);
		if (canonical==null) {
			// not yet stored, so create a canonical version
			canonical = new Feature( f.getName(), nextID++ );
			featureMap.put(f,canonical);
		}
		return canonical;
	}
	
	public int getID(Feature feature)
	{
		return getFeature(feature).numericName();
	}

	/** Return a version of the instance in which all features have been
	 * translated to canonical versions from the feature factory
	 */
	public Instance compress(Instance instance)
	{
		return new CompactInstance(instance);
	}

	/** Return a version of the example in which all features have been
	 * translated to canonical versions from the feature factory.
	 */
	public Example compress(Example example)
	{
    if (example.asInstance() instanceof CompactInstance) {
      CompactInstance instance = (CompactInstance)example.asInstance();
      if (instance.getFactory()==this) return example;
    }
    Instance compressedInstance = new CompactInstance(example.asInstance());
    return new Example(compressedInstance, example.getLabel(), example.getWeight());
	}

  /**
	 * A compact but immutable implementation of an instance.
	 * @author wcohen, ksteppe
	 */

	private class CompactInstance extends AbstractInstance implements Serializable
	{
		private Feature[] binaryFeatures;
		private Feature[] numericFeatures;
		private double[] weights;

		/**
		 * Create a compact instance from some other instance
		 * @param instance Instance object to generate from
		 */
		public CompactInstance(Instance instance)
		{
			this.source = instance.getSource();
			this.subpopulationId = instance.getSubpopulationId();

			//two pieces, numeric and binary to copy over
			//don't assume features are ordered...

			TreeSet set = new TreeSet();
			for (Feature.Looper it = instance.numericFeatureIterator(); it.hasNext(); ) {
				set.add( getFeature( it.nextFeature() ) ); //also sorts
			}
			numericFeatures = (Feature[])set.toArray(new Feature[set.size()]);
			weights = new double[numericFeatures.length];
			for (int i = 0; i < numericFeatures.length; i++) {
				weights[i] = instance.getWeight(numericFeatures[i]);
			}
			set.clear();
			for (Feature.Looper it = instance.binaryFeatureIterator(); it.hasNext();) {
				set.add( getFeature( it.nextFeature() ) );
			}
			binaryFeatures = (Feature[])set.toArray( new Feature[set.size()] );
		}

    public FeatureFactory getFactory() { return FeatureFactory.this; }

		public double getWeight(Feature f)
		{
			if (Arrays.binarySearch(binaryFeatures, f) > -1) return 1;
			int index = Arrays.binarySearch(numericFeatures, f);
			if (index > -1) return weights[index];
			return 0;
		}

		public Feature.Looper binaryFeatureIterator()	{ return new FeatureArrayLooper(binaryFeatures); }
		
		public Feature.Looper numericFeatureIterator()	{ return new FeatureArrayLooper(numericFeatures); }

		public Feature.Looper featureIterator()	{ return new UnionFeatureArrayLooper(binaryFeatures, numericFeatures); }
		
		public String toString() {
			String out = new String("[compact instance/" + subpopulationId + ":");
			for (int i = 0; i < binaryFeatures.length; i++)	{
				out += " "+binaryFeatures[i];
			}
			for (int i = 0; i < numericFeatures.length; i++){
				out += " "+ numericFeatures[i]+":"+getWeight(numericFeatures[i]);
			}
			out += "]";
			return out;
		}

		/** sequential composite of n FeatureArrayLoopers (constructor for 2) */
		public class UnionFeatureArrayLooper extends FeatureArrayLooper	{
			int whichLooper = 0;
			FeatureArrayLooper[] looperArray = new FeatureArrayLooper[2];
			public UnionFeatureArrayLooper(Object[] features, Object[] moreFeatures) {
				super(null);
				looperArray[0] = new FeatureArrayLooper(features);
				looperArray[1] = new FeatureArrayLooper(moreFeatures);
			}
			
			public int estimatedSize() {
				int size = 0;
				for (int i = 0; i < looperArray.length; i++)
				{ size += looperArray[i].estimatedSize(); }
				return size;
			}
			
			public boolean hasNext()
			{
				while (whichLooper < looperArray.length)
				{
					if (looperArray[whichLooper].hasNext())
						return true;
					else
						whichLooper++;
				}
				return false;
			}
			
			public Feature nextFeature()
			{
				if (this.hasNext())
					return looperArray[whichLooper].nextFeature();
				else
					return null;
			}
			
			public Object next()
			{ return nextFeature(); }
			
			public void remove()
			{ throw new Error ("method CompactInstance.UnionFeatureArrayLooper:remove not implemented"); }
			
  }

		/** a looper over a feature array with Feature.Looper type */
		public class FeatureArrayLooper extends Feature.Looper
		{
			int curIndex;
			Object[] featureArray;
			
			public FeatureArrayLooper(Object[] features)
			{
				super((Iterator)null);
				curIndex = 0;
				this.featureArray = features;
			}
			
			public boolean hasNext()
			{ return curIndex < featureArray.length; }
			
			public Feature nextFeature()
			{ return (Feature)featureArray[curIndex++]; }
			
			public int estimatedSize()
			{ return featureArray.length; }
			
			public Object next()
			{ return nextFeature(); }
			
			public void remove()
			{ throw new Error ("method CompactInstance.FeatureArrayLooper:remove not implemented"); }
		}
	}
}
