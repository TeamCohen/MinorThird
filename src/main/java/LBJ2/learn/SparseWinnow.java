package LBJ2.learn;

import java.io.PrintStream;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * Simple sparse Winnow implementation.  It is assumed that
  * {@link Learner#labeler} is a single discrete classifier whose returned
  * feature values are available through the
  * {@link LBJ2.classify.Classifier#allowableValues()} method.  The second
  * value returned from {@link LBJ2.classify.Classifier#allowableValues()} is
  * treated as "positive", and it is assumed there are exactly 2 allowable
  * values.  Assertions will produce error messages if these assumptions do
  * not hold.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.SparseWinnow.Parameters Parameters} as input.  The
  * documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.SparseWinnow.Parameters Parameters} class indicates the
  * default value of the parameter when using the latter type of constructor.
  *
  * @author Nick Rizzolo
 **/
public class SparseWinnow extends LinearThresholdUnit
{
  /** Default for {@link #learningRate}. */
  public static final double defaultLearningRate = 2;
  /** Default for {@link LinearThresholdUnit#threshold}. */
  public static final double defaultThreshold = 16;
  /** Default for {@link LinearThresholdUnit#initialWeight}. */
  public static final double defaultInitialWeight = 1;


  /**
    * The rate at which weights are demoted; default equal to <code>1 /</code>
    * {@link #learningRate}.
   **/
  protected double beta;


  /**
    * {@link #learningRate}, {@link #beta}, and
    * {@link LinearThresholdUnit#threshold} take default values, while the
    * name of the classifier gets the empty string.
   **/
  public SparseWinnow() { this(""); }

  /**
    * Sets {@link #learningRate} to the specified value, {@link #beta} to 1 /
    * {@link #learningRate}, and the {@link LinearThresholdUnit#threshold}
    * takes the default, while the name of the classifier gets the empty
    * string.
    *
    * @param a  The desired value of the promotion parameter.
   **/
  public SparseWinnow(double a) { this("", a); }

  /**
    * Sets {@link #learningRate} and {@link #beta} to the specified values,
    * and the {@link LinearThresholdUnit#threshold} takes the default, while
    * the name of the classifier gets the empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
   **/
  public SparseWinnow(double a, double b) { this("", a, b); }

  /**
    * Sets {@link #learningRate}, {@link #beta}, and
    * {@link LinearThresholdUnit#threshold} to the specified values, while the
    * name of the classifier gets the empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
   **/
  public SparseWinnow(double a, double b, double t) {
    this("", a, b, t);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness, while the name of the classifier gets the empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
   **/
  public SparseWinnow(double a, double b, double t, double pt) {
    this("", a, b, t, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses, while the name of the classifier gets the empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public SparseWinnow(double a, double b, double t, double pt, double nt) {
    this("", a, b, t, pt, nt);
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}, while the name of the classifier gets the
    * empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public SparseWinnow(double a, double b, double t, double pt, double nt,
                      SparseWeightVector v) {
    this("", a, b, t, pt, nt, v);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseWinnow.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public SparseWinnow(Parameters p) { this("", p); }


  /**
    * {@link #learningRate}, {@link #beta}, and
    * {@link LinearThresholdUnit#threshold} take default values.
    *
    * @param n  The name of the classifier.
   **/
  public SparseWinnow(String n) { this(n, defaultLearningRate); }

  /**
    * Sets {@link #learningRate} to the specified value, {@link #beta} to 1 /
    * {@link #learningRate}, and the {@link LinearThresholdUnit#threshold}
    * takes the default.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
   **/
  public SparseWinnow(String n, double a) { this(n, a, 1 / a); }

  /**
    * Sets {@link #learningRate} and {@link #beta} to the specified values,
    * and the {@link LinearThresholdUnit#threshold} takes the default.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
   **/
  public SparseWinnow(String n, double a, double b) {
    this(n, a, b, defaultThreshold);
  }

  /**
    * Sets {@link #learningRate}, {@link #beta}, and
    * {@link LinearThresholdUnit#threshold} to the specified values.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
   **/
  public SparseWinnow(String n, double a, double b, double t) {
    this(n, a, b, t, LinearThresholdUnit.defaultThickness);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
   **/
  public SparseWinnow(String n, double a, double b, double t, double pt) {
    this(n, a, b, t, pt, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public SparseWinnow(String n, double a, double b, double t, double pt,
                      double nt) {
    this(n, a, b, t, pt, nt,
         (SparseWeightVector)
         LinearThresholdUnit.defaultWeightVector.clone());
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public SparseWinnow(String n, double a, double b, double t, double pt,
                      double nt, SparseWeightVector v) {
    super(n);
    Parameters p = new Parameters();
    p.learningRate = a;
    p.beta = b;
    p.threshold = t;
    p.positiveThickness = pt;
    p.negativeThickness = nt;
    p.weightVector = v;
    setParameters(p);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseWinnow.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparseWinnow(String n, Parameters p) {
    super(n);
    setParameters(p);
  }


  /**
    * Sets the values of parameters that control the behavior of this learning
    * algorithm.
    *
    * @param p  The parameters.
   **/
  public void setParameters(Parameters p) {
    super.setParameters(p);
    beta = p.beta;
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p =
      new Parameters((LinearThresholdUnit.Parameters) super.getParameters());
    p.beta = beta;
    return p;
  }


  /**
    * Returns the current value of the {@link #learningRate} variable.
    *
    * @return The value of the {@link #learningRate} variable.
   **/
  public double getLearningRate() { return learningRate; }


  /**
    * Sets the {@link #learningRate} member variable to the specified value.
    *
    * @param t  The new value for {@link #learningRate}.
   **/
  public void setLearningRate(double t) { learningRate = t; }


  /**
    * Returns the current value of the {@link #beta} variable.
    *
    * @return The value of the {@link #beta} variable.
   **/
  public double getBeta() { return beta; }


  /**
    * Sets the {@link #beta} member variable to the specified value.
    *
    * @param t  The new value for {@link #beta}.
   **/
  public void setBeta(double t) { beta = t; }


  /**
    * Returns the learning rate, which is {@link #learningRate} (alpha) if it
    * is a positive example, and {@link #beta} if it is a negative example.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param s                The score.
    * @param label            The example label.
    * @return The appropriate learning rate.
   **/
  public double computeLearningRate(int[] exampleFeatures,
                                    double[] exampleValues, double s,
                                    boolean label) {
    if (label) return learningRate;
    else return beta;
  }


  /**
    * Promotion is simply <code>w_i *= learningRate<sup>x_i</sup></code>.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param rate             The learning rate at which the weights are
    *                         updated.
   **/
  public void promote(int[] exampleFeatures, double[] exampleValues,
                      double rate) {
    update(exampleFeatures, exampleValues, rate);
  }


  /**
    * Demotion is simply <code>w_i *= beta<sup>x_i</sup></code>.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param rate             The learning rate at which the weights are
    *                         updated.
   **/
  public void demote(int[] exampleFeatures, double[] exampleValues,
                     double rate) {
    update(exampleFeatures, exampleValues, rate);
  }


  /**
    * This method performs an update <code>w_i *= base<sup>x_i</sup></code>,
    * initalizing weights in the weight vector as needed.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of values.
    * @param base             As described above.
   **/
  public void update(int[] exampleFeatures, double[] exampleValues,
                     double base) {
    weightVector.scaledMultiply(exampleFeatures, exampleValues, base,
                                initialWeight);
    bias *= base;
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #learningRate}, {@link #beta},
    * {@link LinearThresholdUnit#initialWeight},
    * {@link LinearThresholdUnit#threshold},
    * {@link LinearThresholdUnit#positiveThickness},
    * {@link LinearThresholdUnit#negativeThickness}, and finally
    * {@link LinearThresholdUnit#bias}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    out.println(name + ": " + learningRate + ", " + beta + ", "
                + initialWeight + ", " + threshold + ", " + positiveThickness
                + ", " + negativeThickness + ", " + bias);
    if (lexicon.size() == 0) weightVector.write(out);
    else weightVector.write(out, lexicon);
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeDouble(beta);
  }


  /**
    * Reads the binary representation of a learner with this object's run-time
    * type, overwriting any and all learned or manually specified parameters
    * as well as the label lexicon but without modifying the feature lexicon.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) {
    super.read(in);
    beta = in.readDouble();
  }


  /**
    * Simply a container for all of {@link SparseWinnow}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LinearThresholdUnit.Parameters
  {
    /**
      * The rate at which weights are demoted; default equal to <code>1
      * /</code> {@link #learningRate}.
     **/
    public double beta;


    /** Sets all the default values. */
    public Parameters() {
      learningRate = defaultLearningRate;
      beta = 1 / defaultLearningRate;
      threshold = defaultThreshold;
      initialWeight = defaultInitialWeight;
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(LinearThresholdUnit.Parameters p) {
      super(p);
      beta = 1 / learningRate;
    }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      super(p);
      beta = p.beta;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((SparseWinnow) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();

      if (beta != 1 / LinearThresholdUnit.defaultLearningRate)
        result += ", beta = " + beta;

      if (result.startsWith(", ")) result = result.substring(2);
      return result;
    }
  }
}

