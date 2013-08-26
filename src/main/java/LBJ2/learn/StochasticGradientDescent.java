package LBJ2.learn;

import java.io.PrintStream;

import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.RealPrimitiveStringFeature;
import LBJ2.classify.ScoreSet;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * Gradient descent is a batch learning algorithm for function approximation
  * in which the learner tries to follow the gradient of the error function to
  * the solution of minimal error.  This implementation is a stochastic
  * approximation to gradient descent in which the approximated function is
  * assumed to have linear form.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.StochasticGradientDescent.Parameters Parameters} as
  * input.  The documentation in each member field in this class indicates the
  * default value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.StochasticGradientDescent.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Nick Rizzolo
 **/
public class StochasticGradientDescent extends Learner
{
  /** Default value for {@link #learningRate}. */
  public static final double defaultLearningRate = 0.1;
  /** Default for {@link #weightVector}. */
  public static final SparseWeightVector defaultWeightVector =
    new SparseWeightVector();


  /** The hypothesis vector; default {@link #defaultWeightVector}. */
  protected SparseWeightVector weightVector;
  /**
    * The bias is stored here rather than as an element of the weight vector.
   **/
  protected double bias;
  /**
    * The rate at which weights are updated; default
    * {@link #defaultLearningRate}.
   **/
  protected double learningRate;


  /**
    * The learning rate takes the default value, while the name of the
    * classifier gets the empty string.
   **/
  public StochasticGradientDescent() { this(""); }

  /**
    * Sets the learning rate to the specified value, while the name of the
    * classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
   **/
  public StochasticGradientDescent(double r) { this("", r); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link StochasticGradientDescent.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public StochasticGradientDescent(Parameters p) { this("", p); }

  /**
    * The learning rate takes the default value.
    *
    * @param n  The name of the classifier.
   **/
  public StochasticGradientDescent(String n) { this(n, defaultLearningRate); }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
   **/
  public StochasticGradientDescent(String n, double r) {
    super(n);
    Parameters p = new Parameters();
    p.learningRate = r;
    setParameters(p);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link StochasticGradientDescent.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public StochasticGradientDescent(String n, Parameters p) {
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
    weightVector = p.weightVector;
    learningRate = p.learningRate;
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p = new Parameters(super.getParameters());
    p.weightVector = weightVector.emptyClone();
    p.learningRate = learningRate;
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
    * @param t  The new value for {@link #learningRate}.
   **/
  public void setLearningRate(double t) { learningRate = t; }


  /** Resets the weight vector to all zeros. */
  public void forget() {
    super.forget();
    weightVector = weightVector.emptyClone();
    bias = 0;
  }


  /**
    * Returns a string describing the output feature type of this classifier.
    *
    * @return <code>"real"</code>
   **/
  public String getOutputType() { return "real"; }


  /**
    * Trains the learning algorithm given an object as an example.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param exampleLabels    The example's label(s).
    * @param labelValues      The labels' values.
   **/
  public void learn(int[] exampleFeatures, double[] exampleValues,
                    int[] exampleLabels, double[] labelValues) {
    assert exampleLabels.length == 1
      : "Example must have a single label.";

    double labelValue = labelValues[0];
    double multiplier =
      learningRate
      * (labelValue - weightVector.dot(exampleFeatures, exampleValues)
         - bias);
    weightVector.scaledAdd(exampleFeatures, exampleValues, multiplier);
    bias += multiplier;
  }


  /**
    * Since this algorithm returns a real feature, it does not return scores.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return <code>null</code>
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues) {
    return null;
  }


  /**
    * Returns the classification of the given example as a single feature
    * instead of a {@link FeatureVector}.
    *
    * @param f  The features array.
    * @param v  The values array.
    * @return The classification of the example as a feature.
   **/
  public Feature featureValue(int[] f, double[] v) {
    return
      new RealPrimitiveStringFeature(containingPackage, name, "",
                                     realValue(f, v));
  }


  /**
    * Simply computes the dot product of the weight vector and the example
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The computed real value.
   **/
  public double realValue(int[] exampleFeatures, double[] exampleValues) {
    return weightVector.dot(exampleFeatures, exampleValues) + bias;
  }


  /**
    * Simply computes the dot product of the weight vector and the feature
    * vector extracted from the example object.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The computed feature (in a vector).
   **/
  public FeatureVector classify(int[] exampleFeatures, double[] exampleValues)
  {
    return new FeatureVector(featureValue(exampleFeatures, exampleValues));
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #learningRate} and {@link #bias}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    out.println(name + ": " + learningRate + ", " + bias);
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
    out.writeDouble(learningRate);
    out.writeDouble(bias);
    weightVector.write(out);
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
    learningRate = in.readDouble();
    bias = in.readDouble();
    weightVector = SparseWeightVector.readWeightVector(in);
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone() {
    StochasticGradientDescent clone = null;

    try { clone = (StochasticGradientDescent) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning StochasticGradientDescent: " + e);
      System.exit(1);
    }

    clone.weightVector = (SparseWeightVector) weightVector.clone();
    return clone;
  }


  /**
    * Simply a container for all of {@link StochasticGradientDescent}'s
    * configurable parameters.  Using instances of this class should make code
    * more readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends Learner.Parameters
  {
    /**
      * The hypothesis vector; default
      * {@link StochasticGradientDescent#defaultWeightVector}.
     **/
    public SparseWeightVector weightVector;
    /**
      * The rate at which weights are updated; default
      * {@link #defaultLearningRate}.
     **/
    public double learningRate;


    /** Sets all the default values. */
    public Parameters() {
      weightVector = (SparseWeightVector) defaultWeightVector.clone();
      learningRate = defaultLearningRate;
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(Learner.Parameters p) {
      super(p);
      weightVector = (SparseWeightVector) defaultWeightVector.clone();
      learningRate = defaultLearningRate;
    }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      super(p);
      weightVector = p.weightVector;
      learningRate = p.learningRate;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((StochasticGradientDescent) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();

      if (learningRate != StochasticGradientDescent.defaultLearningRate)
        result += ", learningRate = " + learningRate;

      if (result.startsWith(", ")) result = result.substring(2);
      return result;
    }
  }
}

