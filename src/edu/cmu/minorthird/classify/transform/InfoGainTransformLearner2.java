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

public class InfoGainTransformLearner2 implements InstanceTransformLearner
{
  private String frequencyModel; 
  private int numFeatures;

  /** Default constructor, limits to 100 features */
  public InfoGainTransformLearner2()
  {
		this(100,"document");
  }

  /** This will "learn" an InstanceTransform that discards with low info gain. */
  public InfoGainTransformLearner2(int numFeatures)
  {
		this(numFeatures,"document");
  }

  public InfoGainTransformLearner2(int numFeatures, String frequencyModel)
  {
    this.frequencyModel = frequencyModel;
    this.numFeatures = numFeatures;
  }

  /** The schema's not used here... */
  public void setSchema(ExampleSchema schema) {;}

  /** A class that we use to sort a TreeMap by values */
  private class IGPair implements Comparable {
    double value;
    Feature feature;
    public IGPair(double v, Feature f) {
      this.value = v;
      this.feature = f;
    }
		public int compareTo(Object o2) {
			IGPair ig2 = (IGPair)o2;
			if (value<ig2.value) return 1;
			else if (value>ig2.value) return -1;
			else return feature.compareTo( ig2.feature );
		}
    public String toString() {
      return "[ " + this.value + "," + this.feature + " ]"; //this.key + " ]";
    }
  }

  public InstanceTransform batchTrain(Dataset dataset)
  {
    // figure out what features have high gain
    BasicFeatureIndex index = new BasicFeatureIndex(dataset);
		ArrayList igValues = new ArrayList();

		if (!dataset.getSchema().equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA)) 
			throw new IllegalArgumentException("only works for binary data!");

    if ( frequencyModel.equals("document") )
    {
      double dCntPos = (double)index.size(ExampleSchema.POS_CLASS_NAME);
      double dCntNeg = (double)dataset.size() -dCntPos;
      double totalEntropy = entropy( dCntPos/(dCntPos+dCntNeg),dCntNeg/(dCntPos+dCntNeg) );

      for (Feature.Looper i=index.featureIterator(); i.hasNext(); )
      {
        Feature f = i.nextFeature();

        double dCntWithF[] = new double[2];    // [0] neg, [1] pos
        double dCntWithoutF[] = new double[2]; // [0] neg, [1] pos
        dCntWithF[0] = (double)index.size(f,"NEG");
        dCntWithF[1] = (double)index.size(f) -dCntWithF[0];
        dCntWithoutF[0] = dCntNeg -dCntWithF[0];
        dCntWithoutF[1] = dCntPos -dCntWithF[1];

        double entropyWithF =
            entropy( dCntWithF[1]/(dCntWithF[0]+dCntWithF[1]),dCntWithF[0]/(dCntWithF[0]+dCntWithF[1]) );
        double entropyWithoutF =
            entropy( dCntWithoutF[1]/(dCntWithoutF[0]+dCntWithoutF[1]),dCntWithoutF[0]/(dCntWithoutF[0]+dCntWithoutF[1]) );

        double wf = (dCntWithF[0]+dCntWithF[1]) / (double)dataset.size();
        double infoGain = totalEntropy -wf*entropyWithF -(1.0-wf)*entropyWithoutF;
				igValues.add( new IGPair(infoGain,f) );
      }
    }
    else if ( frequencyModel.equals("word") )
    {
			throw new UnsupportedOperationException("not implemented");
    }
    else
    {
      System.out.println( "warning: "+ frequencyModel +" is an unknown model for frequency!" );
      System.exit(1);
    }

		Collections.sort( igValues );
		final Set activeFeatureSet = new HashSet();
		for (int i=0; i<numFeatures; i++) {
			activeFeatureSet.add( ((IGPair)igValues.get(i)).feature );
		}

		// build an InstanceTransform that removes low-frequency features
		return new AbstractInstanceTransform() {
				public Instance transform(Instance instance) {
					return new MaskedInstance(instance, activeFeatureSet);
				}
				public String toString() {
					return "[InstanceTransform: model = "+frequencyModel+", top "+numFeatures+" by InfoGain]";
				}
			};
  }
	
  /** compute the entropy of a binary attribute */
  public double entropy(double P1, double P2 )
  {
    double entropy;
    if (P1==0.0 | P2==0.0)  {
      entropy = 0.0;
    } else  {
      entropy = -P1*Math.log(P1)/Math.log(2.0) -P2*Math.log(P2)/Math.log(2.0);
    }
    return entropy;
  }
}
