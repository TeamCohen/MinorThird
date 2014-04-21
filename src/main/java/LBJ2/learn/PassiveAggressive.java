package LBJ2.learn;

import java.io.PrintStream;
import LBJ2.classify.Classifier;
import LBJ2.classify.FeatureVector;


/**
  * The Passive Aggressive learning algorithm implementation.  This algorithm
  * operates very similarly to {@link SparsePerceptron} with a thick
  * separator, except the learning rate is a function of each training
  * example's margin.  {@link LinearThresholdUnit#learningRate} is
  * defined for each example as the following value.
  *
  * <p>
  * <blockquote>
  * <i>(1 - y(w*x)) / ||x||<sup>2</sup></i>
  * </blockquote>
  *
  * <p> In the expression above, <i>w</i> is the weight vector, <i>y</i>
  * represents the label of the example vector <i>x</i>, <i>*</i> stands for
  * inner product.  If this expression turns out to be non-positive
  * (i.e., if <i>y(w*x) >= 1</i>), no update is made for that example.
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
  * {@link LBJ2.learn.PassiveAggressive.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.PassiveAggressive.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.  Note that this learner will not actually use any
  * user-supplied value for
  * {@link LBJ2.learn.LinearThresholdUnit.Parameters#learningRate} as this is
  * computed automatically.
  *
  * @author Michael Paul
 **/
public class PassiveAggressive extends LinearThresholdUnit
{
  /**
    * The learning rate and threshold take default values, while the name of
    * the classifier gets the empty string.
   **/
  public PassiveAggressive() { this(""); }


  /**
    * Sets the learning rate and threshold to the specified values, while the
    * name of the classifier gets the empty string.
    *
    * @param t  The desired threshold value.
   **/
  public PassiveAggressive(double t) {
    this("", t);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness, while the name of the classifier gets the empty string.
    *
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public PassiveAggressive(double t, double pt) {
    this("", t, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses, while the name of the classifier gets the empty string.
    *
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public PassiveAggressive(double t, double pt, double nt) {
    this("", t, pt, nt);
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}, while the name of the classifier gets the
    * empty string.
    *
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public PassiveAggressive(double t, double pt, double nt,
                          SparseWeightVector v) {
    this("", t, pt, nt, v);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparsePerceptron.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public PassiveAggressive(Parameters p) { this("", p); }


  /**
    * Sets the learning rate to the specified value, and the threshold takes
    * the default.
    *
    * @param n  The name of the classifier.
   **/
  public PassiveAggressive(String n) {
    this(n, LinearThresholdUnit.defaultThreshold);
  }

  /**
    * Sets the learning rate and threshold to the specified values.
    *
    * @param n  The name of the classifier.
    * @param t  The desired threshold value.
   **/
  public PassiveAggressive(String n, double t) {
    this(n, t, LinearThresholdUnit.defaultThickness);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness.
    *
    * @param n  The name of the classifier.
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public PassiveAggressive(String n, double t, double pt) {
    this(n, t, pt, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses.
    *
    * @param n  The name of the classifier.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public PassiveAggressive(String n, double t, double pt, double nt) {
    this(n, t, pt, nt,
         (SparseWeightVector)
         LinearThresholdUnit.defaultWeightVector.clone());
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}.
    *
    * @param n  The name of the classifier.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public PassiveAggressive(String n, double t, double pt, double nt,
                          SparseWeightVector v) {
    super(n, LinearThresholdUnit.defaultLearningRate, t, pt, nt, v);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparsePerceptron.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public PassiveAggressive(String n, Parameters p) {
    super(n, p);
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    return
      new Parameters((LinearThresholdUnit.Parameters) super.getParameters());
  }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and adds it to the weight vector.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param rate  The learning rate at which the weights are updated.
   **/
  public void promote(int[] exampleFeatures, double[] exampleValues,
                      double rate) {
    weightVector.scaledAdd(exampleFeatures, exampleValues, rate,
                           initialWeight);
    bias += rate;
  }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and subtracts it from the weight vector.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param rate  The learning rate at which the weights are updated.
   **/
  public void demote(int[] exampleFeatures, double[] exampleValues,
                     double rate) {
    weightVector.scaledAdd(exampleFeatures, exampleValues, -rate,
                           initialWeight);
    bias -= rate;
  }


  /**
    * Computes the value of the learning rate for this example.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param s       The score for the example object
    * @param label   The label
   **/
  public double computeLearningRate(int[] exampleFeatures,
                                    double[] exampleValues, double s,
                                    boolean label) {
    double labelVal = label ? 1: -1;

    double rate = (1 - labelVal * s)
                  / (FeatureVector.L2NormSquared(exampleValues) + 1);

    if (rate < 0) rate = 0;
    return rate;
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #learningRate},
    * {@link LinearThresholdUnit#initialWeight},
    * {@link LinearThresholdUnit#threshold},
    * {@link LinearThresholdUnit#positiveThickness},
    * {@link LinearThresholdUnit#negativeThickness},
    * and finally {@link LinearThresholdUnit#bias}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    out.println(name + ": " + learningRate + ", "
                + initialWeight + ", " + threshold + ", " + positiveThickness
                + ", " + negativeThickness + ", " + bias);
    if (lexicon.size() == 0) weightVector.write(out);
    else weightVector.write(out, lexicon);
  }


  /**
    * Simply a container for all of {@link PassiveAggressive}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LinearThresholdUnit.Parameters
  {
    /** Sets all the default values. */
    public Parameters() { }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(LinearThresholdUnit.Parameters p) { super(p); }


    /** Copy constructor. */
    public Parameters(Parameters p) { super(p); }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((LinearThresholdUnit) l).setParameters(this);
    }
  }
}

