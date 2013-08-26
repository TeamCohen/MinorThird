package LBJ2.learn;

import java.io.PrintStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * Same as the <code>RandomWeightVector</code> class that it extends, except
  * that this vector also contains a bias term (also initialized randomly)
  * which is added to every dot product and affected by every vector addition
  * operation.
  *
  * @author Nick Rizzolo
 **/
public class BiasedRandomWeightVector extends RandomWeightVector
{
  /** The current bias weight. */
  protected double bias;


  /** Instantiates this biased vector with a random bias. */
  public BiasedRandomWeightVector() {
    this(RandomWeightVector.defaultStddev);
  }

  /**
    * Sets the specified standard deviation and a random bias.
    *
    * @param s  The standard deviation.
   **/
  public BiasedRandomWeightVector(double s) {
    super(s);
    bias = random.nextGaussian() * stddev;
  }


  /**
    * Takes the dot product of this <code>BiasedRandomWeightVector</code> with
    * the argument vector, using the specified default weight when one is not
    * yet present in this vector.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param defaultW         The default weight.
    * @return The computed dot product.
   **/
  public double dot(int[] exampleFeatures, double[] exampleValues,
                    double defaultW) {
    return super.dot(exampleFeatures, exampleValues, defaultW) + bias;
  }


  /**
    * Self-modifying vector addition where the argument vector is first scaled
    * by the given factor.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param factor           The scaling factor.
    * @param defaultW     An initial weight for previously unseen features.
   **/
  public void scaledAdd(int[] exampleFeatures, double[] exampleValues,
                        double factor, double defaultW) {
    super.scaledAdd(exampleFeatures, exampleValues, factor, defaultW);
    bias += factor;
  }


  /**
    * Empties the weight map and resets the random number generator.  This
    * means that the same "random" values will be filled in for the weights if
    * the same calls to {@link #dot(int[],double[],double)} and
    * {@link #scaledAdd(int[],double[],double,double)} are made in the same
    * order.
   **/
  public void clear() {
    super.clear();
    bias = random.nextGaussian() * stddev;
  }


  /**
    * Outputs a textual representation of this vector to the specified stream.
    * The string representation is the same as in
    * {@link SparseWeightVector#write(PrintStream)}, with two added lines just
    * after the <code>"Begin"</code> annotation that give the values of
    * {@link #stddev} and {@link #bias}.
    *
    * @param out  The stream to write to.
   **/
  public void write(PrintStream out) {
    out.println("Begin BiasedRandomWeightVector");
    out.println("stddev = " + stddev);
    out.println("bias = " + bias);
    toStringJustWeights(out);
    out.println("End BiasedRandomWeightVector");
  }


  /**
    * Outputs a textual representation of this vector to the specified stream.
    * The string representation is the same as in
    * {@link SparseWeightVector#write(PrintStream)}, with two added lines just
    * after the <code>"Begin"</code> annotation that give the values of
    * {@link #stddev} and {@link #bias}.
    *
    * @param out  The stream to write to.
    * @param lex  The feature lexicon.
   **/
  public void write(PrintStream out, Lexicon lex) {
    out.println("Begin BiasedRandomWeightVector");
    out.println("stddev = " + stddev);
    out.println("bias = " + bias);
    toStringJustWeights(out, 0, lex);
    out.println("End BiasedRandomWeightVector");
  }


  /**
    * Writes the weight vector's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeDouble(bias);
  }


  /**
    * Returns a new, empty weight vector with the same parameter settings as
    * this one.
    *
    * @return An empty weight vector.
   **/
  public SparseWeightVector emptyClone() {
    return new BiasedRandomWeightVector(stddev);
  }
}

