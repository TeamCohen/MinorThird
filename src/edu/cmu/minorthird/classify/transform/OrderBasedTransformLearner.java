package edu.cmu.minorthird.classify.transform;

import org.apache.log4j.Logger;
import edu.cmu.minorthird.classify.*;

import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;

/**
 * @author Edoardo M. Airoldi
 * Date: Feb 6, 2004
 */

/**
 *  A simple feature filter based on orderings.
 *  The frequency model is resposible for deciding 'what to count'.  If set to
 *  "document" this filter counts the number of documents which contain a Feature;
 *  if set to "word" this filter counts the number of times a Feature appears in
 *  the whole dataset.
 */
public class OrderBasedTransformLearner implements InstanceTransformLearner
{
  static private Logger log = Logger.getLogger(T1InstanceTransformLearner.class);
  private String frequencyModel;


  /** Constructors */
  public OrderBasedTransformLearner()
  {
    this.frequencyModel = "document"; // Default
  }
  public OrderBasedTransformLearner(String model)
  {
    this.frequencyModel = model;
  }


  /** Accept an ExampleSchema - constraints on what the
   * Examples will be. */
  public void setSchema(ExampleSchema schema)
  {
    if (!ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema))
    {
      throw new IllegalStateException("can only learn binary example data");
    }
  }

  /** Examine data, build an instance transformer */
  public InstanceTransform batchTrain(Dataset dataset)
  {
    OrderBasedInstanceTransform filter = new OrderBasedInstanceTransform();

    // figure out what features are high-frequency
    DatasetIndex index = new DatasetIndex(dataset);
    if ( frequencyModel.equals("document") )
    {
      for (Feature.Looper i = index.featureIterator(); i.hasNext(); ) {
        Feature f = i.nextFeature();
        double value = index.size(f);
        filter.addFeatureVal( value,f );
      }
    }
    else if ( frequencyModel.equals("word") )
    {
      for (Feature.Looper i = index.featureIterator(); i.hasNext(); ) {
        Feature f = i.nextFeature();
        double value=0.0;
        for (int j=0; j<index.size(f); j++) {
          value += index.getExample(f,j).getWeight(f);
        }
        filter.addFeatureVal( value,f );
      }
    }
    else
    {
      System.out.println( "warning: "+ frequencyModel +" is an unknown model for frequency!" );
      System.exit(1);
    }

    return filter;
  }



  // Test Info-Gain Transform

  static public void main(String[] args)
  {
    Dataset dataset = SampleDatasets.sampleData("toy",false);
    System.out.println( "old data:\n" + dataset );
    OrderBasedTransformLearner learner = new OrderBasedTransformLearner("word");
    OrderBasedInstanceTransform filter = (OrderBasedInstanceTransform)learner.batchTrain( dataset );
    filter.setNumberOfFeatures(2);
    dataset = filter.transform( dataset );
    System.out.println( "new data:\n" + dataset );
  }
}
