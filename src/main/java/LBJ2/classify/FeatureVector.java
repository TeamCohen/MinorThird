package LBJ2.classify;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import LBJ2.learn.Lexicon;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.FVector;


/**
  * Objects of this class are returned by classifiers that have been applied
  * to an object.
  *
  * @author Nick Rizzolo
 **/
public class FeatureVector implements Cloneable, Serializable
{
  /** Stores non-label features. */
  protected FVector features;
  /** Stores labels. */
  protected FVector labels;
  /** With this variable, the user can weight the entire vector. */
  protected double weight;
  /** Caches the result of the {@link #makeReal()} method. */
  protected FeatureVector realCache;


  /** Simply instantiates the member variables. */
  public FeatureVector() {
    features = new FVector();
    labels = new FVector();
    weight = 1;
  }

  /**
    * Creates the vector and adds the given feature to it.
    *
    * @param f  A feature to start this vector off with.
   **/
  public FeatureVector(Feature f) {
    this();
    addFeature(f);
  }

  /**
    * Creates the vector and adds the given features to it.
    *
    * @param features A feature array to start this vector off with.
   **/
  public FeatureVector(Feature[] features) {
    this();
    for (int f = 0; f < features.length; f++)
      addFeature(features[f]);
  }

  /**
    * Instantiates a feature vector from example arrays and lexicons.
    *
    * @param ex   The example array.
    * @param lex  The feature lexicon.
    * @param llex The label lexicon.
   **/
  public FeatureVector(Object[] ex, Lexicon lex, Lexicon llex) {
    this();
    int[] fs = (int[]) ex[0];
    double[] vs = (double[]) ex[1];

    for (int i = 0; i < fs.length; ++i) {
      Feature f = lex.lookupKey(fs[i]);
      Feature ff = f.withStrength(vs[i]);
      addFeature(ff == null ? f : ff);
    }

    if (ex.length > 2) {
      int[] ls = (int[]) ex[2];
      double[] lvs = (double[]) ex[3];
      for (int i = 0; i < ls.length; ++i) {
        Feature f = llex.lookupKey(ls[i]);
        if (!f.isDiscrete()) f = f.withStrength(lvs[i]);
        addLabel(f);
      }
    }
  }


  /**
    * The size of this vector is defined as the size of {@link #features} plus
    * the size of {@link #labels}.
    *
    * @return The size of this vector.
   **/
  public int size() { return features.size() + labels.size(); }
  /** Returns the size of just the {@link #features} list. */
  public int featuresSize() { return features.size(); }
  /** Returns the size of just the {@link #labels} list. */
  public int labelsSize() { return labels.size(); }


  /**
    * Returns the feature at the specified index.
    *
    * @param index  The index of the requested feature.
    * @return The feature.
   **/
  public Feature getFeature(int index) { return features.get(index); }


  /**
    * Returns the label at the specified index.
    *
    * @param index  The index of the requested label.
    * @return The label.
   **/
  public Feature getLabel(int index) { return labels.get(index); }


  /** Returns the value of {@link #weight}. */
  public double getWeight() { return weight; }


  /** Removes all elements from both {@link #features} and {@link #labels}. */
  public void clear() {
    features = new FVector();
    labels = new FVector();
    realCache = null;
  }


  /** Removes all elements from just the {@link #labels} list. */
  public void clearLabels() { labels = new FVector(); }


  /** Sorts both of the feature lists. */
  public void sort() {
    features.sort();
    labels.sort();
  }


  /**
    * Adds a feature to the vector.
    *
    * @param f  The features to be added.
   **/
  public void addFeature(Feature f) {
    features.add(f);
    realCache = null;
  }


  /**
    * Adds all the features in another vector to this vector.
    *
    * @param v  The vector whose features are to be added.
   **/
  public void addFeatures(FeatureVector v) {
    features.addAll(v.features);
    realCache = null;
  }


