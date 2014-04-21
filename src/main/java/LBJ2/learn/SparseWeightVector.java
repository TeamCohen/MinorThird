package LBJ2.learn;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import LBJ2.classify.Feature;
import LBJ2.util.ClassUtils;
import LBJ2.util.DVector;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * This class is used as a weight vector in sparse learning algorithms.
  * {@link Feature}s are associated with <code>Double</code>s and/or with
  * <code>double[]</code>s representing the weights of the features they
  * produce.  Features not appearing in the vector are assumed to have the
  * {@link #defaultWeight}.
  *
  * @author Nick Rizzolo
 **/
public class SparseWeightVector implements Cloneable, Serializable
{
  /**
    * When a feature appears in an example but not in this vector, it is
    * assumed to have this weight.
   **/
  protected static final double defaultWeight = 0;
  /** The initial capacity for {@link #weights} if not specified otherwise. */
  protected static final int defaultCapacity = 1 << 10;

  /** The weights in the vector indexed by their {@link Lexicon} key. */
  protected DVector weights;


  /** Simply instantiates {@link #weights}. */
  public SparseWeightVector() { this(new DVector(defaultCapacity)); }

  /**
    * Simply initializes {@link #weights}.
    *
    * @param w  An array of weights.
   **/
  public SparseWeightVector(double[] w) { this(new DVector(w)); }

  /**
    * Simply initializes {@link #weights}.
    *
    * @param w  A vector of weights.
   **/
  public SparseWeightVector(DVector w) { weights = w; }


  /**
    * Returns the weight of the given feature.
    *
    * @param featureIndex The feature index.
    * @return The weight of the feature.
   **/
  public double getWeight(int featureIndex) {
    return getWeight(featureIndex, defaultWeight);
  }

  /**
    * Returns the weight of the given feature.
    *
    * @param featureIndex The feature index.
    * @param defaultW     The default weight.
    * @return The weight of the feature.
   **/
  public double getWeight(int featureIndex, double defaultW) {
    return weights.get(featureIndex, defaultW);
  }


  /**
    * Sets the weight of the given feature.
    *
    * @param featureIndex The feature index.
    * @param w            The new weight.
   **/
  protected void setWeight(int featureIndex, double w) {
    setWeight(featureIndex, w, defaultWeight);
  }

  /**
    * Sets the weight of the given feature.
    *
    * @param featureIndex The feature index.
    * @param w            The new weight.
    * @param defaultW     The default weight.
   **/
  protected void setWeight(int featureIndex, double w, double defaultW) {
    weights.set(featureIndex, w, defaultW);
  }


  /**
    * Takes the dot product of this <code>SparseWeightVector</code> with the
    * argument vector, using the hard coded default weight.
    *
    * @param exampleFeatures  The example's feature indices.
    * @param exampleValues    The example's feature values.
    * @return The computed dot product.
   **/
  public double dot(int[] exampleFeatures, double[] exampleValues) {
    return dot(exampleFeatures, exampleValues, defaultWeight);
  }

  /**
    * Takes the dot product of this <code>SparseWeightVector</code> with the
    * argument vector, using the specified default weight when one is not yet
    * present in this vector.
    *
    * @param exampleFeatures  The example's feature indices.
    * @param exampleValues    The example's feature values.
    * @param defaultW         The default weight.
    * @return The computed dot product.
   **/
  public double dot(int[] exampleFeatures, double[] exampleValues,
                    double defaultW) {
    double sum = 0;

    for (int i = 0; i < exampleFeatures.length; i++) {
      double w = getWeight(exampleFeatures[i], defaultW);
      sum += w * exampleValues[i];
    }

    return sum;
  }


  /**
    * Self-modifying vector addition.
    *
    * @param exampleFeatures  The example's feature indices.
    * @param exampleValues    The example's feature values.
   **/
  public void scaledAdd(int[] exampleFeatures, double[] exampleValues) {
    scaledAdd(exampleFeatures, exampleValues, 1, defaultWeight);
  }

  /**
    * Self-modifying vector addition where the argument vector is first scaled
    * by the given factor.  The default weight is used to initialize new
    * feature weights.
    *
    * @param exampleFeatures  The example's feature indices.
    * @param exampleValues    The example's feature values.
    * @param factor           The scaling factor.
   **/
  public void scaledAdd(int[] exampleFeatures, double[] exampleValues,
                        double factor) {
    scaledAdd(exampleFeatures, exampleValues, factor, defaultWeight);
  }

  /**
    * Self-modifying vector addition where the argument vector is first scaled
    * by the given factor.
    *
    * @param exampleFeatures  The example's feature indices.
    * @param exampleValues    The example's feature values.
    * @param factor           The scaling factor.
    * @param defaultW         An initial weight for previously unseen
    *                         features.
   **/
  public void scaledAdd(int[] exampleFeatures, double[] exampleValues,
                        double factor, double defaultW) {
    for (int i = 0; i < exampleFeatures.length; i++) {
      int featureIndex = exampleFeatures[i];
      double w = getWeight(featureIndex, defaultW) + factor*exampleValues[i];
      setWeight(featureIndex, w, defaultW);
    }
  }


  /**
    * Self-modifying vector multiplication where the argument vector is first
    * scaled by the given factor.  The default weight is used to initialize
    * new feature weights.
    *
    * @param exampleFeatures  The example's feature indices.
    * @param exampleValues    The example's feature values.
    * @param factor           The scaling factor.
   **/
  public void scaledMultiply(int[] exampleFeatures, double[] exampleValues,
                             double factor) {
    scaledMultiply(exampleFeatures, exampleValues, factor, defaultWeight);
  }

  /**
    * Self-modifying vector multiplication where the argument vector is first
    * scaled by the given factor.
    *
    * @param exampleFeatures  The example's feature indices.
    * @param exampleValues    The example's feature values.
    * @param factor           The scaling factor.
    * @param defaultW         An initial weight for previously unseen
    *                         features.
   **/
  public void scaledMultiply(int[] exampleFeatures, double[] exampleValues,
                             double factor, double defaultW) {
    for (int i = 0; i < exampleFeatures.length; i++) {
      int featureIndex = exampleFeatures[i];
      double s = exampleValues[i];

      double multiplier = factor;
      if (s == 0) multiplier = 1;
      else if (s != 1) multiplier = Math.pow(factor, s);

      double w = getWeight(featureIndex, defaultW) * multiplier;
      setWeight(featureIndex, w, defaultW);
    }
  }


  /**
    * The strength of each feature in the argument vector is multiplied by the
    * corresponding weight in this weight vector and the result is returned as
    * an array of arrays. The first array contains the integer keys of the
    * example's features, as indexed in the lexicon.  The second array gives
    * the double values corresponding to the product of the pairwise
    * multiplication of the strengths of that feature.
    *
    * @param exampleFeatures  The example's feature indices.
    * @param exampleValues    The example's feature values.
    * @param defaultW         An initial weight for previously unseen
    *                         features.
    * @param inverse          When set to <code>true</code> the weight in this
    *                         vector is inverted before the multiplication
    *                         takes place.
    * @return A new example vector representing the pairwise multiplication.
   **/
  public Object[] pairwiseMultiply(int[] exampleFeatures,
                                   double[] exampleValues,
                                   double defaultW, boolean inverse) {
    int resultFeatures[] = new int[exampleFeatures.length];
    double resultValues[] = new double[exampleFeatures.length];

    for (int i = 0; i < exampleFeatures.length; i++) {
      int featureIndex = exampleFeatures[i];
      double w = getWeight(featureIndex, defaultW);
      if (inverse) w = 1 / w;
      resultFeatures[i] = exampleFeatures[i];
      resultValues[i] = w * exampleValues[i];
    }

    return new Object[] {resultFeatures, resultValues};
  }



  /** Empties the weight map. */
  public void clear() { weights = new DVector(defaultCapacity); }
  /** Returns the length of the weight vector. */
  public int size() { return weights.size(); }


  /**
    * Outputs the contents of this <code>SparseWeightVector</code> into the
    * specified <code>PrintStream</code>.  The string representation starts
    * with a <code>"Begin"</code> annotation, ends with an <code>"End"</code>
    * annotation, and without a <code>Lexicon</code> passed as a parameter,
    * the weights are simply printed in the order of their integer indices.
    *
    * @param out  The stream to write to.
   **/
  public void write(PrintStream out) {
    out.println("Begin SparseWeightVector");
    toStringJustWeights(out);
    out.println("End SparseWeightVector");
  }


  /**
    * Outputs the contents of this <code>SparseWeightVector</code> into the
    * specified <code>PrintStream</code>.  The string representation starts
    * with a <code>"Begin"</code> annotation, ends with an <code>"End"</code>
    * annotation, and lists each feature with its corresponding weight on the
    * same, separate line in between.
    *
    * @param out  The stream to write to.
    * @param lex  The feature lexicon.
   **/
  public void write(PrintStream out, Lexicon lex) {
    out.println("Begin SparseWeightVector");
    toStringJustWeights(out, 0, lex);
    out.println("End SparseWeightVector");
  }


  /**
    * Outputs a textual representation of this <code>SparseWeightVector</code>
    * to a stream just like {@link #write(PrintStream)}, but without the
    * <code>"Begin"</code> and <code>"End"</code> annotations.  Without a
    * <code>Lexicon</code> passed as a parameter, the weights are simply
    * printed in the order of their integer indices.
    *
    * @param out  The stream to write to.
   **/
  public void toStringJustWeights(PrintStream out) {
    for (int i = 0; i < weights.size(); i++)
      out.println(weights.get(i));
  }


  /**
    * Outputs a textual representation of this <code>SparseWeightVector</code>
    * to a stream just like {@link #write(PrintStream)}, but without the
    * <code>"Begin"</code> and <code>"End"</code> annotations.  With a
    * <code>Lexicon</code> passed as a parameter, the feature is printed along
    * with each weight.
    *
    * @param out  The stream to write to.
    * @param min  Sets the minimum width for the textual representation of all
    *             features.
    * @param lex  The feature lexicon.
   **/
  public void toStringJustWeights(PrintStream out, int min, Lexicon lex) {
    Map map = lex.getMap();
    Map.Entry[] entries =
      (Map.Entry[]) map.entrySet().toArray(new Map.Entry[map.size()]);
    Arrays.sort(entries,
                new Comparator() {
                  public int compare(Object o1, Object o2) {
                    Map.Entry e1 = (Map.Entry) o1;
                    Map.Entry e2 = (Map.Entry) o2;
                    int i1 = ((Integer) e1.getValue()).intValue();
                    int i2 = ((Integer) e2.getValue()).intValue();
                    if ((i1 < weights.size()) != (i2 < weights.size()))
                      return i1 - i2;
                    return ((Feature) e1.getKey()).compareTo(e2.getKey());
                  }
                });

    int i, biggest = min;
    for (i = 0; i < entries.length; ++i) {
    //for (i = 0; i < weights.size(); ++i)
      String key =
        entries[i].getKey().toString()
        + (((Integer) entries[i].getValue()).intValue() < weights.size()
           ? "" : " (pruned)");
      biggest = Math.max(biggest, key.length());
    }

    if (biggest % 2 == 0) biggest += 2;
    else ++biggest;

    for (i = 0; i < entries.length; ++i) {
    //for (i = 0; i < weights.size(); ++i)
      String key =
        entries[i].getKey().toString()
        + (((Integer) entries[i].getValue()).intValue() < weights.size()
           ? "" : " (pruned)");
      out.print(key);
      for (int j = 0; key.length() + j < biggest; ++j) out.print(" ");

      int index = ((Integer) entries[i].getValue()).intValue();
      out.println(weights.get(index));
    }
  }


  /**
    * Creates a string representation of this <code>SparseWeightVector</code>.
    * This method merely returns the data computed by
    * {@link #write(PrintStream)}.
    *
    * @return A textual representation of this vector.
   **/
  public String toString() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(baos);
    write(out);
    return baos.toString();
  }


  /**
    * Creates a string representation of this <code>SparseWeightVector</code>.
    * This method merely returns the data computed by
    * {@link #write(PrintStream,Lexicon)}.
    *
    * @param lex  The feature lexicon.
    * @return A textual representation of this vector.
   **/
  public String toString(Lexicon lex) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(baos);
    write(out, lex);
    return baos.toString();
  }


  /**
    * Writes the weight vector's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    out.writeString(getClass().getName());
    weights.write(out);
  }


  /**
    * Reads the binary representation of a weight vector of any type from the
    * given stream.  The stream is expected to first return a string
    * containing the fully qualified class name of the weight vector.  If the
    * <i>short</i> value <code>-1</code> appears instead, this method returns
    * <code>null</code>.
    *
    * <p> This method is appropriate for reading weight vectors as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param in The input stream.
    * @return The weight vector read from the stream.
   **/
  public static SparseWeightVector readWeightVector(
      ExceptionlessInputStream in) {
    String name = in.readString();
    if (name == null) return null;
    Class c = ClassUtils.getClass(name);
    SparseWeightVector result = null;

    try { result = (SparseWeightVector) c.newInstance(); }
    catch (Exception e) {
      System.err.println("Error instantiating weight vector '" + name + "':");
      e.printStackTrace();
      in.close();
      System.exit(1);
    }

    result.read(in);
    return result;
  }


  /**
    * Reads the representation of a weight vector with this object's run-time
    * type from the given stream, overwriting the data in this object.
    *
    * <p> This method is appropriate for reading weight vectors as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) { weights.read(in); }


  /**
    * Returns a copy of this <code>SparseWeightVector</code> in which the
    * {@link #weights} variable has been cloned deeply.
    *
    * @return A copy of this <code>SparseWeightVector</code>.
   **/
  public Object clone() {
    SparseWeightVector clone = null;

    try { clone = (SparseWeightVector) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning " + getClass().getName() + ":");
      e.printStackTrace();
      System.exit(1);
    }

    clone.weights = (DVector) weights.clone();
    return clone;
  }


  /**
    * Returns a new, empty weight vector with the same parameter settings as
    * this one.
    *
    * @return An empty weight vector.
   **/
  public SparseWeightVector emptyClone() {
    return new SparseWeightVector();
  }
}

