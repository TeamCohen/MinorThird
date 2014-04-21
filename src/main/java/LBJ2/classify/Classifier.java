package LBJ2.classify;

import java.io.IOException;
import java.io.Serializable;


/**
  * Objects of this class represent functions that make some multi-valued
  * decision about an object.
  *
  * @author Nick Rizzolo
 **/
public abstract class Classifier implements Cloneable, Serializable
{
  /**
    * Measures the performance of a classifier as compared with the values
    * produced by an oracle.
    *
    * @param subject  The classifier to test.
    * @param oracle   A classifier that produces the correct classifications.
    * @param o        The set of objects to test the subject on.
    * @return The accuracy of the subject classifier.
   **/
  public static double test(Classifier subject, Classifier oracle, Object[] o)
  {
    int correct = 0;
    for (int i = 0; i < o.length; ++i)
      if (subject.classify(o[i]).valueEquals(oracle.classify(o[i])))
        ++correct;
    return correct / (double) o.length;
  }


  /** The name of the package containing this classifier. */
  public String containingPackage;
  /**
    * The name of the classifier usually becomes the identifier of produced
    * features.
   **/
  public String name;


  /** Does nothing. */
  protected Classifier() { }

  /**
    * Initializing constructor.
    *
    * @param n  The name of the classifier, which can be fully qualified.
   **/
  protected Classifier(String n) {
    int lastDot = n.lastIndexOf('.');
    containingPackage = lastDot == -1 ? "" : n.substring(0, lastDot);
    containingPackage = containingPackage.intern();
    name = n.substring(lastDot + 1).intern();
  }


  /**
    * This method makes one or more decisions about a single object, returning
    * those decisions as {@link Feature}s in a vector.
    *
    * @param o  The object to make decisions about.
    * @return A vector of {@link Feature}s about the input object.
   **/
  public abstract FeatureVector classify(Object o);


  /**
    * Use this method to make a batch of classification decisions about
    * several objects.  This function is implemented in the most naive way
    * (simply calling {@link #classify(Object)} repeatedly) and should be
    * overridden if there is a more efficient implementation.
    *
    * @param o  The objects to make decisions about.
    * @return An array of feature vectors, one per input object.
   **/
  public FeatureVector[] classify(Object[] o) {
    FeatureVector[] result = new FeatureVector[o.length];
    for (int i = 0; i < o.length; ++i)
      result[i] = classify(o[i]);
    return result;
  }


  /**
    * Returns a string describing the input type of this classifier.  The
    * type name must be fully specified (i.e. including its package name).
    * For example, the default return value of this method is: <br>
    *
    * <pre> "java.lang.Object" </pre>
    *
    * This method should be overridden by derived classes.
    *
    * @return A string representation of the expected input type of this
    *         classifier.
   **/
  public String getInputType() { return "java.lang.Object"; }


  /**
    * Returns a string describing the output feature type of this classifier.
    * It should either contain the basic type (<code>discrete</code> or
    * <code>real</code>) and square brackets or a percent sign if the
    * classifier returns an array or is a generator respectively, or simply
    * <code>mixed%</code>.  In the case that the basic type is
    * <code>discrete</code>, the curly braces containing a list of allowable
    * values should be omitted, as this list is provided by the
    * {@link #allowableValues()} method.  The default return value of this
    * method is: <br>
    *
    * <pre> "discrete" </pre>
    *
    * This method should be overridden by derived classes.
    *
    * @return A string representation of the output feature type of this
    *         classifier.
   **/
  public String getOutputType() { return "discrete"; }


  /**
    * Returns the array of allowable values that a feature returned by this
    * classifier may take.  If the array has length 0, it means either that
    * the feature has discrete type and allowable values were not specified or
    * that the feature has real or mixed type.  The default return value of
    * this method is a 0 length array.
    *
    * <p> This method should be overridden by derived classes.
    *
    * @return The allowable values that a feature returned by this classifier
    *         may take.
   **/
  public String[] allowableValues() { return new String[0]; }


