package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * @author Edoardo M. Airoldi
 * Date: Feb 6, 2004
 */

public class OrderBasedInstanceTransform implements InstanceTransform
{
  static private Logger log = Logger.getLogger(InfoGainInstanceTransform.class);

  private int TOP_FEATURES;
  private ArrayList values;


  /** Constructor */
  public OrderBasedInstanceTransform()
  {
    this.TOP_FEATURES = 100;  // or max number of features if < 100
    this.values = new ArrayList();
  }


  /** Not used */
  public Instance transform(Instance instance)
  {
    System.out.println("Warning: cannot transform instance with Info-Gain!");
    return instance;
  }
  /** Not used */
  public Example transform(Example example)
  {
    System.out.println("Warning: cannot transform example with Info-Gain!");
    return example;
  }

  /** Transform a dataset according to Info-Gain criterion */
  public Dataset transform(Dataset dataset)
  {
    final Comparator VAL_COMPARATOR = new Comparator() {
      public int compare(Object o1, Object o2) {
        Pair ig1 = (Pair)o1;
        Pair ig2 = (Pair)o2;
        if (ig1.value<ig2.value) return 1;
        else if (ig1.value>ig2.value) return -1;
        else return (ig1.feature).compareTo( ig2.feature );
      }
    };

    // collect features to keep
    Collections.sort( values, VAL_COMPARATOR);
    int maxIndex = Math.min( values.size(),TOP_FEATURES );
    Set availableFeatures = new HashSet();
    for (int j=0; j<maxIndex; j++)
    {
      availableFeatures.add( ((Pair)values.get(j)).feature );
    }
    // create masked dataset
    BasicDataset maskeDataset = new BasicDataset();
    for (Example.Looper i=dataset.iterator(); i.hasNext(); )
    {
      Example e = i.nextExample();
      Instance mi = new MaskedInstance( e.asInstance(),availableFeatures );
      Example ex = new Example( mi,e.getLabel() );
      maskeDataset.add( ex );
    }
    return maskeDataset;
  }


  /** A class that we use to sort a TreeMap by values */
  private class Pair extends Object {
    double value;
    Feature feature;

    public Pair(double v, Feature f) {
      this.value = v;
      this.feature = f;
    }

    public String toString() {
      return "[ " + this.value + "," + this.feature + " ]"; //this.key + " ]";
    }
  }


  // Accessory Methods

  /** Number of features with the highest Info-Gain scores to keep in the dataset */
  public void setNumberOfFeatures(int number)
  {
    this.TOP_FEATURES = number;
  }

  /** Adds the Info-Gain score of feature f to the InstanceTransform */
  public void addFeatureVal(double infoGain, Feature f)
  {
    Pair p = new Pair( infoGain,f );
    values.add( p );
  }

}
