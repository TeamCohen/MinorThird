package LBJ2.learn;

import java.io.PrintStream;

import LBJ2.classify.FeatureVector;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * This is an implementation of the approximate "variance algorithm" of
  * <i>Confidence Weighted Linear Classification</i>, Dredze, et.al (ICML,
  * 2008).  This algorithm envisions each parameter stored in a linear
  * threshold unit's weight vector as having been drawn independently from a
  * normal distribution with an independent mean and variance representing our
  * estimate and confidence in that parameter.  Given a training example, this
  * algorithm then tries to find new values for all these means and
  * confidences such that both of the following hold:
  *
  * <ul>
  *   <li> the KL-divergence between the old and new distributions is
  *        minimized, and
  *   <li> the current example is classified correctly when a weight vector is
  *        drawn according to the current distributions with user-specified
  *        confidence.
  * </ul>
  *
  * <p> In this implementation, the user-specified confidence parameter is a
  * real value representing the result of applying the inverse cumulative
  * function of the normal distribution to a probability (ie, a real value
  * greater than or equal to 0 and less than or equal to 1).  The inverse of
  * the normal cdf is a monotonically increasing function.
  *
  * <p> It is assumed that {@link Learner#labeler} is a single discrete
  * classifier that produces the same feature for every example object and
  * that the values that feature may take are available through the
  * {@link LBJ2.classify.Classifier#allowableValues()} method.  The second
  * value returned from {@link LBJ2.classify.Classifier#allowableValues()} is
  * treated as "positive", and it is assumed there are exactly 2 allowable
  * values.  Assertions will produce error messages if these assumptions do
  * not hold.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.SparseConfidenceWeighted.Parameters Parameters} as
  * input.  The documentation in each member field in this class indicates the
  * default value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.SparseConfidenceWeighted.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Nick Rizzolo
 **/
public class SparseConfidenceWeighted extends LinearThresholdUnit
{
  /** Default value for {@link #confidence}. */
  public static final double defaultConfidence = 2;
  /** Default value for {@link #initialVariance}. */
  public static final double defaultInitialVariance = 1;


  /**
    * The confidence parameter as described above; default
    * {@link #defaultConfidence}.
   **/
  protected double confidence;
  /**
    * The strictly positive initial variance of the parameters; default
    * {@link #defaultInitialVariance}.
   **/
  protected double initialVariance;
  /** The <i>inverses of</i> the current variances of the parameters. */
  protected SparseWeightVector variances;
  /** The bias element of the {@link #variances} vector. */
  protected double variancesBias;


  /** All parameters get default values. */
  public SparseConfidenceWeighted() { this(""); }

  /**
    * Sets the {@link #confidence} parameter.
    *
    * @param c  The desired confidence value.
   **/
  public SparseConfidenceWeighted(double c) { this("", c); }

  /**
    * Sets the {@link #confidence} and {@link #initialVariance} parameters.
    *
    * @param c  The desired confidence value.
    * @param v  The desired initial variance.
   **/
  public SparseConfidenceWeighted(double c, double v) {
    this("", c, v);
  }

  /**
    * Sets the {@link #confidence}, {@link #initialVariance}, and
    * {@link LinearThresholdUnit#weightVector} parameters.
    *
    * @param c  The desired confidence value.
    * @param v  The desired initial variance.
    * @param vm An empty sparse weight vector of means, perhaps of an
    *           alternative subclass of {@link SparseWeightVector}.
   **/
  public SparseConfidenceWeighted(double c, double v, SparseWeightVector vm) {
    this("", c, v, vm);
  }

  /**
    * Sets the {@link #confidence}, {@link #initialVariance},
    * {@link LinearThresholdUnit#weightVector}, and {@link #variances}
    * parameters.  Make sure that the references passed to the last two
    * arguments refer to different objects.
    *
    * @param c  The desired confidence value.
    * @param v  The desired initial variance.
    * @param vm An empty sparse weight vector of means, perhaps of an
    *           alternative subclass of {@link SparseWeightVector}.
    * @param vv An empty sparse weight vector of variances, perhaps of an
    *           alternative subclass of {@link SparseWeightVector}.
   **/
  public SparseConfidenceWeighted(double c, double v, SparseWeightVector vm,
                                  SparseWeightVector vv) {
    this("", c, v, vm, vv);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseConfidenceWeighted.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public SparseConfidenceWeighted(Parameters p) { this("", p); }


  /**
    * All parameters get default values.
    *
    * @param n  The name of the classifier.
   **/
  public SparseConfidenceWeighted(String n) { this(n, defaultConfidence); }

  /**
    * Sets the {@link #confidence} parameter.
    *
    * @param n  The name of the classifier.
    * @param c  The desired confidence value.
   **/
  public SparseConfidenceWeighted(String n, double c) {
    this(n, c, defaultInitialVariance);
  }

  /**
    * Sets the {@link #confidence} and {@link #initialVariance} parameters.
    *
    * @param n  The name of the classifier.
    * @param c  The desired confidence value.
    * @param v  The desired initial variance.
   **/
  public SparseConfidenceWeighted(String n, double c, double v) {
    this(n, c, v, (SparseWeightVector) defaultWeightVector.clone());
  }

  /**
    * Sets the {@link #confidence}, {@link #initialVariance}, and
    * {@link LinearThresholdUnit#weightVector} parameters.
    *
    * @param n  The name of the classifier.
    * @param c  The desired confidence value.
    * @param v  The desired initial variance.
    * @param vm An empty sparse weight vector of means, perhaps of an
    *           alternative subclass of {@link SparseWeightVector}.
   **/
  public SparseConfidenceWeighted(String n, double c, double v,
                                  SparseWeightVector vm) {
    this(n, c, v, vm, (SparseWeightVector) defaultWeightVector.clone());
  }

  /**
    * Sets the {@link #confidence}, {@link #initialVariance},
    * {@link LinearThresholdUnit#weightVector}, and {@link #variances}
    * parameters.  Make sure that the references passed to the last two
    * arguments refer to different objects.
    *
    * @param n  The name of the classifier.
    * @param c  The desired confidence value.
    * @param v  The desired initial variance.
    * @param vm An empty sparse weight vector of means, perhaps of an
    *           alternative subclass of {@link SparseWeightVector}.
    * @param vv An empty sparse weight vector of variances, perhaps of an
    *           alternative subclass of {@link SparseWeightVector}.
   **/
  public SparseConfidenceWeighted(String n, double c, double v,
                                  SparseWeightVector vm,
                                  SparseWeightVector vv) {
    super(n);
    Parameters p = new Parameters();
    p.confidence = c;
    p.initialVariance = v;
    p.weightVector = vm;
    p.variances = vv;
    setParameters(p);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseConfidenceWeighted.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparseConfidenceWeighted(String n, Parameters p) {
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
    confidence = p.confidence;
    initialVariance = p.initialVariance;
    variances = p.variances;
    variancesBias = 1 / initialVariance;
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
    p.confidence = confidence;
    p.initialVariance = initialVariance;
    p.variances = variances.emptyClone();
    return p;
  }


  /**
    * Returns the current value of the {@link #confidence} variable.
    *
    * @return The value of the {@link #confidence} variable.
   **/
  public double getConfidence() { return confidence; }


  /**
    * Sets the {@link #confidence} member variable to the specified
    * value.
    *
    * @param c  The new value for {@link #confidence}.
   **/
  public void setConfidence(double c) { confidence = c; }


  /**
    * Returns the current value of the {@link #initialVariance} variable.
    *
    * @return The value of the {@link #initialVariance} variable.
   **/
  public double getInitialVariance() { return initialVariance; }


  /**
    * Sets the {@link #initialVariance} member variable to the specified
    * value.
    *
    * @param v  The new value for {@link #initialVariance}.
   **/
  public void setInitialVariance(double v) { initialVariance = v; }


  /**
    * Updates the means and variances according to the new labeled example.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param exampleLabels    The example's label(s)
    * @param labelValues      The labels' values
   **/
  public void learn(int[] exampleFeatures, double[] exampleValues,
                    int[] exampleLabels, double[] labelValues) {
    assert exampleLabels.length == 1
      : "Example must have a single label.";
    assert exampleLabels[0] == 0 || exampleLabels[0] == 1
      : "Example has unallowed label value.";

    double y = 2 * exampleLabels[0] - 1;
    double m = y * (weightVector.dot(exampleFeatures, exampleValues) + bias);

    Object sigmaX[] =
      variances.pairwiseMultiply(exampleFeatures, exampleValues,
                                 initialVariance, true);
    int sigmaXFeatures[] = (int[])sigmaX[0];
    double sigmaXValues[] = (double[])sigmaX[1];

    double v =
      FeatureVector.dot(exampleFeatures, exampleValues, sigmaXFeatures,
                        sigmaXValues)
      + 1 / variancesBias;

    double t = 2 * confidence * m + 1;
    double sqrtTerm = t * t - 8 * confidence * (m - confidence * v);
    double alpha = (-t + Math.sqrt(sqrtTerm)) / (4 * confidence * v);

    if (alpha > 0) {
      weightVector.scaledAdd(sigmaXFeatures, sigmaXValues, alpha * y);
      bias += alpha * y / variancesBias;
      variances.scaledAdd(exampleFeatures, exampleValues,
                          2 * alpha * confidence);
      variancesBias += 2 * alpha * confidence;
    }
  }


  /**
   * This method does nothing.  The entire implementation is in
   * {@link #learn(Object)}.
   */
  public void demote(int[] exampleFeatures, double[] exampleValues,
                     double rate) {
  }


  /**
   * This method does nothing.  The entire implementation is in
   * {@link #learn(Object)}.
   */
  public void promote(int[] exampleFeatures, double[] exampleValues,
                      double rate) {
  }


  /**
    * Reinitializes the learner to the state it started at before any learning
    * was performed.
   **/
  public void forget() {
    super.forget();
    variances = variances.emptyClone();
    variancesBias = 1 / initialVariance;
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #confidence} and {@link #initialVariance}.  Next, the annotation
    * <code>Begin means</code> on its own line is followed by the contents of
    * {@link LinearThresholdUnit#weightVector} and the annotation <code>End
    * means</code> on its own line.  Finally, the annotation <code>Begin
    * variances</code> on its own line is followed by the contents of
    * {@link #variances} and the annotation <code>End variances</code> on its
    * own line.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    out.println(name + ": " + confidence + ", " + initialVariance);
    out.println("Means:");
    if (lexicon.size() == 0) weightVector.write(out);
    else weightVector.write(out, lexicon);
    out.println("\nVariances:");
    if (lexicon.size() == 0) variances.write(out);
    else variances.write(out, lexicon);
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeDouble(confidence);
    out.writeDouble(initialVariance);
    out.writeDouble(variancesBias);
    variances.write(out);
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
    confidence = in.readDouble();
    initialVariance = in.readDouble();
    variancesBias = in.readDouble();
    variances = SparseWeightVector.readWeightVector(in);
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone() {
    SparseConfidenceWeighted clone = null;

    try { clone = (SparseConfidenceWeighted) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning SparseConfidenceWeighted: " + e);
      System.exit(1);
    }

    if (variances != null)
      clone.variances = (SparseWeightVector) variances.clone();
    return clone;
  }


  /**
    * Simply a container for all of {@link SparseConfidenceWeighted}'s
    * configurable parameters.  Using instances of this class should make code
    * more readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LinearThresholdUnit.Parameters
  {
    /**
      * The confidence parameter as described above; default
      * {@link SparseConfidenceWeighted#defaultConfidence}.
     **/
    protected double confidence;
    /**
      * The strictly positive initial variance of the parameters; default
      * {@link SparseConfidenceWeighted#defaultInitialVariance}.
     **/
    protected double initialVariance;
    /**
      * The current variances of the parameters; default
      * {@link LinearThresholdUnit#defaultWeightVector}.
     **/
    protected SparseWeightVector variances;


    /** Sets all the default values. */
    public Parameters() {
      confidence = defaultConfidence;
      initialVariance = defaultInitialVariance;
      variances = (SparseWeightVector) defaultWeightVector.clone();
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(LinearThresholdUnit.Parameters p) {
      super(p);
      confidence = defaultConfidence;
      initialVariance = defaultInitialVariance;
      variances = (SparseWeightVector) defaultWeightVector.clone();
    }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      super(p);
      confidence = p.confidence;
      initialVariance = p.initialVariance;
      variances = p.variances;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((SparseConfidenceWeighted) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();

      if (confidence != SparseConfidenceWeighted.defaultConfidence)
        result += ", confidence = " + confidence;
      if (initialVariance != SparseConfidenceWeighted.defaultInitialVariance)
        result += ", initialVariance = " + initialVariance;

      if (result.startsWith(", ")) result = result.substring(2);
      return result;
    }
  }
}

