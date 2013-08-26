package LBJ2.learn;

import java.io.PrintStream;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * Same as the <code>SparseWeightVector</code> class that it extends, except
  * that this vector also contains a bias term which is added to every dot
  * product and affected by every vector addition operation.
  *
  * @author Nick Rizzolo
 **/
public class BiasedWeightVector extends SparseWeightVector
{
  /** Default value for {@link #initialBias}. */
  protected static final double defaultInitialBias = 0;


  /** The first value for {@link #bias}. */
  protected double initialBias;
  /** The current bias weight. */
  protected double bias;


  /** Instantiates this biased vector with default parameter values. */
  public BiasedWeightVector() { this(defaultInitialBias); }

  /**
    * Instantiates this biased vector with the specified initial bias.
    *
    * @param b  The inital bias.
   **/
  public BiasedWeightVector(double b) { initialBias = bias = b; }


  /**
    * Takes the dot product of this <code>BiasedWeightVector</code> with the
    * argument vector, using the specified default weight when one is not yet
    * present in this vector.
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
    * @param factor   The scaling factor.
    * @param defaultW An initial weight for previously unseen features.
   **/
  public void scaledAdd(int[] exampleFeatures, double[] exampleValues,
                        double factor, double defaultW) {
    super.scaledAdd(exampleFeatures, exampleValues, factor, defaultW);
    bias += factor;
  }


  /** Empties the weight map. */
  public void clear() {
    super.clear();
    bias = initialBias;
  }


  /**
    * Outputs the contents of this <code>BiasedWeightVector</code> into the
    * specified <code>PrintStream</code>.  The string representation is the
    * same as in the super class, with two added lines just after the
    * <code>"Begin"</code> annotation that give the values of {@link #bias}
    * and {@link #initialBias}.
    *
    * @param out  The stream to write to.
   **/
  public void write(PrintStream out) {
    out.println("Begin BiasedWeightVector");
    out.println("bias = " + bias);
    out.println("initialBias = " + initialBias);
    toStringJustWeights(out);
    out.println("End BiasedWeightVector");
  }


  /**
    * Outputs the contents of this <code>BiasedWeightVector</code> into the
    * specified <code>PrintStream</code>.  The string representation is the
    * same as in the super class, with two added lines just after the
    * <code>"Begin"</code> annotation that give the values of {@link #bias}
    * and {@link #initialBias}.
    *
    * @param out  The stream to write to.
    * @param lex  The feature lexicon.
   **/
  public void write(PrintStream out, Lexicon lex) {
    out.println("Begin BiasedWeightVector");
    out.println("bias = " + bias);
    out.println("initialBias = " + initialBias);
    toStringJustWeights(out, 0, lex);
    out.println("End BiasedWeightVector");
  }


  /**
    * Writes the weight vector's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeDouble(initialBias);
    out.writeDouble(bias);
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
  public void read(ExceptionlessInputStream in) {
    super.read(in);
    initialBias = in.readDouble();
    bias = in.readDouble();
  }


  /**
    * Returns a new, empty weight vector with the same parameter settings as
    * this one.
    *
    * @return An empty weight vector.
   **/
  public SparseWeightVector emptyClone() {
    return new BiasedWeightVector(initialBias);
  }
}

