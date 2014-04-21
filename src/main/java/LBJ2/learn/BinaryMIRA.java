package LBJ2.learn;

import java.io.PrintStream;

import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * The Binary MIRA learning algorithm implementation.  This algorithm
  * operates very similarly to {@link SparsePerceptron} with a thick
  * separator, except the learning rate is a function of each training
  * example's margin.  When the weight vector has made a mistake, the full
  * {@link LinearThresholdUnit#learningRate} will be used.  When the weight
  * vector did not make a mistake, {@link LinearThresholdUnit#learningRate} is
  * multiplied by the following value before the update takes place.
  *
  * <p>
  * <blockquote>
  * <i>(beta/2 - y(w*x)) / ||x||<sup>2</sup></i>
  * </blockquote>
  *
  * <p> In the expression above, <i>w</i> is the weight vector, <i>y</i>
  * represents the label of the example vector <i>x</i>, <i>*</i> stands for
  * inner product, and <i>beta</i> is a user supplied parameter.  If this
  * expression turns out to be non-positive (i.e., if <i>y(w*x) >=
  * beta/2</i>), no update is made for that example.
  *
  * <p> It is assumed that {@link Learner#labeler} is a single discrete
  * classifier that produces the same feature for every example object and
  * that the values that feature may take are available through the
  * {@link Classifier#allowableValues()} method.  The second value returned
  * from {@link Classifier#allowableValues()} is treated as "positive", and it
  * is assumed there are exactly 2 allowable values.  Assertions will produce
  * error messages if these assumptions do not hold.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.BinaryMIRA.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.BinaryMIRA.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Arindam Saha
 **/
public class BinaryMIRA extends SparsePerceptron {
  /**
    * Used to decide if two values are nearly equal to each other.
    * @see #nearlyEqualTo(double,double)
   **/
  public static final double TOLERANCE = 0.000000001;
  /** Default value for {@link #beta}. */
  public static final double defaultBeta = 2;
  /** Default value for {@link #learningRate}. */
  public static final double defaultLearningRate = 1;


  /**
    * The user supplied learning algorithm parameter; default
    * {@link #defaultBeta}. The learning rate changes as a function of
    * <code>beta</code>.
   **/
  protected double beta;


  /**
    * The learning rate and beta take default values while the name of
    * the classifier takes the empty string.
   **/
  public BinaryMIRA() { this(""); }

  /**
    * Sets the learning rate to the specified value, and beta to the
    * default, while the name of the classifier takes the empty string.
    *
    * @param r The desired learning rate value.
   **/
  public BinaryMIRA(double r) { this("", r); }

  /**
    * Sets the learning rate and beta to the specified values, while
    * the name of the classifier takes the empty string.
    *
    * @param r The desired learning rate value.
    * @param B the desired beta value.
   **/
  public BinaryMIRA(double r, double B) { this("", r, B); }

  /**
    * Sets the learning rate, beta and the weight vector to the specified
    * values.
    *
    * @param r The desired learning rate.
    * @param B The desired beta value.
    * @param v The desired weight vector.
   **/
  public BinaryMIRA(double r, double B, SparseWeightVector v) {
    this("", r, B, v);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link BinaryMIRA.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public BinaryMIRA(Parameters p) { this("", p); }


  /**
    * Sets the name of the classifier to the specified value, while the
    * learning rate and beta take default values.
    *
    * @param n The name of the classifier.
   **/
  public BinaryMIRA(String n) { this(n, defaultLearningRate); }

  /**
    * Sets the name of the classifier and learning rate to the specified
    * values, while beta takes the default value.
    *
    * @param n The name of the classifier.
    * @param r The desired learning rate.
   **/
  public BinaryMIRA(String n, double r) { this(n, r, defaultBeta); }

  /**
    * Sets the name of the classifier, the learning rate and beta to
    * the specified values.
    *
    * @param n The name of the classifier.
    * @param r The desired learning rate.
    * @param B The desired beta value.
   */
  public BinaryMIRA(String n, double r, double B) {
    this(n, r, B,
         (SparseWeightVector)
         LinearThresholdUnit.defaultWeightVector.clone());
  }

  /**
    * Sets the name of the classifier, the learning rate, beta and the
    * weight vector to the specified values.  Use this constructor to specify
    * an alternative subclass of {@link SparseWeightVector}.
    *
    * @param n The name of the classifier.
    * @param r The desired learning rate.
    * @param B The desired beta value.
    * @param v The desired weight vector.
   */
  public BinaryMIRA(String n, double r, double B, SparseWeightVector v) {
    super(n);
    Parameters p = new Parameters();
    p.learningRate = r;
    p.weightVector = v;
    p.beta = B;
    setParameters(p);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link BinaryMIRA.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public BinaryMIRA(String n, Parameters p) {
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
      new Parameters((SparsePerceptron.Parameters) super.getParameters());
    p.beta = beta;
    return p;
  }


  /**
    * Returns the current value of the {@link #beta} member variable.
    *
    * @return The value of the {@link #beta} variable.
   **/
  public double getBeta() { return beta; }


  /**
    * Sets the {@link #beta} member variable to the specified value.
    *
    * @param B The new value for {@link #beta}.
   **/
  public void setBeta(double B) { beta = B; }


  /**
    * Determines if the weights should be promoted.
    *
    * @param label              The label of the example object.
    * @param s                  The score of the example object.
    * @param threshold          The LTU threshold.
    * @param positiveThickness  The thickness of the hyperplane on the
    *                           positive side.
    * @return <code>true</code> iff the weights should be promoted.
   **/
  public boolean shouldPromote(boolean label, double s, double threshold,
                               double positiveThickness) {
    return label;
  }

  /**
    * Determines if the weights should be promoted.
    *
    * @param label              The label of the example object.
    * @param s                  The score of the example object.
    * @param threshold          The LTU threshold.
    * @param negativeThickness  The thickness of the hyperplane on the
    *                           negative side.
    * @return <code>true</code> iff the weights should be demoted.
   **/
  public boolean shouldDemote(boolean label, double s, double threshold,
                              double negativeThickness) {
    return !label;
  }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and adds it to the weight vector.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param rate             The learning rate at which the weights are
    *                         updated.
   **/
  public void promote(int[] exampleFeatures, double[] exampleValues,
                      double rate) {
    if (!nearlyEqualTo(rate, 0.0)) {
      super.promote(exampleFeatures, exampleValues, rate);
    }
  }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and subtracts it from the weight vector.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param rate             The learning rate at which the weights are
    *                         updated.
   **/
  public void demote(int[] exampleFeatures, double[] exampleValues,
                     double rate) {
    if (!nearlyEqualTo(rate, 0.0)) {
      super.demote(exampleFeatures, exampleValues, rate);
    }
  }


  /**
    * Determines if <code>a</code> is nearly equal to <code>b</code> based on
    * the value of the <code>TOLERANCE</code> member variable.
    *
    * @param a  The first value.
    * @param b  The second value.
    * @return <code>true</code> iff they are nearly equal.
   **/
  private static boolean nearlyEqualTo(double a, double b) {
    return -TOLERANCE < a - b && a - b < TOLERANCE;
  }


  /**
    * Computes the learning rate for this example.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param s                The score for the example object.
    * @param label            The label.
    * @return The new learning rate.
   **/
  public double computeLearningRate(int[] exampleFeatures,
                                    double[] exampleValues, double s,
                                    boolean label) {
    double labelVal = label? 1: -1;

    double x = (beta / 2 - labelVal * s)
      / (FeatureVector.L2NormSquared(exampleValues) + 1);

    double rate = 1;
    if (x < 0) rate = 0;
    else if (x < 1) rate = x;

    rate *= learningRate;

    return rate;
  }


  /**
    * Returns the original value of the {@link #learningRate} variable.
    *
    * @return The value of the {@link #learningRate} variable.
   **/
  public double getLearningRate() { return learningRate; }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #learningRate}, {@link #beta},
    * {@link LinearThresholdUnit#initialWeight},
    * {@link LinearThresholdUnit#threshold},
    * {@link LinearThresholdUnit#positiveThickness},
    * {@link LinearThresholdUnit#negativeThickness},
    * and finally {@link LinearThresholdUnit#bias}.
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
    * Simply a container for all of {@link BinaryMIRA}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends SparsePerceptron.Parameters {
    /**
      * The user supplied learning algorithm parameter; default
      * {@link #defaultBeta}. The learning rate changes as a function of
      * <code>beta</code>.
     */
    public double beta;


    /** Sets all the default values. */
    public Parameters() {
      beta = defaultBeta;
      learningRate = defaultLearningRate;
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(SparsePerceptron.Parameters p) {
      super(p);
      beta = defaultBeta;
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
      ((BinaryMIRA) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();

      if (beta != BinaryMIRA.defaultBeta)
        result += ", beta = " + beta;

      if (result.startsWith(", ")) result = result.substring(2);
      return result;
    }
  }
}



