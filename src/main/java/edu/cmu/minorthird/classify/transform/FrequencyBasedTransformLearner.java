/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;

import java.util.*;

/**
 * @author William Cohen
 * Date: Nov 21, 2003
 * @author Edoardo Airoldi
 * Date: Feb 05, 2004
 */

/**
 *  A simple feature filter based on their frequency of occurrence.
 *  The frequency model is resposible for deciding 'what to count'.  If set to
 *  "document" this filter counts the number of documents which contain a Feature;
 *  if set to "word" this filter counts the number of times a Feature appears in
 *  the whole dataset.
 */
public class FrequencyBasedTransformLearner implements InstanceTransformLearner
{
  private String frequencyModel;
  private int minimumFrequency = 3;

	/** Default constructor, for use in gui. */
  public FrequencyBasedTransformLearner()
	{
		this(3,"document");
	}

  /** This will "learn" an InstanceTransform that discards instances
   * which appear in minimumFrequency or fewer examples. */
  public FrequencyBasedTransformLearner(int minimumFrequency)
  {
    this.frequencyModel = "document"; // Default
    this.minimumFrequency = minimumFrequency;
  }
  public FrequencyBasedTransformLearner(int minimumFrequency, String frequencyModel)
  {
    this.frequencyModel = frequencyModel;
    this.minimumFrequency = minimumFrequency;
  }

  /** The schema's not used here... */
  @Override
	public void setSchema(ExampleSchema schema) {;}

  @Override
	public InstanceTransform batchTrain(Dataset dataset)
  {
    final Set<Feature> activeFeatureSet = new HashSet<Feature>();

    // figure out what features are high-frequency
    DatasetIndex index = new DatasetIndex(dataset);
    if ( frequencyModel.equals("document") )
    {
      for (Iterator<Feature> i = index.featureIterator(); i.hasNext(); ) {
        Feature f = i.next();
        if (index.size(f) >= minimumFrequency) {
          activeFeatureSet.add(f);
        }
      }
    }
    else if ( frequencyModel.equals("word") )
    {
      for (Iterator<Feature> i = index.featureIterator(); i.hasNext(); ) {
        Feature f = i.next();
        double totalCounts=0.0;
        for (int j=0; j<index.size(f); j++) {
          totalCounts += index.getExample(f,j).getWeight(f);
        }
        if (totalCounts >= minimumFrequency) {
          activeFeatureSet.add(f);
        }
      }
    }
    else
    {
      System.out.println( "warning: "+ frequencyModel +" is an unknown model for frequency!" );
      System.exit(1);
    }

    // build an InstanceTransform that removes low-frequency features
    return new AbstractInstanceTransform() {
      @Override
			public Instance transform(Instance instance) {
        return new MaskedInstance(instance, activeFeatureSet);
      }
      @Override
			public String toString() {
        return "[InstanceTransform: model = "+frequencyModel+", features appear >= "+minimumFrequency+" times]";
      }
    };
  }
}
