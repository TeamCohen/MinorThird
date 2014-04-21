package LBJ2.learn;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;

import LBJ2.classify.Classifier;
import LBJ2.classify.DiscretePrimitiveStringFeature;
import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.ScoreSet;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.OVector;


/**
  * An implementation of the Margin Infused Relaxed Algorithm of Crammer and
  * Singer.  This is a multi-class, online learner that maintains a separate
  * weight vector for every prediction class, just as
  * {@link SparseNetworkLearner} does.  However, updates to these weight
  * vectors given an example vector <i>x</i> with label <i>y</i> are dependent
  * on each other as follows.  For each weight vector <i>w<sub>v</sub></i>
  * corresponding to a prediction value <i>v</i>, a multiplier
  * <i>t<sub>v</sub></i> is selected and used to update <i>w<sub>v</sub></i>
  * as <i>w<sub>v</sub></i> += <i>t<sub>v</sub> x</i>.  <i>t<sub>v</sub></i>
  * must be less than or equal to zero for all <i>v</i> != <i>y</i>.
  * <i>t<sub>y</sub></i> must be less than or equal to one.  MIRA selects
  * these multipliers so that they sum to 0 and so that the vector norm of all
  * updated weight vectors concatenated is as small as possible.
  *
  * <p> In this sparse implementation of the algorithm, weight vectors
  * corresponding to labels and weights for features within those vectors are
  * added as they are observed in the data.  Whenever a feature is observed
  * for the first time, its corresponding weight in any given weight vector is
  * set to a random number, which is necessary to make this algorithm work.
  * It must never be the case that all weight vectors are equal to each other,
  * or updates will stop happening.  To ensure that results are reproducible,
  * the random number generator is seeded with the same seed every time.
  *
  * <p> In addition to the observed features, each weight vector also contains
  * a bias.  For this reason, we also halucinate an extra dimension on every
  * example vector containing a feature whose strength is <i>1</i>.
  *
  * <p> It is assumed that a single discrete label feature will be produced in
  * association with each example object.  A feature taking one of the values
  * observed in that label feature will be produced by the learned classifier.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.SparseMIRA.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.SparseMIRA.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Nick Rizzolo
 **/
public class SparseMIRA extends Learner
{
  /**
    * Used to decide if two values are nearly equal to each other.
    * @see #nearlyEqualTo(double,double)
   **/
  public static final double TOLERANCE = 1e-9;


  /** A map from labels to the weight vector corresponding to that label. */
  protected OVector network;
  /** Whether or not this learner's labeler produces conjunctive features. */
  protected boolean conjunctiveLabels;


  /** This algorithm has no parameters to set! */
  public SparseMIRA() { this(""); }

  /**
    * Initializing constructor.  This constructor appears here for
    * completeness; the algorithm takes no parameters.
    *
    * @param p  The settings of all parameters.
   **/
  public SparseMIRA(Parameters p) { this("", p); }

  /**
    * This algorithm has no parameters to set!
    *
    * @param n  The name of the classifier.
   **/
  public SparseMIRA(String n) {
    super(n);
    network = new OVector();
  }

  /**
    * Initializing constructor.  This constructor appears here for
    * completeness; the algorithm takes no parameters.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparseMIRA(String n, Parameters p) { this(n); }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() { return new Parameters(); }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(Classifier l) {
    if (getClass().getName().indexOf("SparseMIRA") != -1
        && !l.getOutputType().equals("discrete")) {
      System.err.println(
          "LBJ WARNING: SparseMIRA will only work with a label classifier "
          + "that returns discrete.");
      System.err.println(
          "             The given label classifier, " + l.getClass().getName()
          + ", returns " + l.getOutputType() + ".");
    }

    super.setLabeler(l);
  }


  /**
    * Finds the optimal multiplier settings before updating the weight
    * vectors.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param exampleLabels    The example's label(s).
    * @param labelValues      The labels' values.
   **/
  public void learn(int[] exampleFeatures, double[] exampleValues,
                    int[] exampleLabels, double[] labelValues) {
    int label = exampleLabels[0];
    int N = network.size();

    if (label >= N) {
      conjunctiveLabels |= labelLexicon.lookupKey(label).isConjunctive();
      while (N++ <= label)
        network.add(new BiasedRandomWeightVector());
    }

    if (N == 1) return;

    double norm2 = FeatureVector.L2NormSquared(exampleValues) + 1;

    double[] scores = new double[N];
    boolean[] isLabel = new boolean[scores.length];

    BiasedRandomWeightVector[] w =
      new BiasedRandomWeightVector[scores.length];
    double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;

    for (int i = 0; i < N; ++i) {
      isLabel[i] = i == label;
      w[i] = (BiasedRandomWeightVector) network.get(i);
      scores[i] = w[i].dot(exampleFeatures, exampleValues) / norm2;
      min = Math.min(min, scores[i]);
      max = Math.max(max, scores[i]);
    }

    min--;
    max++;

    while (!nearlyEqualTo(min, max)) {
      double mid = (max + min) / 2;
      if (sumMultipliers(mid, scores, isLabel) <= 0) min = mid;
      else max = mid;
    }

    for (int i = 0; i < N; ++i) {
      double t = getMultiplier(min, scores[i], isLabel[i]);
      if (!nearlyEqualTo(t, 0))
        w[i].scaledAdd(exampleFeatures, exampleValues, t);
    }
  }


