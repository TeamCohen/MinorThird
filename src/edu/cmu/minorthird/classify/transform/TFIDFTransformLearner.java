/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import java.util.*;
import java.io.*;
import gnu.trove.*;


/**
 * Replaces feature counts by a TFIDF version of counts.
 * 
 * @author William Cohen
 */

public class TFIDFTransformLearner implements InstanceTransformLearner,Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

	private TObjectDoubleHashMap featureFreq; 
	private double numDocuments;

  /** The schema's not used here... */
  public void setSchema(ExampleSchema schema) {;}

  public InstanceTransform batchTrain(Dataset dataset)
  {
    // figure out frequency of each feature
		numDocuments = dataset.size();
		featureFreq = new TObjectDoubleHashMap();
		for (Example.Looper i=dataset.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			for (Feature.Looper j=e.featureIterator(); j.hasNext(); ) {
				Feature f = j.nextFeature();
				double d = featureFreq.get(f);
				featureFreq.put(f, d+1);
			}
		}
		// build an InstanceTransform that removes low-frequency features
		return new TFIDFWeighter(numDocuments,featureFreq);
	}

	private class TFIDFWeighter extends AbstractInstanceTransform implements Serializable
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;

		private double numDocuments;
		private TObjectDoubleHashMap featureFreq;
		public TFIDFWeighter(double numDocuments,TObjectDoubleHashMap featureFreq)
		{
			this.numDocuments = numDocuments;
			this.featureFreq = featureFreq;
		}
		public Instance transform(Instance instance)
		{
			double norm = 0.0;
			for (Feature.Looper i = instance.featureIterator(); i.hasNext(); ) {
				Feature g = i.nextFeature();
				double unnormalized = unnormalizedTFIDFWeight( g, instance );
				norm += unnormalized*unnormalized;
			}
			norm = Math.sqrt(norm);
			MutableInstance result = new MutableInstance(instance.getSource(), instance.getSubpopulationId());
			for (Feature.Looper i=instance.featureIterator(); i.hasNext(); ) {
				Feature f = i.nextFeature();
				double w = unnormalizedTFIDFWeight(f, instance);
				result.addNumeric( f, w/norm);
			}
			return result;
		}
		private double unnormalizedTFIDFWeight(Feature f, Instance instance)
		{
			double df = featureFreq.get(f);
			if (df==0) df = 1; // assume new words are important
			return Math.log( instance.getWeight(f) + 1) * Math.log( numDocuments/df );
		}
		public String toString() { return "[TFIDFWeighter]"; }
	}
}
