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
  * A <code>SparseNetworkLearner</code> uses multiple
  * {@link LinearThresholdUnit}s to make a multi-class classification.
  * Any {@link LinearThresholdUnit} may be used, so long as it implements its
  * <code>clone()</code> method and a public constructor that takes no
  * arguments.
  *
  * <p> It is assumed that a single discrete label feature will be produced in
  * association with each example object.  A feature taking one of the values
  * observed in that label feature will be produced by the learned classifier.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.SparseNetworkLearner.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.SparseNetworkLearner.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Nick Rizzolo
 **/
public class SparseNetworkLearner extends Learner
{
  /** Default for {@link #baseLTU}. */
  public static final LinearThresholdUnit defaultBaseLTU =
    new SparseAveragedPerceptron();


  /**
    * The underlying algorithm used to learn each class separately as a binary
    * classifier; default {@link #defaultBaseLTU}.
   **/
  protected LinearThresholdUnit baseLTU;
  /**
    * A collection of the linear threshold units used to learn each label,
    * indexed by the label.
   **/
  protected OVector network;
  /** The total number of examples in the training data, or 0 if unknown. */
  protected int numExamples;
  /**
    * The total number of distinct features in the training data, or 0 if
    * unknown.
   **/
  protected int numFeatures;
  /** Whether or not this learner's labeler produces conjunctive features. */
  protected boolean conjunctiveLabels;


  /**
    * Instantiates this multi-class learner with the default learning
    * algorithm: {@link #defaultBaseLTU}.
   **/
  public SparseNetworkLearner() { this(""); }

