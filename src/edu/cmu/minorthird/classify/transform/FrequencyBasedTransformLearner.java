/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;

import java.util.*;

/**
 * @author William Cohen
 * Date: Nov 21, 2003
 */

public class FrequencyBasedTransformLearner implements InstanceTransformLearner
{
	private int minimumFrequency = 3;

	/** This will "learn" an InstanceTransform that discards instances
	 * which appear in minimumFrequency or fewer examples. */
	public FrequencyBasedTransformLearner(int minimumFrequency)
	{
		this.minimumFrequency = minimumFrequency;
	}

	/** The schema's not used here... */
	public void setSchema(ExampleSchema schema) {;}

	public InstanceTransform batchTrain(Dataset dataset)
	{
		final Set activeFeatureSet = new HashSet();

		// figure out what features are high-frequency
		DatasetIndex index = new DatasetIndex(dataset);
		for (Feature.Looper i = index.featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			if (index.size(f) > minimumFrequency) {
				activeFeatureSet.add(f);
			}
		}
		
		// build an InstanceTransform that removes low-frequency features
		return new AbstractInstanceTransform() {
				public Instance transform(Instance instance) {
					return new MaskedInstance(instance, activeFeatureSet);
				}
			};
	}
}
