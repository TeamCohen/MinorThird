package LBJ2.learn;

import java.util.Arrays;

import LBJ2.classify.Classifier;
import LBJ2.classify.DiscretePrimitiveStringFeature;
import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.ScoreSet;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.FVector;


/**
  * A <code>LinearThresholdUnit</code> is a {@link Learner} for binary
  * classification in which a score is computed as a linear function a
  * <i>weight vector</i> and the input example, and the decision is made by
  * comparing the score to some threshold quantity.  Deriving a linear
  * threshold algorithm from this class gives the programmer more flexible
  * access to the score it computes as well as its promotion and demotion
  * methods (if it's on-line).
  *
  * <p> On-line, mistake driven algorithms derived from this class need only
  * override the {@link #promote(int[],double[],double)}, and
  * {@link #demote(int[],double[],double)}
  * methods, assuming the score returned by the {@link #score(Object)} method
  * need only be compared with {@link #threshold} to make a prediction.
  * Otherwise, the {@link #classify(Object)} method also needs to be
  * overridden.  If the algorithm is not mistake driven, the
  * {@link #learn(Object)} method needs to be overridden as well.
  *
  * <p> It is assumed that {@link Learner#labeler} is a single discrete
  * classifier that produces the same feature for every example object and
  * that the values that feature may take are available through the
  * {@link Classifier#allowableValues()} method.  The first value returned
  * from {@link Classifier#allowableValues()} is treated as "negative", and it
  * is assumed there are exactly 2 allowable values.  Assertions will produce
  * error messages if these assumptions do not hold.
  *
  * <p> Fitting a "thick separator" instead of just a hyperplane is also
  * supported through this class.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.LinearThresholdUnit.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.LinearThresholdUnit.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Nick Rizzolo
 **/
public abstract class LinearThresholdUnit extends Learner
{
  /** Default for {@link #initialWeight}. */
  public static final double defaultInitialWeight = 0;
  /** Default for {@link #threshold}. */
  public static final double defaultThreshold = 0;
  /** Default for {@link #positiveThickness}. */
  public static final double defaultThickness = 0;
  /** Default value for {@link #learningRate}. */
  public static final double defaultLearningRate = 0.1;
  /** Default for {@link #weightVector}. */
  public static final SparseWeightVector defaultWeightVector =
    new SparseWeightVector();

  /**
    * The rate at which weights are updated; default
    * {@link #defaultLearningRate}.
   **/
  protected double learningRate;
  /** The LTU's weight vector; default is an empty vector. */
  protected SparseWeightVector weightVector;
  /**
    * The weight associated with a feature when first added to the vector;
    * default {@link #defaultInitialWeight}.
   **/
  protected double initialWeight;
  /**
    * The score is compared against this value to make predictions; default
    * {@link LinearThresholdUnit#defaultThreshold}.
   **/
  protected double threshold;
  /**
    * The bias is stored here rather than as an element of the weight vector.
   **/
  protected double bias;
  /**
    * The thickness of the hyperplane on the positive side; default
    * {@link #defaultThickness}.
   **/
  protected double positiveThickness;
  /**
    * The thickness of the hyperplane on the negative side; default equal to
    * {@link #positiveThickness}.
   **/
  protected double negativeThickness;
  /** The label producing classifier's allowable values. */
  protected String[] allowableValues;



  /**
    * Default constructor.  The learning rate and threshold take default
    * values, while the name of the classifier gets the empty string.
   **/
  public LinearThresholdUnit() { this(""); }

  /**
    * Initializing constructor. Sets the learning rate to the specified value,
    * and the threshold and thickness take the default, while the name of the
    * classifier gets the empty string.
    *
    * @param r  The desired learning rate.
   **/
  public LinearThresholdUnit(double r) { this("", r); }


