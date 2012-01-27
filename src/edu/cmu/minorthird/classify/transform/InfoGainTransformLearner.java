package edu.cmu.minorthird.classify.transform;

import java.util.Iterator;

import edu.cmu.minorthird.classify.BasicFeatureIndex;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.SampleDatasets;

/**
 * @author Edoardo M. Airoldi
 * Date: Feb 6, 2004
 */

/**
 *  A simple feature filter based on Ingormation Gain.
 *  The frequency model is resposible for deciding 'what to count'.  If set to
 *  "document" this filter counts the number of documents which contain a Feature;
 *  if set to "word" this filter counts the number of times a Feature appears in
 *  the whole dataset.
 */
public class InfoGainTransformLearner implements InstanceTransformLearner
{
//  static private Logger log = Logger.getLogger(T1InstanceTransformLearner.class);
  private String frequencyModel;


  /** Constructors */
  public InfoGainTransformLearner()
  {
    this.frequencyModel = "document"; // Default
  }
  public InfoGainTransformLearner(String model)
  {
    this.frequencyModel = model;
  }


  /** Accept an ExampleSchema - constraints on what the
   * Examples will be. */
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
    InfoGainInstanceTransform filter = new InfoGainInstanceTransform();
    BasicFeatureIndex index = new BasicFeatureIndex(dataset);

    if (frequencyModel.equals("document"))
    {
      double dCntPos = index.size("POS");
      double dCntNeg = dataset.size() -dCntPos;
      double totalEntropy = Entropy( dCntPos/(dCntPos+dCntNeg),dCntNeg/(dCntPos+dCntNeg) );

      for (Iterator<Feature> i=index.featureIterator(); i.hasNext(); )
      {
        Feature f = i.next();

        double dCntWithF[] = new double[2];    // [0] neg, [1] pos
        double dCntWithoutF[] = new double[2]; // [0] neg, [1] pos
        dCntWithF[0] = index.size(f,"NEG");
				dCntWithF[0] = index.size(f,ExampleSchema.NEG_CLASS_NAME);
        dCntWithF[1] = index.size(f) -dCntWithF[0];
        dCntWithoutF[0] = dCntNeg -dCntWithF[0];
        dCntWithoutF[1] = dCntPos -dCntWithF[1];

        double entropyWithF =
            Entropy( dCntWithF[1]/(dCntWithF[0]+dCntWithF[1]),dCntWithF[0]/(dCntWithF[0]+dCntWithF[1]) );
        double entropyWithoutF =
            Entropy( dCntWithoutF[1]/(dCntWithoutF[0]+dCntWithoutF[1]),dCntWithoutF[0]/(dCntWithoutF[0]+dCntWithoutF[1]) );

        double wf = (dCntWithF[0]+dCntWithF[1]) / dataset.size();
        double infoGain = totalEntropy -wf*entropyWithF -(1.0-wf)*entropyWithoutF;
        filter.addFeatureIG( infoGain,f );
      }
    }

    else if(frequencyModel.equals("word"))
    {
      //System.out.println("dCntPos="+dCntPos+" dCntNeg="+dCntNeg+" totEnt="+totalEntropy+"\n");
      //System.out.println("feature="+f+" dCntWithF+="+dCntWithF[1]+" dCntWithF-="+dCntWithF[0]);
      //System.out.println("dCntWithoutF+="+dCntWithoutF[1]+" dCntWithoutF-="+dCntWithoutF[0]);
      //System.out.println("entropyWithF="+entropyWithF+" entropyWithoutF="+entropyWithoutF+"\n");
      //System.out.println("feature: "+((Pair)igValues.get(j)).feature+", ig="+((Pair)igValues.get(j)).value);

      /*for (Example.Looper j=dataset.iterator(); j.hasNext(); )
      {
        Example e = j.nextExample();
        if (e.getLabel().bestClassName().equals("POS"))
        {
          dCnt[1] += 1.0;
          dCntWithF[1] += ( e.getWeight(f)>0 ) ? 1.0 : 0.0;
          dCntWithoutF[1] += ( e.getWeight(f)>0 ) ? 0.0 : 1.0;
        }
        else if (e.getLabel().bestClassName().equals("NEG"))
        {
          dCnt[0] += 1.0;
          dCntWithF[0] += ( e.getWeight(f)>0 ) ? 1.0 : 0.0;
          dCntWithoutF[0] += ( e.getWeight(f)>0 ) ? 0.0 : 1.0;
        }
        else
        {
          System.out.println( "error: unlabeled example!" );
          System.exit(1);
        }
      }*/

      /*// fill array of <counts_ex(feature), length_ex> for POS class
      double[] xPos = new double[ index.size(f,"POS") ];
      double[] omegaPos = new double[ index.size(f,"POS") ];
      int position=0;
      for (int j=0; j<index.size(f); j++) {
        Example e = index.getExample(f,j);
        if ( "POS".equals( e.getLabel().bestClassName() ) ) {
          xPos[position] = e.getWeight(f);
          omegaPos[position] = getLength(e);
          position += 1;
        }
      }
      // fill array of <counts(example,feature), length(example)> for NEG class
      double[] xNeg = new double[ index.size(f,"NEG") ];
      double[] omegaNeg = new double[ index.size(f,"NEG") ];
      position=0;
      for (int j=0; j<index.size(f); j++) {
        Example e = index.getExample(f,j);
        if ( "NEG".equals( e.getLabel().bestClassName() ) ) {
          xNeg[position] = e.getWeight(f);
          omegaNeg[position] = getLength(e);
          position += 1;
        }
      }*/
      // estimate Parameters for the two classes and update the T1-Filter

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


  // Accessory Methods

  /** compute the entropy of a binary attribute */
  public double Entropy(double P1, double P2 )
  {
    double entropy;
    if (P1==0.0 | P2==0.0)
    {
      entropy = 0.0;
    }
    else
    {
      entropy = -P1*Math.log(P1)/Math.log(2.0) -P2*Math.log(P2)/Math.log(2.0);
    }
    return entropy;
  }

  /** Get the total number of words in an Example */
  public double getLength(Example e)
  {
    double len=0.0;
    for (Iterator<Feature> i=e.featureIterator(); i.hasNext(); )
    {
      Feature f = i.next();
      len += e.getWeight(f);
    }
    return len;
  }


  // Test Info-Gain Transform

  static public void main(String[] args)
  {
    Dataset dataset = SampleDatasets.sampleData("toy",false);
    System.out.println( "old data:\n" + dataset );
    InfoGainTransformLearner learner = new InfoGainTransformLearner();
    InfoGainInstanceTransform filter = (InfoGainInstanceTransform)learner.batchTrain( dataset );
    filter.setNumberOfFeatures(100);
    dataset = filter.transform( dataset );
    System.out.println( "new data:\n" + dataset );
  }
}
