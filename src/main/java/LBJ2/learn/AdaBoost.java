package LBJ2.learn;

import java.io.PrintStream;
import java.util.Arrays;

import LBJ2.classify.Classifier;
import LBJ2.classify.DiscretePrimitiveStringFeature;
import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.ScoreSet;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.OVector;


/**
  * Implementation of the AdaBoost binary classification learning algorithm.
  * This implementation samples from its internal distribution, giving the
  * weak learner a new set of examples that the weak learner assumes are
  * weighted equally.
  *
  * <p> Assumptions:
  * <ol>
  *   <li> The weak learner is cloneable.
  *   <li> The weak learner is specified with the same values list as this
  *        learner.
  *   <li> The weak learning algorithm is trained on objects that are each
  *        given a single label feature.
  * </ol>
  *
  * @author Nick Rizzolo
 **/
public class AdaBoost extends Learner
{
  /** Default for {@link #weakLearner}. */
  public static final Learner defaultWeakLearner =
    new SparseAveragedPerceptron();
  /** Default for {@link #rounds}. */
  public static final int defaultRounds = 10;


  /** The weak learning algorithm to be boosted. */
  protected Learner weakLearner;
  /** The number of times the weak learner will be called. */
  protected int rounds;
  /** Will be filled with trained copies of the weak learner. */
  protected Learner[] weakLearners;
  /** Parameters associated with the trained copies of the weak learner. */
  protected double[] alpha;
  /** All the examples observed by this learner during training. */
  protected OVector allExamples;
  /** The label producing classifier's allowable values. */
  protected String[] allowableValues;


  /** Instantiates member variables. */
  public AdaBoost() { this(""); }

  /**
    * Instantiates member variables.
    *
    * @param w  The weak learning algorithm.
   **/
  public AdaBoost(Learner w) { this("", w); }

  /**
    * Instantiates member variables.
    *
    * @param r  The number of rounds of boosting.
   **/
  public AdaBoost(int r) { this("", r); }

