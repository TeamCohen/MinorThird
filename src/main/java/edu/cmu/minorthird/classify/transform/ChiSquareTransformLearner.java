package edu.cmu.minorthird.classify.transform;

import java.util.Iterator;

import edu.cmu.minorthird.classify.BasicFeatureIndex;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.SampleDatasets;

/**
 * @author Vitor R. Carvalho
 * Date: March 2005
 *
 *  A simple feature filter based on the Chi-Squared statistic.
 *  The frequency model is resposible for deciding 'what to count'.  If set to
 *  "document" this filter counts the number of documents which contain a Feature;
 *  if set to "word" this filter counts the number of times a Feature appears in
 *  the whole dataset.
 */
public class ChiSquareTransformLearner implements InstanceTransformLearner
{
  private String frequencyModel;

  public ChiSquareTransformLearner()
  {
      this.frequencyModel = "document";
  }
  public ChiSquareTransformLearner(String model)
  {
    this.frequencyModel = model;
  }

  /** only accepts binary schemas */
  @Override
	public void setSchema(ExampleSchema schema)
  {
    if (!ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema))
    {
      throw new IllegalStateException("can only learn binary example data");
    }
  }

  /** Examine data, build an instance transformer */
  @Override
	public InstanceTransform batchTrain(Dataset dataset)
  {
    ChiSquareInstanceTransform filter = new ChiSquareInstanceTransform();
    BasicFeatureIndex index = new BasicFeatureIndex(dataset);

    if (frequencyModel.equals("document"))
    {
      int totalPos = index.size(ExampleSchema.POS_CLASS_NAME);
      int totalNeg = index.size(ExampleSchema.NEG_CLASS_NAME);
      if((totalPos+totalNeg)!=(dataset.size())){
          throw new IllegalStateException("ERROR - Dataset size and index size do not match");
      }
      
      for (Iterator<Feature> i=index.featureIterator(); i.hasNext(); )
      {
        Feature f = i.next();
        int a = index.size(f,ExampleSchema.POS_CLASS_NAME);
        int b = index.size(f,ExampleSchema.NEG_CLASS_NAME);
        int c = totalPos - a;
        int d = totalNeg - b;
        
        ContingencyTable ct = new ContingencyTable(a,b,c,d);
        double chiScore = ct.getChiSquared();
//        double chiScore = ct.getPMutualInfo();
//        double chiScore = ct.getCompensatedPMutualInfo(count(a));
        filter.addFeature( chiScore,f );
      }
    }

    else if(frequencyModel.equals("word"))//not implemented yet
    {
      System.out.println( "warning: "+ frequencyModel +" not implemented yet!" );
      System.exit(1);
    }
    else
    {
      System.out.println( "warning: "+ frequencyModel +" is an unknown model for frequency!" );
      System.exit(1);
    }

    return filter;
  }

  static public void main(String[] args)
  {
    Dataset dataset = SampleDatasets.sampleData("toy",false);
    System.out.println( "old data:\n" + dataset );
    ChiSquareTransformLearner learner = new ChiSquareTransformLearner();
    ChiSquareInstanceTransform filter = (ChiSquareInstanceTransform)learner.batchTrain( dataset );
    filter.setNumberOfFeatures(10);
    dataset = filter.transform( dataset );
    System.out.println( "new data:\n" + dataset );
    System.out.println( "\n\n\n " + filter.toString(8));
    
  }
}
