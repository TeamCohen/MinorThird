package edu.cmu.minorthird.classify;

import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import java.io.*;

/**
 * A compact but immutable implementation of an instance.
 * It contains two structures:
 * <ul>
 *  <li>binary: array storage of features</li>
 *  <li>numeric: trove Object->Double hash map</li>
 * </ul>
 * @author ksteppe
 */
public class CompactInstance extends AbstractInstance implements Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

  Feature[] binaryFeatures;
  Feature[] numericFeatures;
  double[] weights;

  static private Logger log = Logger.getLogger(CompactInstance.class);

  /**
   * Create a compact instance from some other instance
   * @param instance Instance object to generate from
   */
  public CompactInstance(Instance instance)
  {
    this.source = instance.getSource();
    this.weight = instance.getWeight();
    this.subpopulationId = instance.getSubpopulationId();

    //two pieces, numeric and binary to copy over
		//don't assume features are ordered...
		TreeSet set = new TreeSet();
		for (Feature.Looper it = instance.numericFeatureIterator(); it.hasNext(); ) 
		{
			set.add( it.nextFeature() ); //also sorts
		}
    numericFeatures = (Feature[])set.toArray(new Feature[set.size()]);
		weights = new double[numericFeatures.length];
    for (int i = 0; i < numericFeatures.length; i++)
    {
      weights[i] = instance.getWeight(numericFeatures[i]);
    }

		set.clear();
		for (Feature.Looper it = instance.binaryFeatureIterator(); it.hasNext();) 
		{
			set.add( it.nextFeature() );
		}
		binaryFeatures = (Feature[])set.toArray( new Feature[set.size()] );
  }

  /**
   * returns the weight of the Feature.
   * @param f Feature
   * @return searchs binaries first - returning 1
   *         then the numerics are searched and result returned
   *         if feature is not found, returns 0 (like the mutable)
   */
  public double getWeight(Feature f)
  {
    if (Arrays.binarySearch(binaryFeatures, f) > -1)
      return 1;

    int index = Arrays.binarySearch(numericFeatures, f);
    if (index > -1)
        return weights[index];

    return 0;
  }

  public Feature.Looper binaryFeatureIterator()
  { return new FeatureArrayLooper(binaryFeatures); }

  public Feature.Looper numericFeatureIterator()
  { return new FeatureArrayLooper(numericFeatures); }

  public Feature.Looper featureIterator()
  { return new UnionFeatureArrayLooper(binaryFeatures, numericFeatures); }

  public String toString()
  {
    String out = new String("[compact instance/" + subpopulationId + ":");
    for (int i = 0; i < binaryFeatures.length; i++)
    {
      out += " ";
      out += binaryFeatures[i];
    }

    for (int i = 0; i < numericFeatures.length; i++)
    {
      out += " ";
      out += numericFeatures[i];
      out += ":";
      out += this.getWeight(numericFeatures[i]);

    }
    out += "]";
    return out;
  }

  // ------------------------------------------------------------------  //

  /** sequential composite of n FeatureArrayLoopers (constructor for 2) */
  public class UnionFeatureArrayLooper extends FeatureArrayLooper
  {
    int whichLooper = 0;
    FeatureArrayLooper[] looperArray = new FeatureArrayLooper[2];

    public UnionFeatureArrayLooper(Object[] features, Object[] moreFeatures)
    {
      super(null);
      looperArray[0] = new FeatureArrayLooper(features);
      looperArray[1] = new FeatureArrayLooper(moreFeatures);
    }

    public int estimatedSize()
    {
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