  /**
    * Locates the specified discrete feature value in the array of allowable
    * values defined for this classifier.
    *
    * @param value  The value to locate.
    * @return The index of the specified value, or -1 if it wasn't found.
   **/
  public short valueIndexOf(String value) {
    String[] allowable = allowableValues();
    short result = 0;
    while (result < allowable.length && !allowable[result].equals(value))
      ++result;
    return result == allowable.length ? -1 : result;
  }


  /**
    * Returns the classification of the given example object as a single
    * feature instead of a {@link FeatureVector}.  By default, this method is
    * implemented to simply throw an
    * <code>UnsupportedOperationException</code> since some classifiers return
    * zero or multiple features at once.
    *
    * @param o  The object to classify.
    * @return The classification of <code>o</code> as a feature.
   **/
  public Feature featureValue(Object o) {
    throw
      new UnsupportedOperationException(
        "The featureValue(Object) method has not been overridden in class '"
        + getClass().getName() + "'.");
  }


  /**
    * Returns the value of the discrete feature that would be returned by this
    * classifier.  This method should only be called when overridden by a
    * classifier returning a single discrete feature.
    *
    * @param o  The object to classify.
    * @return The value of the feature produced for the input object.
   **/
  public String discreteValue(Object o) {
    throw
      new UnsupportedOperationException(
        "The discreteValue(Object) method has not been overridden in class '"
        + getClass().getName() + "'.");
  }


  /**
    * Returns the value of the real feature that would be returned by this
    * classifier.  This method should only be called when overridden by a
    * classifier returning a single real feature.
    *
    * @param o  The object to classify.
    * @return The value of the feature produced for the input object.
   **/
  public double realValue(Object o) {
    throw
      new UnsupportedOperationException(
        "The realValue(Object) method has not been overridden in class '"
        + getClass().getName() + "'.");
  }

  /**
    * Returns the values of the discrete array of features that would be
    * returned by this classifier.  This method should only be called when
    * overridden by a classifier returning an array of discrete features.
    *
    * @param o  The object to classify.
    * @return The values of the array of features produced for the input
    *         object.
   **/
  public String[] discreteValueArray(Object o) {
    throw
     new UnsupportedOperationException(
       "The discreteValueArray(Object) method has not been overridden in "
       + "class '" + getClass().getName() + "'.");
  }


  /**
    * Returns the values of the real array of features that would be returned
    * by this classifier.  This method should only be called when overridden
    * by a classifier returning an array of real features.
    *
    * @param o  The object to classify.
    * @return The value of the array of features produced for the input
    *         object.
   **/
  public double[] realValueArray(Object o) {
    throw
     new UnsupportedOperationException(
       "The realValueArray(Object) method has not been overridden in class '"
       + getClass().getName() + "'.");
  }


  /**
    * If this classifier is a composite generator, this method will be
    * overridden such that it returns all the classifiers it calls on in a
    * list.
    *
    * @return All the classifiers that take part in this composite classifier,
    *         or <code>null</code> if this classifier is not a composite
    *         classifier.
   **/
  public java.util.LinkedList getCompositeChildren() {
    throw
     new UnsupportedOperationException(
       "The getCompositeChildren() method has not been overridden in class '"
       + getClass().getName() + "'.");
  }


  /**
    * Simply returns the name of the classifier.
    *
    * @return The name of the classifier.
   **/
  public String toString() { return name; }


  /**
    * This method returns a shallow clone.
    *
    * @return A shallow clone.
   **/
  public Object clone() {
    Object clone = null;

    try { clone = super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning " + getClass().getName() + ":");
      e.printStackTrace();
      System.exit(1);
    }

    return clone;
  }


  /**
    * Special handling during deserialization to ensure that
    * <code>Strings</code> are <code>intern()</code>ed.
    *
    * @param in The stream to deserialize from.
   **/
  private void readObject(java.io.ObjectInputStream in)
          throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    containingPackage = containingPackage.intern();
  }
}