  /**
    * Adds a label to the vector.
    *
    * @param l  The label to be added.
   **/
  public void addLabel(Feature l) { labels.add(l); }


  /**
    * Adds all the features in another vector (but not the labels in that
    * vector) to the labels of this vector.
    *
    * @param v  The vector whose features will become this vector's labels.
   **/
  public void addLabels(FeatureVector v) { labels.addAll(v.features); }


  /**
    * Determines whether this vector has any labels.
    *
    * @return <code>true</code> iff this vector has at least one label.
   **/
  public boolean isLabeled() { return labels.size() > 0; }


  /**
    * Converts all of the features in the {@link #features} list to
    * {@link RealFeature}s with appropriate strengths.  Otherwise, the
    * returned feature vector is the same as this one.  In particular, the
    * {@link #labels} list of the returned vector is a shallow clone of this
    * vector's {@link #labels} list.
    *
    * @return A new feature vector which is the same as this one, except all
    *         features have been converted to {@link RealFeature}s.
   **/
  public FeatureVector makeReal() {
    if (realCache == null) {
      realCache = (FeatureVector) clone();
      int N = realCache.labels.size();
      for (int i = 0; i < N; ++i)
        realCache.labels.set(i, realCache.labels.get(i).makeReal());
      N = realCache.features.size();
      for (int i = 0; i < N; ++i)
        realCache.features.set(i, realCache.features.get(i).makeReal());
    }

    return realCache;
  }


  /**
    * Returns all the values of the features in this vector (not labels)
    * arranged in a <code>String</code> array.
    *
    * @return An array of <code>String</code>s with all the feature values
    *         from this vector, or <code>null</code> if there are any
    *         {@link RealFeature}s in this vector.
   **/
  public String[] discreteValueArray() {
    String[] result = new String[features.size()];
    for (int i = 0; i < result.length; ++i)
      result[i] = features.get(i).getStringValue();
    return result;
  }


  /**
    * Returns all the values of the features in this vector (not labels)
    * arranged in a <code>double</code> array.
    *
    * @return An array of <code>double</code>s with all the feature values
    *         from this vector, or <code>null</code> if there are any
    *         {@link DiscreteFeature}s in this vector.
   **/
  public double[] realValueArray() {
    double[] result = new double[features.size()];
    for (int i = 0; i < result.length; ++i)
      result[i] = features.get(i).getStrength();
    return result;
  }


  /**
    * Returns the first feature in {@link #features}.
    *
    * @return The first feature, or <code>null</code> if there aren't any.
   **/
  public Feature firstFeature() { return features.get(0); }


  /** Removes and returns the first feature in {@link #features}. * /
  public Feature removeFirstFeature() {
    realCache = null;
    return (Feature) features.removeFirst();
  }
  */


  /**
    * Returns the first feature in {@link #labels}.
    *
    * @return The first label, or <code>null</code> if there aren't any.
   **/
  public Feature firstLabel() { return labels.get(0); }


  /**
    * Returns the square of the magnitude of the feature vector.
    *
    * @return The square of the magnitude of the feature vector.
   **/
  public double L2NormSquared() {
    double sum = 0;
    int N = features.size();

    for (int i = 0; i < N; ++i) {
      double val = features.get(i).getStrength();
      sum += val * val;
     }

     return sum;
  }


  /**
    * Returns the square of the magnitude of the given vector.
    *
    * @param exampleValues  A vector.
    * @return The square of the magnitude of the given vector.
   **/
  public static double L2NormSquared(double[] exampleValues) {
    double sum = 0;
    for (int i = 0; i < exampleValues.length; i++)
      sum += exampleValues[i] * exampleValues[i];
    return sum;
  }


  /**
    * The hash code for a <code>FeatureVector</code> is simply the sum of the
    * hash codes of the features and the labels.
    *
    * @return The hash code of this vector.
   **/
  public int hashCode() {
    int result = 0;
    int N = features.size();
    for (int i = 0; i < N; ++i)
      result = 17 * result + features.get(i).hashCode();
    N = labels.size();
    for (int i = 0; i < N; ++i)
      result = 31 * result + labels.get(i).hashCode();
    return result;
  }


