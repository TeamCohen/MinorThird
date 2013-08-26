package LBJ2.learn;

import java.io.PrintStream;


/**
  * Simple sparse Perceptron implementation.  It is assumed that
  * {@link Learner#labeler} is a single discrete classifier that produces the
  * same feature for every example object and that the values that feature may
  * take are available through the
  * {@link LBJ2.classify.Classifier#allowableValues()} method.  The second
  * value returned from {@link LBJ2.classify.Classifier#allowableValues()} is
  * treated as "positive", and it is assumed there are exactly 2 allowable
  * values.  Assertions will produce error messages if these assumptions do
  * not hold.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.SparsePerceptron.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.SparsePerceptron.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Nick Rizzolo
 **/
public class SparsePerceptron extends LinearThresholdUnit
{
  /**
    * The learning rate and threshold take default values, while the name of
    * the classifier gets the empty string.
   **/
  public SparsePerceptron() { super(); }

  /**
    * Sets the learning rate to the specified value, and the threshold takes
    * the default, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
   **/
  public SparsePerceptron(double r) { super(r); }

  /**
    * Sets the learning rate and threshold to the specified values, while the
    * name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
   **/
  public SparsePerceptron(double r, double t) {
    super(r, t);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public SparsePerceptron(double r, double t, double pt) {
    super(r, t, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public SparsePerceptron(double r, double t, double pt, double nt) {
    super(r, t, pt, nt);
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}, while the name of the classifier gets the
    * empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public SparsePerceptron(double r, double t, double pt, double nt,
                          SparseWeightVector v) {
    super("", r, t, pt, nt, v);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparsePerceptron.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public SparsePerceptron(Parameters p) { super(p); }


  /**
    * The learning rate and threshold take default values.
    *
    * @param n  The name of the classifier.
   **/
  public SparsePerceptron(String n) { super(n); }

  /**
    * Sets the learning rate to the specified value, and the threshold takes
    * the default.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
   **/
  public SparsePerceptron(String n, double r) {
    super(n, r);
  }

  /**
    * Sets the learning rate and threshold to the specified values.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
   **/
  public SparsePerceptron(String n, double r, double t) {
    super(n, r, t);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public SparsePerceptron(String n, double r, double t, double pt) {
    super(n, r, t, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public SparsePerceptron(String n, double r, double t, double pt, double nt)
  {
    super(n, r, t, pt, nt);
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public SparsePerceptron(String n, double r, double t, double pt, double nt,
                          SparseWeightVector v) {
    super(n, r, t, pt, nt, v);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparsePerceptron.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparsePerceptron(String n, Parameters p) {
    super(n, p);
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
    return p;
  }


  /**
    * Returns the current value of the {@link #learningRate} variable.
    *
    * @return The value of the {@link #learningRate} variable.
   **/
  public double getLearningRate() { return learningRate; }


  /**
    * Sets the {@link #learningRate} member variable to the specified
    * value.
    *
    * @param r  The new value for {@link #learningRate}.
   **/
  public void setLearningRate(double r) { learningRate = r; }


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
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #learningRate}, {@link LinearThresholdUnit#initialWeight},
    * {@link LinearThresholdUnit#threshold},
    * {@link LinearThresholdUnit#positiveThickness},
    * {@link LinearThresholdUnit#negativeThickness}, and finally
    * {@link LinearThresholdUnit#bias}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    out.println(name + ": " + learningRate + ", " + initialWeight + ", "
                + threshold + ", " + positiveThickness + ", "
                + negativeThickness + ", " + bias);
    if (lexicon.size() == 0) weightVector.write(out);
    else weightVector.write(out, lexicon);
  }


  /**
    * Simply a container for all of {@link SparsePerceptron}'s configurable
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

