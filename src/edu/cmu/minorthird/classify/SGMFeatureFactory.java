/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

//import gnu.trove.THashMap;

import java.io.Serializable;
import java.util.*;

/**
 * For Stacked Graphical Learning. Creates Features, and maintains a mapping between Features and
 * numeric ids.  Also ensures that only a single feature instance
 * exists with a particular name. Add the ExampleID when creating new SGMExample.
 *
 */

public class SGMFeatureFactory extends FeatureFactory
{
	static private final long serialVersionUID = 1;


//	private THashMap featureMap = new THashMap();                    // maps features to canonical version
//	private int nextID = 1;

	/** Return a version of the example in which all features have been
	 * translated to canonical versions from the feature factory.
	 */
	public SGMExample compressSGM(SGMExample example)
	{
		if (example.asInstance() instanceof CompactInstance) {
			CompactInstance instance = (CompactInstance)example.asInstance();
			if (instance.getFactory()==this) return example;
		}
		Instance compressedInstance = new CompactInstance(example.asInstance());
		return new SGMExample(compressedInstance, example.getLabel(), example.getExampleID(), example.getWeight());
	}

	/**
	 * A compact but immutable implementation of an instance.
	 * @author wcohen, ksteppe
	 */

	private class CompactInstance extends AbstractInstance implements Serializable
	{
		static final long serialVersionUID=20071015;
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

		public SGMFeatureFactory getFactory() { return SGMFeatureFactory.this; }

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