  /**
    * Instantiates this multi-class learner using the specified algorithm to
    * learn each class separately as a binary classifier.  This constructor
    * will normally only be called by the compiler.
    *
    * @param ltu  The linear threshold unit used to learn binary classifiers.
   **/
  public SparseNetworkLearner(LinearThresholdUnit ltu) { this("", ltu); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseNetworkLearner.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public SparseNetworkLearner(Parameters p) { this("", p); }

  /**
    * Instantiates this multi-class learner with the default learning
    * algorithm: {@link #defaultBaseLTU}.
    *
    * @param n  The name of the classifier.
   **/
  public SparseNetworkLearner(String n) { this(n, new Parameters()); }

  /**
    * Instantiates this multi-class learner using the specified algorithm to
    * learn each class separately as a binary classifier.
    *
    * @param n    The name of the classifier.
    * @param ltu  The linear threshold unit used to learn binary classifiers.
   **/
  public SparseNetworkLearner(String n, LinearThresholdUnit ltu) {
    super(n);
    Parameters p = new Parameters();
    p.baseLTU = ltu;
    setParameters(p);
    network = new OVector();
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseNetworkLearner.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparseNetworkLearner(String n, Parameters p) {
    super(n);
    setParameters(p);
    network = new OVector();
  }


  /**
    * Sets the values of parameters that control the behavior of this learning
    * algorithm.
    *
    * @param p  The parameters.
   **/
  public void setParameters(Parameters p) {
    if (!p.baseLTU.getOutputType().equals("discrete")) {
      System.err.println(
          "LBJ WARNING: SparseNetworkLearner will only work with a "
          + "LinearThresholdUnit that returns discrete.");
      System.err.println(
          "             The given LTU, " + p.baseLTU.getClass().getName()
          + ", returns " + p.baseLTU.getOutputType() + ".");
    }

    setLTU(p.baseLTU);
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p = new Parameters(super.getParameters());
    p.baseLTU = baseLTU;
    return p;
  }


  /**
    * Sets the <code>baseLTU</code> variable.  This method will <i>not</i>
    * have any effect on the LTUs that already exist in the network.  However,
    * new LTUs created after this method is executed will be of the same type
    * as the object specified.
    *
    * @param ltu  The new LTU.
   **/
  public void setLTU(LinearThresholdUnit ltu) {
    baseLTU = ltu;
    baseLTU.name = name + "$baseLTU";
  }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(Classifier l) {
    if (getClass().getName().indexOf("SparseNetworkLearner") != -1
        && !l.getOutputType().equals("discrete")) {
      System.err.println(
          "LBJ WARNING: SparseNetworkLearner will only work with a "
          + "label classifier that returns discrete.");
      System.err.println(
          "             The given label classifier, " + l.getClass().getName()
          + ", returns " + l.getOutputType() + ".");
    }

    super.setLabeler(l);
  }


  /**
    * Sets the extractor.
    *
    * @param e  A feature extracting classifier.
   **/
  public void setExtractor(Classifier e) {
    super.setExtractor(e);
    baseLTU.setExtractor(e);
    int N = network.size();

    for (int i = 0; i < N; ++i)
      ((LinearThresholdUnit) network.get(i)).setExtractor(e);
  }


  /**
    * Each example is treated as a positive example for the linear threshold
    * unit associated with the label's value that is active for the example
    * and as a negative example for all other linear threshold units in the
    * network.
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

    if (label >= N || network.get(label) == null) {
      conjunctiveLabels |= labelLexicon.lookupKey(label).isConjunctive();

      LinearThresholdUnit ltu = (LinearThresholdUnit) baseLTU.clone();
      ltu.initialize(numExamples, numFeatures);
      network.set(label, ltu);
      N = label + 1;
    }

    int[] l = new int[1];
    for (int i = 0; i < N; ++i) {
      LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
      if (ltu == null) continue;

      l[0] = (i == label) ? 1 : 0;
      ltu.learn(exampleFeatures, exampleValues, l, labelValues);
    }
  }


  /** Simply calls <code>doneLearning()</code> on every LTU in the network. */
  public void doneLearning() {
    super.doneLearning();
    int N = network.size();
    for (int i = 0; i < N; ++i) {
      LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
      if (ltu == null) continue;
      ltu.doneLearning();
    }
  }


  /** Sets the number of examples and features. */
  public void initialize(int ne, int nf) {
    numExamples = ne;
    numFeatures = nf;
  }


  /** Simply calls {@link LinearThresholdUnit#doneWithRound()} on every
      LTU in the network. */
  public void doneWithRound() {
    super.doneWithRound();
    int N = network.size();
    for (int i = 0; i < N; ++i) {
      LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
      if (ltu == null) continue;
      ltu.doneWithRound();
    }
  }


  /** Clears the network. */
  public void forget() {
    super.forget();
    network = new OVector();
  }


  /**
    * Returns scores for only those labels in the given collection.  If the
    * given collection is empty, scores for all labels will be returned.  If
    * there is no {@link LinearThresholdUnit} associated with a given label
    * from the collection, that label's score in the returned {@link ScoreSet}
    * will be set to <code>Double.NEGATIVE_INFINITY</code>.
    *
    * <p> The elements of <code>candidates</code> must all be
    * <code>String</code>s.
    *
    * @param example    The example object.
    * @param candidates A list of the only labels the example may take.
    * @return Scores for only those labels in <code>candidates</code>.
   **/
  public ScoreSet scores(Object example, Collection candidates) {
    Object[] exampleArray = getExampleArray(example, false);
    return
      scores((int[]) exampleArray[0], (double[]) exampleArray[1], candidates);
  }


  /**
    * Returns scores for only those labels in the given collection.  If the
    * given collection is empty, scores for all labels will be returned.  If
    * there is no {@link LinearThresholdUnit} associated with a given label
    * from the collection, that label's score in the returned {@link ScoreSet}
    * will be set to <code>Double.NEGATIVE_INFINITY</code>.
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
        String label = (String) I.next();
        Feature f =
          new DiscretePrimitiveStringFeature(
                labeler.containingPackage, labeler.name, "", label,
                labeler.valueIndexOf(label),
                (short) labeler.allowableValues().length);

        if (labelLexicon.contains(f)) {
          int key = labelLexicon.lookup(f);
          LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(key);
          if (ltu != null)
            result.put(label.toString(),
                       ltu.score(exampleFeatures, exampleValues)
                       - ltu.getThreshold());
        }
      }
    }
    else {
      int N = network.size();
      for (int l = 0; l < N; ++l) {
        LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(l);
        if (ltu == null) continue;
        result.put(labelLexicon.lookupKey(l).getStringValue(),
                   ltu.score(exampleFeatures, exampleValues)
                   - ltu.getThreshold());
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
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.  These scores are just the scores of each LTU's positive
    * classification as produced by
    * <code>LinearThresholdUnit.scores(Object)</code>.
    *
    * @see   LinearThresholdUnit#scores(Object)
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The set of scores produced by the LTUs
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues) {
    ScoreSet result = new ScoreSet();
    int N = network.size();

    for (int l = 0; l < N; l++) {
      LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(l);
      if (ltu == null) continue;

      result.put(labelLexicon.lookupKey(l).getStringValue(),
                 ltu.score(exampleFeatures, exampleValues)
                 - ltu.getThreshold());
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
    int bestValue = -1;
    int N = network.size();

    for (int l = 0; l < N; l++) {
      LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(l);
      if (ltu == null) continue;
      double score = ltu.score(f, v);

      if (score > bestScore) {
        bestValue = l;
        bestScore = score;
      }
    }

    return bestValue == -1 ? null : predictions.get(bestValue);
  }


  /**
    * This implementation uses a winner-take-all comparison of the outputs
    * from the individual linear threshold units' score methods.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return A single value with the winning linear threshold unit's
    *         associated value.
   **/
  public String discreteValue(int[] exampleFeatures, double[] exampleValues) {
    return featureValue(exampleFeatures, exampleValues).getStringValue();
  }


  /**
    * This implementation uses a winner-take-all comparison of the outputs
    * from the individual linear threshold units' score methods.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return A single feature with the winning linear threshold unit's
    *         associated value.
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
    Object[] exampleArray = getExampleArray(example, false);
    return
      valueOf((int[]) exampleArray[0], (double[]) exampleArray[1],
              candidates);
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
    Iterator cI = candidates.iterator();

    if (cI.hasNext()) {
      if (conjunctiveLabels)
        return conjunctiveValueOf(exampleFeatures, exampleValues, cI);

      while (cI.hasNext()) {
        double score = Double.NEGATIVE_INFINITY;
        String label = (String) cI.next();

        Feature f =
          new DiscretePrimitiveStringFeature(
                labeler.containingPackage, labeler.name, "", label,
                labeler.valueIndexOf(label),
                (short) labeler.allowableValues().length);

        int key = -1;
        if (labelLexicon.contains(f)) {
          key = labelLexicon.lookup(f);
          LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(key);
          if (ltu != null) score = ltu.score(exampleFeatures, exampleValues);
        }

        if (score > bestScore) {
          bestValue = key;
          bestScore = score;
        }
      }
    }
    else {
      int N = network.size();
      for (int i = 0; i < N; ++i) {
        LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
        if (ltu == null) continue;

        double score = ltu.score(exampleFeatures, exampleValues);

        if (score > bestScore) {
          bestValue = i;
          bestScore = score;
        }
      }
    }

    return predictions.get(bestValue);
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
    * @return The label chosen by this classifier or <code>null</code> if the
    *         network did not contain any of the specified labels.
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
        if (ltu == null || !labelLexicon.lookupKey(i).valueEquals(label))
          continue;
        double score = ltu.score(exampleFeatures, exampleValues);
        if (score > bestScore) {
          bestScore = score;
          bestValue = i;
        }
        break;
      }
    }

    return predictions.get(bestValue);
  }


  /**
    * Writes the algorithm's internal representation as text.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    out.println(baseLTU.getClass().getName());
    baseLTU.write(out);
    int N = network.size();

    for (int i = 0; i < N; ++i) {
      LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
      if (ltu == null) continue;
      out.println("label: " + labelLexicon.lookupKey(i).getStringValue());
      ltu.setLexicon(lexicon);
      ltu.write(out);
      ltu.setLexicon(null);
    }

    out.println("End of SparseNetworkLearner");
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    baseLTU.write(out);
    out.writeBoolean(conjunctiveLabels);
    int N = network.size();
    out.writeInt(N);

    for (int i = 0; i < N; ++i) {
      LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
      if (ltu == null) out.writeString(null);
      else ltu.write(out);
    }
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
    baseLTU = (LinearThresholdUnit) Learner.readLearner(in);
    conjunctiveLabels = in.readBoolean();
    int N = in.readInt();
    network = new OVector(N);
    for (int i = 0; i < N; ++i)
      network.add(Learner.readLearner(in));
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone() {
    SparseNetworkLearner clone = null;
    try { clone = (SparseNetworkLearner) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning SparseNetworkLearner: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    clone.baseLTU = (LinearThresholdUnit) baseLTU.clone();
    int N = network.size();
    clone.network = new OVector(N);

    for (int i = 0; i < N; ++i) {
      LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
      if (ltu == null) clone.network.add(null);
      else clone.network.add(ltu.clone());
    }

    return clone;
  }


  /**
    * Simply a container for all of {@link SparseNetworkLearner}'s
    * configurable parameters.  Using instances of this class should make code
    * more readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends Learner.Parameters
  {
    /**
      * The underlying algorithm used to learn each class separately as a
      * binary classifier; default
      * {@link SparseNetworkLearner#defaultBaseLTU}.
     **/
    public LinearThresholdUnit baseLTU;


    /** Sets all the default values. */
    public Parameters() {
      baseLTU = (LinearThresholdUnit) defaultBaseLTU.clone();
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(Learner.Parameters p) {
      super(p);
      baseLTU = (LinearThresholdUnit) defaultBaseLTU.clone();
    }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      super(p);
      baseLTU = p.baseLTU;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((SparseNetworkLearner) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String name = baseLTU.getClass().getName();
      name = name.substring(name.lastIndexOf('.') + 1);
      return name + ": " + baseLTU.getParameters().nonDefaultString();
    }
  }
}