  /**
    * Instantiates member variables.
    *
    * @param w  The weak learning algorithm.
    * @param r  The number of rounds of boosting.
   **/
  public AdaBoost(Learner w, int r) { this("", w, r); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link AdaBoost.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public AdaBoost(Parameters p) { this("", p); }

  /**
    * Instantiates member variables.
    *
    * @param n  The name of the classifier.
   **/
  public AdaBoost(String n) { this(n, new Parameters()); }

  /**
    * Instantiates member variables.
    *
    * @param n  The name of the classifier.
    * @param w  The weak learning algorithm.
   **/
  public AdaBoost(String n, Learner w) {
    this(n, w, defaultRounds);
  }

  /**
    * Instantiates member variables.
    *
    * @param n  The name of the classifier.
    * @param r  The number of rounds of boosting.
   **/
  public AdaBoost(String n, int r) {
    this(n, defaultWeakLearner, r);
  }

  /**
    * Instantiates member variables.
    *
    * @param n  The name of the classifier.
    * @param w  The weak learning algorithm.
    * @param r  The number of rounds of boosting.
   **/
  public AdaBoost(String n, Learner w, int r) {
    super(n);
    weakLearner = w;
    rounds = r;
    allExamples = new OVector();
    allowableValues = new String[]{ "*", "*" };
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link AdaBoost.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public AdaBoost(String n, Parameters p) {
    super(n);
    setParameters(p);
    allExamples = new OVector();
    allowableValues = new String[]{ "*", "*" };
  }


  /**
    * Sets the values of parameters that control the behavior of this learning
    * algorithm.
    *
    * @param p  The parameters.
   **/
  public void setParameters(Parameters p) {
    weakLearner = p.weakLearner;
    rounds = p.rounds;
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p = new Parameters(super.getParameters());
    p.weakLearner = weakLearner;
    p.rounds = rounds;
    return p;
  }


  /**
    * Returns the array of allowable values that a feature returned by this
    * classifier may take.
    *
    * @return The allowable values of this learner's labeler, or an array of
    *         length zero if the labeler has not yet been established or does
    *         not specify allowable values.
   **/
  public String[] allowableValues() { return allowableValues; }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(Classifier l) {
    if (l == null || l.allowableValues().length != 2) {
      System.err.println(
          "Error: " + name
          + ": An LTU must be given a single binary label classifier.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    super.setLabeler(l);
    allowableValues = l.allowableValues();
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
    createPrediction(0);
    createPrediction(1);
  }


  /**
    * Initializes the weight vector array to the size of the supplied number
    * of features.
   **/
  public void initialize(int numExamples, int numFeatures) {
    allExamples = new OVector(numExamples);
  }


  /**
    * This method adds the example object to the array storing the training
    * examples.
    *
    * <p> Note that learning does not actually take place until
    * {@link #doneLearning()} is called.
    *
    * @param example  The example object.
   **/
  public void learn(Object example) {
    allExamples.add(getExampleArray(example));
  }


  /**
    * This method adds the example object to the array storing the training
    * examples.
    *
    * <p> Note that learning does not actually take place until
    * {@link #doneLearning()} is called.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param exampleLabels    The example's label(s).
    * @param labelValues      The labels' values.
   **/
  public void learn(int[] exampleFeatures, double[] exampleValues,
                    int[] exampleLabels, double[] labelValues) {
    allExamples.add(new Object[]{ exampleFeatures, exampleValues,
                                  exampleLabels, labelValues });
  }


  /**
    * Performs learning on the examples stored in {@link #allExamples}, if
    * they exist; otherwise do nothing.
   **/
  public void doneLearning() {
    int m = allExamples.size();
    if (m == 0) return;

    double[] D = new double[m];
    Arrays.fill(D, 1 / (double) m);

    weakLearners = new Learner[rounds];
    alpha = new double[rounds];

    for (int i = 0; i < rounds; ++i) {
      Object[][] sample = new Object[m][];
      for (int j = 0; j < m; ++j) {
        double p = Math.random();
        double sum = 0;
        int k = 0;
        while (sum <= p) sum += D[k++];
        sample[j] = (Object[]) allExamples.get(k - 1);
      }

      weakLearners[i] = (Learner) weakLearner.clone();
      weakLearners[i].setLabelLexicon(labelLexicon);
      weakLearners[i].learn((Object[]) sample);
      weakLearners[i].doneLearning();

      int totalCorrect = 0;
      boolean[] correct = new boolean[m];
      for (int j = 0; j < m; ++j) {
        String label =
          labelLexicon.lookupKey(((int[]) sample[j][2])[0]).getStringValue();
        String prediction =
          weakLearners[i].featureValue(sample[j]).getStringValue();
        correct[j] = label.equals(prediction);
        if (correct[j]) totalCorrect++;
      }

      double x = totalCorrect / (double) (m - totalCorrect);
      alpha[i] = Math.log(x) / 2.0;

      if (i + 1 < rounds) {
        double multiplier = Math.sqrt(x);
        double sum = 0;

        for (int j = 0; j < m; ++j) {
          if (correct[j]) D[j] /= multiplier;
          else D[j] *= multiplier;
          sum += D[j];
        }

        for (int j = 0; j < m; ++j) D[j] /= sum;
      }
    }

    allExamples = null;
  }


  /**
    * Clears <code>weakLearners</code> and <code>alpha</code>, although this
    * is not necessary since <code>learn(Object[])</code> will overwrite them
    * fresh each time it is called.
   **/
  public void forget() {
    super.forget();
    weakLearners = null;
    alpha = null;
    allExamples = new OVector();
  }


  /**
    * Computes the scores corresponding to the two prediction values for the
    * given example.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The scores corresponding to the values in the
    *         {@link #labelLexicon} in an array with the same indexes.
   **/
  protected double[] sumAlphas(int[] exampleFeatures, double[] exampleValues)
  {
    double[] sums = new double[2];

    for (int i = 0; i < rounds; ++i) {
      int v =
        weakLearners[i].featureValue(exampleFeatures, exampleValues)
                       .getValueIndex();
      sums[v] += alpha[i];
    }

    return sums;
  }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The accumulated alpha values of weak learners that predicted the
    *         associated classification value.
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues) {
    double[] scores = sumAlphas(exampleFeatures, exampleValues);
    String[] values =
      new String[]{ labelLexicon.lookupKey(0).getStringValue(),
                    labelLexicon.lookupKey(1).getStringValue() };
    return new ScoreSet(values, scores);
  }


  /**
    * Returns the classification of the given example as a single feature
    * instead of a {@link FeatureVector}.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The classification of the example as a feature.
   **/
  public Feature featureValue(int[] exampleFeatures, double[] exampleValues) {
    double[] scores = sumAlphas(exampleFeatures, exampleValues);
    return predictions.get(scores[0] > scores[1] ? 0 : 1);
  }


  /**
    * This method uses the trained parameters to make a binary decision about
    * an example object.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The decision value.
   **/
  public String discreteValue(int[] exampleFeatures, double[] exampleValues) {
    double[] scores = sumAlphas(exampleFeatures, exampleValues);
    return allowableValues[scores[0] > scores[1] ? 0 : 1];
  }


  /**
    * This method uses the trained parameters to make a binary decision about
    * an example object.
    *
    * @param exampleFeatures  The example features.
    * @param exampleValues    The example values.
    * @return A binary <code>DiscreteFeature</code>.
   **/
  public FeatureVector classify(int[] exampleFeatures, double[] exampleValues)
  {
    return new FeatureVector(featureValue(exampleFeatures, exampleValues));
  }


  /**
    * Writes this algorithm's internal representation as text.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    out.println(name);

    if (rounds > 0) {
      out.print(alpha[0]);
      for (int i = 1; i < rounds; ++i) out.print(", " + alpha[i]);
      out.println();
    }
    else out.println("---");

    out.println(weakLearner.getClass().getName());
    weakLearner.write(out);
    for (int i = 0; i < rounds; ++i) {
      weakLearners[i].setLexicon(lexicon);
      weakLearners[i].write(out);
      weakLearners[i].setLexicon(null);
    }
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    weakLearner.write(out);
    out.writeInt(rounds);
    for (int i = 0; i < rounds; ++i) weakLearners[i].write(out);
    for (int i = 0; i < rounds; ++i) out.writeDouble(alpha[i]);
    out.writeString(allowableValues[0]);
    out.writeString(allowableValues[1]);
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
    weakLearner = Learner.readLearner(in);
    rounds = in.readInt();
    for (int i = 0; i < rounds; ++i)
      weakLearners[i] = Learner.readLearner(in);
    for (int i = 0; i < rounds; ++i) alpha[i] = in.readDouble();
    allowableValues = new String[2];
    allowableValues[0] = in.readString();
    allowableValues[1] = in.readString();
  }


  /**
    * A container for all of {@link AdaBoost}'s configurable parameters.
    * Using instances of this class should make code more readable and
    * constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends Learner.Parameters
  {
    /** The weak learning algorithm to be boosted. */
    protected Learner weakLearner;
    /** The number of times the weak learner will be called. */
    protected int rounds;


    /** Sets all the default values. */
    public Parameters() {
      weakLearner = (Learner) defaultWeakLearner.clone();
      rounds = defaultRounds;
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(Learner.Parameters p) {
      super(p);
      weakLearner = (Learner) defaultWeakLearner.clone();
      rounds = defaultRounds;
    }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      super(p);
      weakLearner = p.weakLearner;
      rounds = p.rounds;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((AdaBoost) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();
      if (rounds != AdaBoost.defaultRounds)
        result += ", rounds = " + rounds;
      if (result.startsWith(", ")) result = result.substring(2);
      return result;
    }
  }
}