  /**
    * Sets the learning rate and threshold to the specified values, while the
    * name of the classifier gets the empty string.

    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
   **/
  public LinearThresholdUnit(double r, double t) { this("", r, t); }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public LinearThresholdUnit(double r, double t, double pt) {
    this("", r, t, pt);
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
  public LinearThresholdUnit(double r, double t, double pt, double nt) {
    this("", r, t, pt, nt);
  }

  /**
    * Initializing constructor.  Sets the threshold, positive thickness, and
    * negative thickness to their default values.
    *
    * @param n  The name of the classifier.
   **/
  protected LinearThresholdUnit(String n) { this(n, defaultLearningRate); }


  /**
    * Default constructor.  Sets the threshold, positive thickness, and
    * negative thickness to their default values.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate.
   **/
  protected LinearThresholdUnit(String n, double r) {
      this(n, r, defaultThreshold);
  }

  /**
    * Initializing constructor.  Sets the threshold to the specified value,
    * while the positive and negative thicknesses get their defaults.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate.
    * @param t  The desired value for the threshold.
   **/
  protected LinearThresholdUnit(String n, double r, double t) {
    this(n, r, t, defaultThickness);
  }

  /**
    * Initializing constructor.  Sets the threshold and positive thickness to
    * the specified values, and the negative thickness is set to the same
    * value as the positive thickness.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate.
    * @param t  The desired value for the threshold.
    * @param pt The desired thickness.
   **/
  protected LinearThresholdUnit(String n, double r, double t, double pt) {
    this(n, r, t, pt, pt);
  }

  /**
    * Initializing constructor.  Sets the threshold, positive thickness, and
    * negative thickness to the specified values.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate.
    * @param t  The desired value for the threshold.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  protected LinearThresholdUnit(String n, double r, double t, double pt,
                                double nt) {
    this(n, r, t, pt, nt, (SparseWeightVector) defaultWeightVector.clone());
  }

  /**
    * Initializing constructor.  Sets the threshold, positive thickness, and
    * negative thickness to the specified values.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate.
    * @param t  The desired value for the threshold.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An initial weight vector.
   **/
  protected LinearThresholdUnit(String n, double r, double t, double pt,
                                double nt, SparseWeightVector v) {
    super(n);
    Parameters p = new Parameters();
    p.weightVector = v;
    p.threshold = t;
    p.learningRate = r;
    p.positiveThickness = pt;
    p.negativeThickness = nt;
    setParameters(p);
  }


  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link LinearThresholdUnit.Parameters} object.
    * The name of the classifier is the empty string.
    *
    * @param p  The settings of all parameters.
   **/
  protected LinearThresholdUnit(Parameters p) { this("", p); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link LinearThresholdUnit.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  protected LinearThresholdUnit(String n, Parameters p) {
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
    learningRate = p.learningRate;
    weightVector = p.weightVector;
    initialWeight = p.initialWeight;
    threshold = p.threshold;
    bias = p.initialWeight;
    positiveThickness = p.thickness + p.positiveThickness;
    negativeThickness = p.thickness + p.negativeThickness;
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p = new Parameters(super.getParameters());
    p.learningRate = learningRate;
    p.weightVector = weightVector.emptyClone();
    p.initialWeight = initialWeight;
    p.threshold = threshold;
    p.positiveThickness = positiveThickness;
    p.negativeThickness = negativeThickness;
    return p;
  }


  /**
    * Sets the labels list.
    *
    * @param l  A new label producing classifier.
   **/
  public void setLabeler(Classifier l) {
    if (!(l == null || l.allowableValues().length == 2)) {
      System.err.println(
          "Error: " + name
          + ": An LTU must be given a single binary label classifier.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    super.setLabeler(l);
    allowableValues = l == null ? null : l.allowableValues();
    labelLexicon.clear();
    labelLexicon.lookup(
        new DiscretePrimitiveStringFeature(
              l.containingPackage, l.name, "", allowableValues[0], (short) 0,
              (short) 2),
        true);
    labelLexicon.lookup(
        new DiscretePrimitiveStringFeature(
              l.containingPackage, l.name, "", allowableValues[1], (short) 1,
              (short) 2),
        true);
    predictions = new FVector(2);
    createPrediction(0);
    createPrediction(1);
  }


  /**
    * Returns the current value of the {@link #initialWeight} variable.
    *
    * @return The value of the {@link #initialWeight} variable.
   **/
  public double getInitialWeight() { return initialWeight; }


  /**
    * Sets the {@link #initialWeight} member variable to the specified value.
    *
    * @param w  The new value for {@link #initialWeight}.
   **/
  public void setInitialWeight(double w) { initialWeight = w; }


  /**
    * Returns the current value of the {@link #threshold} variable.
    *
    * @return The value of the {@link #threshold} variable.
   **/
  public double getThreshold() { return threshold; }


  /**
    * Sets the {@link #threshold} member variable to the specified value.
    *
    * @param t  The new value for {@link #threshold}.
   **/
  public void setThreshold(double t) { threshold = t; }


  /**
    * Returns the current value of the {@link #positiveThickness} variable.
    *
    * @return The value of the {@link #positiveThickness} variable.
   **/
  public double getPositiveThickness() { return positiveThickness; }


  /**
    * Sets the {@link #positiveThickness} member variable to the specified
    * value.
    *
    * @param t  The new value for {@link #positiveThickness}.
   **/
  public void setPositiveThickness(double t) {
    positiveThickness = t;
  }


  /**
    * Returns the current value of the {@link #negativeThickness} variable.
    *
    * @return The value of the {@link #negativeThickness} variable.
   **/
  public double getNegativeThickness() { return negativeThickness; }


  /**
    * Sets the {@link #negativeThickness} member variable to the specified
    * value.
    *
    * @param t  The new value for {@link #negativeThickness}.
   **/
  public void setNegativeThickness(double t) { negativeThickness = t; }


  /**
    * Sets the {@link #positiveThickness} and {@link #negativeThickness}
    * member variables to the specified value.
    *
    * @param t  The new thickness value.
   **/
  public void setThickness(double t) {
    positiveThickness = negativeThickness = t;
  }


  /**
    * Returns the array of allowable values that a feature returned by this
    * classifier may take.
    *
    * @return If a labeler has not yet been established for this LTU, byte
    *         strings equivalent to <code>{ "*", "*" }</code> are returned,
    *         which indicates to the compiler that classifiers derived from
    *         this learner will return features that take one of two values
    *         that are specified in the source code.  Otherwise, the allowable
    *         values of the labeler are returned.
   **/
  public String[] allowableValues() {
    if (allowableValues == null) allowableValues = new String[]{ "*", "*" };
    return allowableValues;
  }


  /**
    * The default training algorithm for a linear threshold unit consists of
    * evaluating the example object with the {@link #score(Object)} method and
    * {@link #threshold}, checking the result of evaluation against the label,
    * and, if they are different, promoting when the label is positive or
    * demoting when the label is negative.
    *
    * <p> This method does not call {@link #classify(Object)}; it calls
    * {@link #score(Object)} directly.
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

    boolean label = (exampleLabels[0] == 1);

    double s = score(exampleFeatures, exampleValues);

    if (shouldPromote(label, s, threshold, positiveThickness))
      promote(exampleFeatures, exampleValues,
              computeLearningRate(exampleFeatures, exampleValues, s, label));
    if (shouldDemote(label, s, threshold, negativeThickness))
      demote(exampleFeatures, exampleValues,
             computeLearningRate(exampleFeatures, exampleValues, s, label));
  }


  /**
    * Computes the value of the {@link #learningRate} variable if needed
    * and returns the value. By default, the current value of
    * {@link #learningRate}
    * is returned.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param s The score of the example object
    * @param label The label of the example object
    * @return The computed value of the {@link #learningRate} variable
   **/
  public double computeLearningRate(int[] exampleFeatures,
                                    double[] exampleValues, double s,
                                    boolean label) {
    return learningRate;
  }


  /**
    * Determines if the weights should be promoted
    *
    * @param label The label of the example object
    * @param s     The score of the example object
    * @param threshold The LTU threshold
    * @param positiveThickness The thickness of the hyperplane on
                               the positive side
    * @return True if the weights should be promoted, false otherwise.
   **/
  public boolean shouldPromote(boolean label, double s, double threshold,
                               double positiveThickness) {
    return (label && s < threshold + positiveThickness);
  }

  /**
    * Determines if the weights should be demoted
    *
    * @param label The label of the example object
    * @param s     The score of the example object
    * @param threshold The LTU threshold
    * @param negativeThickness The thickness of the hyperplane on
                               the negative side
    * @return True if the weights should be demoted, false otherwise.
   **/
  public boolean shouldDemote(boolean label, double s, double threshold,
                              double negativeThickness) {
    return (!label && s >= threshold - negativeThickness);
  }


  /**
    * Initializes the weight vector array to the size of the specified number
    * of features, setting each weight equal to {@link #initialWeight}.
   **/
  public void initialize(int numExamples, int numFeatures) {
    double[] weights = new double[numFeatures];
    Arrays.fill(weights, initialWeight);
    weightVector = new SparseWeightVector(weights);
  }


  /**
    * An LTU returns two scores; one for the negative classification and one
    * for the positive classification.  By default, the score for the positive
    * classification is the result of {@link #score(Object)} minus the
    * {@link #threshold}, and the score for the negative classification is the
    * opposite of the positive classification's score.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @return Two scores as described above.
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues) {
    double s = score(exampleFeatures, exampleValues) - threshold;
    ScoreSet result = new ScoreSet();
    result.put(allowableValues[0], -s);
    result.put(allowableValues[1], s);
    return result;
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
    int index = score(f, v) >= threshold ? 1 : 0;
    return predictions.get(index);
  }


  /**
    * The default evaluation method simply computes the score for the example
    * and returns a {@link DiscretePrimitiveStringFeature} set to either the
    * second value from the label classifier's array of allowable values if
    * the score is greater than or equal to {@link #threshold} or the first
    * otherwise.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @return The computed feature (in a vector).
   **/
  public String discreteValue(int[] exampleFeatures, double[] exampleValues) {
    int index = score(exampleFeatures, exampleValues) >= threshold ? 1 : 0;
    return allowableValues[index];
  }


  /**
    * The default evaluation method simply computes the score for the example
    * and returns a {@link DiscretePrimitiveStringFeature} set to either the
    * second value from the label classifier's array of allowable values if
    * the score is greater than or equal to {@link #threshold} or the first
    * otherwise.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @return The computed feature (in a vector).
   **/
  public FeatureVector classify(int[] exampleFeatures, double[] exampleValues)
  {
    return new FeatureVector(featureValue(exampleFeatures, exampleValues));
  }


  /**
    * Computes the score for the specified example vector which will be
    * thresholded to make the binary classification.
    *
    * @param example  The example object.
    * @return The score for the given example vector.
   **/
  public double score(Object example) {
    Object[] exampleArray = getExampleArray(example, false);
    return score((int[]) exampleArray[0], (double[]) exampleArray[1]);
  }


  /**
    * Computes the score for the specified example vector which will be
    * thresholded to make the binary classification.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @return The score for the given example vector.
   **/
  public double score(int[] exampleFeatures, double[] exampleValues) {
    return weightVector.dot(exampleFeatures, exampleValues, initialWeight)
           + bias;
  }


  /**
    * Resets the weight vector to associate the default weight with all
    * features.
   **/
  public void forget() {
    super.forget();
    weightVector = weightVector.emptyClone();
    bias = initialWeight;
    setLabeler(labeler);
  }


  /**
    * If the <code>LinearThresholdUnit</code> is mistake driven, this method
    * should be overridden and used to update the internal representation when
    * a mistake is made on a positive example.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param rate  The learning rate at which the weights are updated.
   **/
  public abstract void promote(int[] exampleFeatures, double[] exampleValues,
                               double rate);


  /**
    * If the <code>LinearThresholdUnit</code> is mistake driven, this method
    * should be overridden and used to update the internal representation when
    * a mistake is made on a negative example.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param rate  The learning rate at which the weights are updated.
   **/
  public abstract void demote(int[] exampleFeatures, double[] exampleValues,
                             double rate);


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);

    if (allowableValues == null) out.writeInt(0);
    else {
      out.writeInt(allowableValues.length);
      for (int i = 0; i < allowableValues.length; ++i)
        out.writeString(allowableValues[i]);
    }

    out.writeDouble(initialWeight);
    out.writeDouble(threshold);
    out.writeDouble(learningRate);
    out.writeDouble(positiveThickness);
    out.writeDouble(negativeThickness);
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

    int N = in.readInt();
    if (N == 0) allowableValues = null;
    else {
      allowableValues = new String[N];
      for (int i = 0; i < N; ++i)
        allowableValues[i] = in.readString();
    }

    initialWeight = in.readDouble();
    threshold = in.readDouble();
    learningRate = in.readDouble();
    positiveThickness = in.readDouble();
    negativeThickness = in.readDouble();
    bias = in.readDouble();
    weightVector = SparseWeightVector.readWeightVector(in);
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone() {
    LinearThresholdUnit clone = (LinearThresholdUnit) super.clone();
    if (weightVector != null)
      clone.weightVector = (SparseWeightVector) weightVector.clone();
    return clone;
  }


  /**
    * Simply a container for all of {@link LinearThresholdUnit}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends Learner.Parameters
  {
    /**
      * The rate at which weights are updated; default
      * {@link LinearThresholdUnit#defaultLearningRate}.
     **/
    public double learningRate;
    /** The LTU's weight vector; default is an empty vector. */
    public SparseWeightVector weightVector;
    /**
      * The weight associated with a feature when first added to the vector;
      * default {@link LinearThresholdUnit#defaultInitialWeight}.
     **/
    public double initialWeight;
    /**
      * The score is compared against this value to make predictions; default
      * {@link LinearThresholdUnit#defaultThreshold}.
     **/
    public double threshold;
    /**
      * This thickness will be added to both {@link #positiveThickness} and
      * {@link #negativeThickness}; default
      * {@link LinearThresholdUnit#defaultThickness}.
     **/
    public double thickness;
    /** The thickness of the hyperplane on the positive side; default 0. */
    public double positiveThickness;
    /** The thickness of the hyperplane on the negative side; default 0. */
    public double negativeThickness;


    /** Sets all the default values. */
    public Parameters() {
      learningRate = defaultLearningRate;
      weightVector = (SparseWeightVector) defaultWeightVector.clone();
      initialWeight = defaultInitialWeight;
      threshold = defaultThreshold;
      thickness = defaultThickness;
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(Learner.Parameters p) { this(); }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      learningRate = p.learningRate;
      weightVector = p.weightVector;
      initialWeight = p.initialWeight;
      threshold = p.threshold;
      thickness = p.thickness;
      positiveThickness = p.positiveThickness;
      negativeThickness = p.negativeThickness;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((LinearThresholdUnit) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();

      if (learningRate != LinearThresholdUnit.defaultLearningRate)
        result += ", learningRate = " + learningRate;
      if (initialWeight != LinearThresholdUnit.defaultInitialWeight)
        result += ", initialWeight = " + initialWeight;
      if (threshold != LinearThresholdUnit.defaultThreshold)
        result += ", threshold = " + threshold;
      if (thickness != LinearThresholdUnit.defaultThickness)
        result += ", thickness = " + thickness;
      if (positiveThickness != 0)
        result += ", positiveThickness = " + positiveThickness;
      if (negativeThickness != 0)
        result += ", negativeThickness = " + negativeThickness;

      if (result.startsWith(", ")) result = result.substring(2);
      return result;
    }
  }
}

