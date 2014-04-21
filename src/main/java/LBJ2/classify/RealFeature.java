package LBJ2.classify;

import LBJ2.util.ByteString;


/**
  * A real feature takes on any value representable by a <code>double</code>.
  *
  * @author Nick Rizzolo
 **/
public abstract class RealFeature extends Feature
{
  /**
    * For internal use only.
    *
    * @see Feature#readFeature(LBJ2.util.ExceptionlessInputStream)
   **/
  protected RealFeature() { }

  /**
    * Sets both the identifier and the value.
    *
    * @param p  The new real feature's package.
    * @param c  The name of the classifier that produced this feature.
   **/
  public RealFeature(String p, String c) { super(p, c); }


  /**
    * Determines if this feature is discrete.
    *
    * @return <code>true</code> iff this is discrete.
   **/
  public boolean isDiscrete() { return false; }


  /**
    * Gives a string representation of the value of this feature.
    *
    * @return <code>null</code>, since real features don't have string values.
   **/
  public String getStringValue() { return null; }


  /**
    * Gives a string representation of the value of this feature.
    *
    * @return <code>null</code>, since real features don't have string values.
   **/
  public ByteString getByteStringValue() { return null; }


  /**
    * Determines whether or not the parameter is equivalent to the string
    * representation of the value of this feature.
    *
    * @param v  The string to compare against.
    * @return <code>false</code>, since real features don't have string
    *         values.
   **/
  public boolean valueEquals(String v) { return false; }


  /** Simply returns this object. */
  public RealFeature makeReal() { return this; }


  /**
    * Returns a new feature object, the same as this one in all respects
    * except the value has been multiplied by the specified number.
    *
    * @param m  The multiplier.
    * @return A new real feature whose value is the product of this feature's
    *         value and the specified multiplier.
   **/
  public abstract RealFeature multiply(double m);


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  public Feature conjunction(Feature f, Classifier c) {
    return new RealConjunctiveFeature(c, f, this);
  }
}