  /**
    * Returns the multiplier for a given weight vector update.  See Section
    * 5.1 of Crammer and Singer (2003) for a description of where this
    * computation comes from.
    *
    * @param theta    See Crammer and Singer (2003).
    * @param score    The dot product of the weight vector with the example
    *                 vector, divided by the norm of the example vector
    *                 squared.
    * @param isLabel  <code>true</code> iff this weight vector corresponds to
    *                 the example's label.
    * @return The multiplier for this weight vector's update.
   **/
  private static double getMultiplier(double theta, double score,
                                      boolean isLabel) {
    return Math.min(theta - score, isLabel ? 1 : 0);
  }


  /**
    * Finds the sum of the multipliers for a given value of theta.  See
    * Section 5.1 of Crammer and Singer (2003) for an explanation of what
    * theta is.
    *
    * @param theta    There should exist a value for this parameter that
    *                 causes this method to return zero.
    * @param scores   The dot products of the various weight vectors with the
    *                 example vector, divided by the norm of the example
    *                 vector squared.
    * @param isLabel  <code>true</code> at element <code>i</code> iff
    *                 <code>scores[i]</code> is the dot product involving the
    *                 weight vector corresponding to the example's label.
    * @return The sum of the multipliers assuming the given value of
    *         <code>theta</code>.
   **/
  private static double sumMultipliers(double theta, double[] scores,
                                       boolean[] isLabel) {
    double result = 0;
    for (int i = 0; i < scores.length; ++i)
      result += getMultiplier(theta, scores[i], isLabel[i]);
    return result;
  }


  /**
    * Determines if <code>a</code> is nearly equal to <code>b</code> based on
    * the value of the {@link #TOLERANCE} member variable.
    *
    * @param a  The first value.
    * @param b  The second value.
    * @return True if they are nearly equal, false otherwise.
   **/
  private static boolean nearlyEqualTo(double a, double b) {
    return -TOLERANCE < a - b && a - b < TOLERANCE;
  }