  /**
    * Two <code>FeatureVector</code>s are equivalent if they contain the same
    * features and labels, as defined by {@link Feature} equivalence.
    *
    * @param o  The object to compare with this <code>FeatureVector</code> for
    *           equality.
    * @return True iff <code>o</code> is a <code>FeatureVector</code>
    *         equivalent with this vector as defined above.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FeatureVector)) return false;
    FeatureVector v = (FeatureVector) o;
    return features.equals(v.features) && labels.equals(v.labels);
  }


  /**
    * Returns a sorted map where the key is the feature index and the value is
    * the feature value.  If there are multiple occurrences of the same
    * feature, then the corresponding values are summed up.
    *
    * @param features The feature indices.
    * @param values   The feature values.
    * @return The sorted map.
   **/
  public static SortedMap getSortedMap(int[] features, double[] values) {
    SortedMap map = new TreeMap();

    for (int i = 0; i < features.length; i++) {
      Integer key = Integer.valueOf(features[i]);

      Object value = map.get(key);
      if (value == null) map.put(key, Double.valueOf(values[i]));
      else
        map.put(key,
                Double.valueOf(((Double) value).doubleValue() + values[i]));
    }

    return map;
  }


  /**
    * Computes the dot product of the 2 argument vectors.
    *
    * @param firstFeatures  The first feature vector's indices.
    * @param firstValues    The first feature vector's values.
    * @param secondFeatures The second feature vector's indices.
    * @param secondValues   The second feature vector's values.
    * @return The dot product.
   **/
  public static double dot(int[] firstFeatures, double[] firstValues,
                           int[] secondFeatures, double[] secondValues) {
    Set firstFeatureValueSet =
      getSortedMap(firstFeatures, firstValues).entrySet();
    Set secondFeatureValueSet =
      getSortedMap(secondFeatures, secondValues).entrySet();

    double result = 0.0;

    try {
      Iterator firstIterator = firstFeatureValueSet.iterator();
      Iterator secondIterator = secondFeatureValueSet.iterator();

      Map.Entry firstEntry = (Map.Entry) firstIterator.next();
      Map.Entry secondEntry = (Map.Entry) secondIterator.next();

      while(true) {
        int firstEntryKey = ((Integer) firstEntry.getKey()).intValue();
        int secondEntryKey = ((Integer) secondEntry.getKey()).intValue();

        if (firstEntryKey == secondEntryKey) {
          result += ((Double) firstEntry.getValue()).doubleValue()
                    * ((Double) secondEntry.getValue()).doubleValue();

          firstEntry = (Map.Entry) firstIterator.next();
          secondEntry = (Map.Entry) secondIterator.next();

        }
        else if (firstEntryKey < secondEntryKey)
          firstEntry = (Map.Entry) firstIterator.next();
        else
          secondEntry = (Map.Entry) secondIterator.next();
      }
    }
    catch (NoSuchElementException nsee) {
      // Program reaches here when one of the iterator.next() in the above
      // try catch block leads to this exception, and so we are done
      // computing the dot product.
    }

    return result;
  }


  /**
    * Take the dot product of two feature vectors.
    *
    * @param vector The feature vector to take the dot product with.
    * @return The dot product of this feature vector and <code>vector</code>.
   **/
  public double dot(FeatureVector vector) {
    if (features.size() == 0 || vector.features.size() == 0) return 0;
    FVector v1 = (FVector) features.clone();
    FVector v2 = (FVector) vector.features.clone();

    v1.sort();
    v2.sort();

    double res = 0;
    int i = 0, j = 0;

    Feature f1 = v1.get(0);
    Feature f2 = v2.get(0);

    while (f1 != null && f2 != null) {
      if (f1.equals(f2)) {
        res += f1.getStrength() * f2.getStrength();
        f1 = v1.get(++i);
        f2 = v2.get(++j);
      }
      else if (f1.compareTo(f2) < 0) f1 = v1.get(++i);
      else f2 = v2.get(++j);
    }

    return res;
  }