  /** Clears the network. */
  public void forget() {
    super.forget();
    network = new OVector();
  }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.  These scores are just the dot product of each weight vector
    * with the example vector.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues) {
    ScoreSet result = new ScoreSet();
    int N = network.size();

    for (int l = 0; l < N; l++) {
      double score =
        ((BiasedRandomWeightVector) network.get(l))
        .dot(exampleFeatures, exampleValues);
      result.put(labelLexicon.lookupKey(l).getStringValue(), score);
    }

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
    double bestScore = Double.NEGATIVE_INFINITY;
    int bestLabel = -1;
    int N = network.size();

    for (int l = 0; l < N; l++) {
      double score = ((BiasedRandomWeightVector) network.get(l)).dot(f, v);

      if (score > bestScore) {
        bestLabel = l;
        bestScore = score;
      }
    }

    if (bestLabel == -1) return null;
    return predictions.get(bestLabel);
  }


  /**
    * This implementation uses a winner-take-all comparison of the individual
    * weight vectors' dot products.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The discrete value of the best prediction.
   **/
  public String discreteValue(int[] exampleFeatures, double[] exampleValues) {
    return featureValue(exampleFeatures, exampleValues).getStringValue();
  }


  /**
    * This implementation uses a winner-take-all comparison of the individual
    * weight vectors' dot products.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return A single feature with the winning weight vector's associated
    *         value.
   **/
  public FeatureVector classify(int[] exampleFeatures, double[] exampleValues)
  {
    return new FeatureVector(featureValue(exampleFeatures, exampleValues));
  }


  /**
    * Using this method, the winner-take-all competition is narrowed to
    * involve only those labels contained in the specified list.  The list
    * must contain only <code>String</code>s.
    *
    * @param example    The example object.
    * @param candidates A list of the only labels the example may take.
    * @return The prediction as a feature or <code>null</code> if the network
    *         did not contain any of the specified labels.
   **/
  public Feature valueOf(Object example, Collection candidates) {
    Object[] array = getExampleArray(example, false);
    return valueOf((int[]) array[0], (double[]) array[1], candidates);
  }


  /**
    * Using this method, the winner-take-all competition is narrowed to
    * involve only those labels contained in the specified list.  The list
    * must contain only <code>String</code>s.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param candidates       A list of the only labels the example may take.
    * @return The prediction as a feature or <code>null</code> if the network
    *         did not contain any of the specified labels.
   **/
  public Feature valueOf(int[] exampleFeatures, double[] exampleValues,
                         Collection candidates) {
    double bestScore = Double.NEGATIVE_INFINITY;
    int bestValue = -1;
    Iterator I = candidates.iterator();

    if (I.hasNext()) {
      if (conjunctiveLabels)
        return conjunctiveValueOf(exampleFeatures, exampleValues, I);

      while (I.hasNext()) {
        double score = Double.NEGATIVE_INFINITY;
        String label = (String) I.next();
        Feature f =
          new DiscretePrimitiveStringFeature(
                labeler.containingPackage, labeler.name, "", label,
                labeler.valueIndexOf(label),
                (short) labeler.allowableValues().length);

        int key = -1;
        if (labelLexicon.contains(f)) {
          key = labelLexicon.lookup(f);
          score = ((BiasedRandomWeightVector) network.get(key))
                  .dot(exampleFeatures, exampleValues);
        }

        if (score > bestScore) {
          bestValue = key;
          bestScore = score;
        }
      }
    }
    else {
      int N = network.size();
      for (int l = 0; l < N; l++) {
        double score =
          ((BiasedRandomWeightVector) network.get(l))
          .dot(exampleFeatures, exampleValues);

        if (score > bestScore) {
          bestValue = l;
          bestScore = score;
        }
      }
    }

    return bestValue == -1 ? null : predictions.get(bestValue);
  }


  /**
    * This method is a surrogate for
    * {@link #valueOf(int[],double[],Collection)} when the labeler is known to
    * produce conjunctive features.  It is necessary because when given a
    * string label from the collection, we will not know how to construct the
    * appropriate conjunctive feature key for lookup in the label lexicon.
    * So, we must go through each feature in the label lexicon and use
    * {@link LBJ2.classify.Feature#valueEquals(String)}.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param I                An iterator over the set of labels to choose
    *                         from.
    * @return The prediction as a feature or <code>null</code> if the network
    *         did not contain any of the specified labels.
   **/
  protected Feature conjunctiveValueOf(
      int[] exampleFeatures, double[] exampleValues, Iterator I) {
    double bestScore = Double.NEGATIVE_INFINITY;
    int bestValue = -1;
    int N = network.size();

    while (I.hasNext()) {
      String label = (String) I.next();

      for (int i = 0; i < N; ++i) {
        LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
        if (ltu == null || !predictions.get(i).valueEquals(label))
          continue;
        double score = ltu.score(exampleFeatures, exampleValues);
        if (score > bestScore) {
          bestScore = score;
          bestValue = i;
        }
        break;
      }
    }

    return bestValue == -1 ? null : predictions.get(bestValue);
  }


  /**
    * Returns scores for only those labels in the given collection.  If the
    * given collection is empty, scores for all labels will be returned.  If
    * there is no {@link BiasedRandomWeightVector} associated with a given
    * label from the collection, that label's score in the returned
    * {@link ScoreSet} will be set to <code>Double.NEGATIVE_INFINITY</code>.
    *
    * <p> The elements of <code>candidates</code> must all be
    * <code>String</code>s.
    *
    * @param example    The example object.
    * @param candidates A list of the only labels the example may take.
    * @return Scores for only those labels in <code>candidates</code>.
   **/
  public ScoreSet scores(Object example, Collection candidates) {
    Object[] array = getExampleArray(example, false);
    return scores((int[]) array[0], (double[]) array[1], candidates);
  }


  /**
    * Returns scores for only those labels in the given collection.  If the
    * given collection is empty, scores for all labels will be returned.  If
    * there is no {@link BiasedRandomWeightVector} associated with a given
    * label from the collection, that label's score in the returned
    * {@link ScoreSet} will be set to <code>Double.NEGATIVE_INFINITY</code>.
    *
    * <p> The elements of <code>candidates</code> must all be
    * <code>String</code>s.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param candidates       A list of the only labels the example may take.
    * @return Scores for only those labels in <code>candidates</code>.
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues,
                         Collection candidates) {
    ScoreSet result = new ScoreSet();
    Iterator I = candidates.iterator();

    if (I.hasNext()) {
      if (conjunctiveLabels)
        return conjunctiveScores(exampleFeatures, exampleValues, I);

      while (I.hasNext()) {
        double score = Double.NEGATIVE_INFINITY;
        String label = (String) I.next();
        Feature f =
          new DiscretePrimitiveStringFeature(
                labeler.containingPackage, labeler.name, "", label,
                labeler.valueIndexOf(label),
                (short) labeler.allowableValues().length);

        if (labelLexicon.contains(f)) {
          int key = labelLexicon.lookup(f);
          score = ((BiasedRandomWeightVector) network.get(key))
                  .dot(exampleFeatures, exampleValues);
          result.put(label.toString(), score);
        }
      }
    }
    else {
      int N = network.size();
      for (int l = 0; l < N; l++) {
        double score =
          ((BiasedRandomWeightVector) network.get(l))
          .dot(exampleFeatures, exampleValues);
        result.put(labelLexicon.lookupKey(l).getStringValue(), score);
      }
    }

    return result;
  }


  /**
    * This method is a surrogate for
    * {@link #scores(int[],double[],Collection)} when the labeler is known to
    * produce conjunctive features.  It is necessary because when given a
    * string label from the collection, we will not know how to construct the
    * appropriate conjunctive feature key for lookup in the label lexicon.
    * So, we must go through each feature in the label lexicon and use
    * {@link LBJ2.classify.Feature#valueEquals(String)}.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param I                An iterator over the set of labels to choose
    *                         from.
    * @return The label chosen by this classifier or <code>null</code> if the
    *         network did not contain any of the specified labels.
   **/
  protected ScoreSet conjunctiveScores(int[] exampleFeatures,
                                       double[] exampleValues, Iterator I) {
    ScoreSet result = new ScoreSet();
    int N = network.size();

    while (I.hasNext()) {
      String label = (String) I.next();

      for (int i = 0; i < N; ++i) {
        LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
        if (ltu == null || !labelLexicon.lookupKey(i).valueEquals(label))
          continue;
        double score = ltu.score(exampleFeatures, exampleValues);
        result.put(label.toString(), score);
        break;
      }
    }

    return result;
  }


  /**
    * Writes the algorithm's internal representation as text.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    int N = network.size();
    for (int i = 0; i < N; ++i) {
      out.println("label: " + predictions.get(i).getStringValue());
      ((BiasedRandomWeightVector) network.get(i)).write(out, lexicon);
    }

    out.println("End of SparseMIRA");
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    int N = network.size();
    out.writeInt(N);
    for (int i = 0; i < N; ++i)
      ((BiasedRandomWeightVector) network.get(i)).write(out);
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
    network = new OVector(N);
    for (int i = 0; i < N; ++i)
      network.add(SparseWeightVector.readWeightVector(in));
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone() {
    SparseMIRA clone = null;

    try { clone = (SparseMIRA) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning SparseMIRA: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    int N = network.size();
    clone.network = new OVector(N);
    for (int i = 0; i < N; ++i)
      clone.network.add(((BiasedRandomWeightVector) network.get(i)).clone());

    return clone;
  }


  /**
    * Simply a container for all of {@link SparseMIRA}'s
    * configurable parameters.  This class appears here for completeness; the
    * algorithm has no parameters to set.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends Learner.Parameters
  {
    /** Sets all the default values. */
    public Parameters() { }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(Learner.Parameters p) { super(p); }


    /** Copy constructor. */
    public Parameters(Parameters p) { super(p); }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) { }
  }
}