  /**
    * Two <code>FeatureVector</code>s have equal value if they contain the
    * same number of {@link Feature}s and if the values of those
    * {@link Feature}s are pair-wise equivalent according to the
    * {@link Feature#valueEquals(String)} method.
    *
    * @param vector The vector with which to test equivalence.
    * @return <code>true</code> iff the two vectors are "value equivalent" as
    *         defined above.
   **/
  public boolean valueEquals(FeatureVector vector) {
    if (features.size() != vector.features.size()
        || labels.size() != vector.labels.size())
      return false;
    int N = features.size();
    for (int i = 0; i < N; ++i)
      if (!features.get(i)
           .valueEquals(vector.features.get(i).getStringValue()))
        return false;
    N = labels.size();
    for (int i = 0; i < N; ++i)
      if (!labels.get(i).valueEquals(vector.labels.get(i).getStringValue()))
        return false;
    return true;
  }


  /**
    * Creates a string representation of this <code>FeatureVector</code>.  A
    * comma separated list of labels appears first, surrounded by square
    * brackets.  Then follows a comma separated list of features.
    *
    * @param buffer The buffer in which to create the representation.
   **/
  public void write(StringBuffer buffer) { write(buffer, true); }


  /**
    * Creates a string representation of this <code>FeatureVector</code>.  A
    * comma separated list of labels appears first, surrounded by square
    * brackets.  Then follows a comma separated list of features.
    *
    * @param buffer   The buffer in which to create the representation.
    * @param packages Whether or not to print package names.
   **/
  public void write(StringBuffer buffer, boolean packages) {
    buffer.append("[");
    int N = labels.size();

    if (N > 0) {
      if (packages) labels.get(0).write(buffer);
      else labels.get(0).writeNoPackage(buffer);

      for (int i = 1; i < N; ++i) {
        buffer.append(", ");
        if (packages) labels.get(i).write(buffer);
        else labels.get(i).writeNoPackage(buffer);
      }
    }

    buffer.append("]");
    N = features.size();

    if (N > 0) {
      buffer.append(" ");
      if (packages) features.get(0).write(buffer);
      else features.get(0).writeNoPackage(buffer);

      for (int i = 1; i < N; ++i) {
        buffer.append(", ");
        if (packages) features.get(i).write(buffer);
        else features.get(i).writeNoPackage(buffer);
      }
    }
  }


  /**
    * Returns the string representation of this <code>FeatureVector</code> as
    * created by {@link #write(StringBuffer)}.
   **/
  public String toString() {
    StringBuffer result = new StringBuffer();
    write(result);
    return result.toString();
  }


  /**
    * Returns the string representation of this <code>FeatureVector</code>
    * like {@link #toString()} except without package names.
   **/
  public String toStringNoPackage() {
    StringBuffer result = new StringBuffer();
    write(result, false);
    return result.toString();
  }


  /**
    * Writes a binary representation of the feature vector.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    out.writeDouble(weight);
    features.write(out);
    labels.write(out);
  }


  /**
    * Reads the binary representation of a feature vector from the specified
    * stream, overwriting the contents of this vector.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) {
    realCache = null;
    weight = in.readDouble();
    features = new FVector();
    features.read(in);
    labels = new FVector();
    labels.read(in);
  }


  /**
    * Returns a shallow clone of this vector; the vectors are cloned, but
    * their elements aren't.
   **/
  public Object clone() {
    FeatureVector clone = new FeatureVector();
    clone.features = (FVector) features.clone();
    clone.labels = (FVector) labels.clone();
    clone.weight = weight;
    return clone;
  }
}

