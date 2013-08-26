package LBJ2.learn;

import java.io.PrintStream;

import LBJ2.classify.Classifier;
import LBJ2.classify.DiscretePrimitiveStringFeature;
import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.ScoreSet;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.FVector;
import LBJ2.util.OVector;
import LBJ2.util.Sort;


/**
  * A <code>MuxLearner</code> uses one of many <code>Learner</code>s indexed
  * by the first feature in an example to produce a classification.  During
  * training, the features produced by the first child classifier of this
  * classifier's composite generator feature extractor are taken to determine
  * which <code>Learner</code>s will learn from the training object.  For any
  * given example, there must be one <code>Feature</code> produced by the
  * labeler for each <code>Feature</code> produced by the first child
  * classifier.  If this classifier's feature extractor is not a composite
  * generator, the first feature it produces is the only one taken.
  *
  * <p> It is assumed that the <code>Learner</code> being multiplexed expects
  * a single label feature on each training example, and that the feature(s)
  * used to do the multiplexing are <code>DiscreteFeature</code>(s).
  * Furthermore, if this classifier's feature extractor is a composite
  * generator, it must produce the same number of features as this
  * classifier's labeler, and they must correspond to each other in the order
  * produced.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.MuxLearner.Parameters Parameters} as input.  The
  * documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.MuxLearner.Parameters Parameters} class indicates the
  * default value of the parameter when using the latter type of constructor.
  *
  * @author Nick Rizzolo
 **/
public class MuxLearner extends Learner
{
  /** Default for {@link #baseLearner}. */
  public static final Learner defaultBaseLearner =
    new SparseNetworkLearner(new SparseAveragedPerceptron());
  /** Default for {@link #defaultPrediction}. */
  public static final String defaultDefaultPrediction = null;


  /**
    * Instances of this learning algorithm will be multiplexed; default
    * <code>null</code>.
   **/
  protected Learner baseLearner;
  /** A map from feature values to learners. */
  protected OVector network;
  /**
    * This string is returned during testing when the multiplexed
    * <code>Learner</code> doesn't exist; default
    * {@link #defaultDefaultPrediction}.
   **/
  protected String defaultPrediction;
  /** A feature whose value is {@link #defaultPrediction}. */
  protected Feature defaultFeature;


  /** For the LBJ compiler; not for use by the LBJ user. */
  public MuxLearner() { }

  /**
    * Instantiates this multiplexed learner using the specified base learning
    * algorithm.  This constructor will normally only be called by the
    * compiler.
    *
    * @param base Instances of this learner will be multiplexed.
   **/
  public MuxLearner(Learner base) { this("", base); }

  /**
    * Instantiates this multiplexed learner using the specified base learning
    * algorithm.
    *
    * @param base Instances of this learner will be multiplexed.
    * @param d    This prediction will be returned during testing when the
    *             multiplexed <code>Learner</code> does not exist.
   **/
  public MuxLearner(Learner base, String d) { this("", base, d); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link MuxLearner.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public MuxLearner(Parameters p) { this("", p); }

  /** For the LBJ compiler; not for use by the LBJ user. */
  public MuxLearner(String n) { super(n); }

  /**
    * Instantiates this multiplexed learner using the specified base learning
    * algorithm.
    *
    * @param n    The name of the classifier.
    * @param base Instances of this learner will be multiplexed.
   **/
  public MuxLearner(String n, Learner base) {
    this(n, base, defaultDefaultPrediction);
  }

  /**
    * Instantiates this multiplexed learner using the specified base learning
    * algorithm.
    *
    * @param n    The name of the classifier.
    * @param base Instances of this learner will be multiplexed.
    * @param d    This prediction will be returned during testing when the
    *             multiplexed <code>Learner</code> does not exist.
   **/
  public MuxLearner(String n, Learner base, String d) {
    super(n);
    Parameters p = new Parameters();
    p.baseLearner = base;
    p.defaultPrediction = d;
    setParameters(p);
    network = new OVector();
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link MuxLearner.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public MuxLearner(String n, Parameters p) {
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
    setBase(p.baseLearner);
    defaultPrediction = p.defaultPrediction;
    setDefaultFeature();
  }


  /**
    * Sets the value of {@link #defaultFeature} according to the current value
    * of {@link #defaultPrediction}.
   **/
  protected void setDefaultFeature() {
    defaultFeature =
      new DiscretePrimitiveStringFeature(
            containingPackage, name, "default", defaultPrediction,
            valueIndexOf(defaultPrediction),
            (short) allowableValues().length);
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p = new Parameters(super.getParameters());
    p.baseLearner = baseLearner;
    p.defaultPrediction = defaultPrediction;
    return p;
  }


  /**
    * Sets {@link #baseLearner}.  This method will <i>not</i> have any effect
    * on the learners that already exist in the network.  However, new
    * learners created after this method is executed will be of the same type
    * as the object specified.
    *
    * @param base The new base learning algorithm.
   **/
  public void setBase(Learner base) {
    baseLearner = base;
    baseLearner.containingPackage = containingPackage;
    baseLearner.name = name + "::base";
  }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(Classifier l) {
    super.setLabeler(l);
    setBase(baseLearner);
  }


  /**
    * Sets the label lexicon.
    *
    * @param l  A feature lexicon.
   **/
  public void setLabelLexicon(Lexicon l) {
    super.setLabelLexicon(l);
    if (network != null) {
      int N = network.size();
      for (int i = 0; i < N; ++i) {
        Learner learner = (Learner) network.get(i);
        if (learner != null) learner.setLabelLexicon(l);
      }
    }
  }


  /**
    * The training example is multiplexed to the appropriate
    * <code>Learner</code>(s).
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param exampleLabels    The example's label(s).
    * @param labelValues      The labels' values.
   **/
  public void learn(int[] exampleFeatures, double[] exampleValues,
                    int[] exampleLabels, double[] labelValues) {
    assert exampleFeatures.length > exampleLabels.length
      : "MuxLearner ERROR: The example vector must have more features "
        + "than labels, since it is assumed that there is a correspondence "
        + "between the labels and the first features in the vector.";

    int F = exampleFeatures.length;
    int L = exampleLabels.length;
    int[] example = new int[F - L];
    double[] values = new double[F - L];
    int[] selections = new int[L];

    int i = 0, j = 0;
    for ( ; i < L; i++) {
      selections[i] = exampleFeatures[i];
    }
    for ( ; i < F; i++, j++) {
      example[j] = exampleFeatures[i];
      values[j] = exampleValues[i];
    }

    for (i = 0; i < L; i++) {
      Learner l = (Learner) network.get(selections[i]);

      if (l == null) {
        l = (Learner) baseLearner.clone();
        l.setLabelLexicon(labelLexicon);
        network.set(selections[i], l);
      }

      int[] labels = new int[1];
      labels[0] = exampleLabels[i];
      double[] labelVal = new double[1];
      labelVal[0] = labelValues[i];

      l.learn(example, values, labels, labelVal);
    }
  }


  /** Clears the network. */
  public void forget() {
    super.forget();
    network = new OVector();
  }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.  These scores are just the scores produced by the multiplexed
    * <code>Learner</code>'s <code>scores(Object)</code> method.
    *
    * @see   Learner#scores(Object)
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues) {
    int[] example = new int[exampleFeatures.length - 1];
    double[] values = new double[exampleFeatures.length - 1];
    System.arraycopy(exampleFeatures, 1, example, 0, example.length);
    System.arraycopy(exampleValues, 1, values, 0, values.length);

    int selection = exampleFeatures[0];
    Learner l = (Learner) network.get(selection);
    if (l == null)
      return
        new ScoreSet(new String[]{ defaultPrediction }, new double[]{ 1 });
    return l.scores(example, values);
  }


  /**
    * Returns the value of the discrete feature that would be returned by this
    * classifier.
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
    * Returns the value of the real feature that would be returned by this
    * classifier.
    *
    * @param f  The features array.
    * @param v  The values array.
    * @return The value of the feature produced for the input object.
   **/
  public double realValue(int[] f, double[] v) {
    return featureValue(f, v).getStrength();
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
    int[] example = new int[f.length - 1];
    double[] values = new double[f.length - 1];
    System.arraycopy(f, 1, example, 0, example.length);
    System.arraycopy(v, 1, values, 0, values.length);

    int selection = f[0];
    Learner l = (Learner) network.get(selection);
    if (l == null) return defaultFeature;
    return l.featureValue(example, values);
  }


  /**
    * This method performs the multiplexing and returns the output of the
    * selected <code>Learner</code>.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The output of the selected <code>Learner</code>.
   **/
  public FeatureVector classify(int[] exampleFeatures, double[] exampleValues)
  {
    return new FeatureVector(featureValue(exampleFeatures, exampleValues));
  }


  /**
    * Writes the algorithm's internal representation as text.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    int N = network.size();
    final FVector entries = new FVector(N);
    final OVector learners = new OVector(N);
    for (int i = 0; i < N; ++i) {
      Learner learner = (Learner) network.get(i);
      if (network.get(i) != null) {
        entries.add(lexicon.lookupKey(i));
        learners.add(learner);
      }
    }

    N = entries.size();
    int[] indexes = new int[N];
    for (int i = 0; i < N; ++i) indexes[i] = i;
    Sort.sort(indexes,
              new Sort.IntComparator() {
                public int compare(int i1, int i2) {
                  return entries.get(i1).compareTo(entries.get(i2));
                }
              });

    for (int i = 0; i < N; ++i) {
      out.println("select: " + entries.get(indexes[i]).getStringValue());
      Learner learner = (Learner) learners.get(indexes[i]);
      learner.setLexicon(lexicon);
      learner.write(out);
      learner.setLexicon(null);
    }

    out.println("End of MuxLearner");
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeString(defaultPrediction);
    baseLearner.write(out);
    int N = network.size();
    out.writeInt(N);

    int M = 0;
    for (int i = 0; i < N; ++i) if (network.get(i) != null) ++M;
    out.writeInt(M);

    for (int i = 0; i < N; ++i) {
      Learner learner = (Learner) network.get(i);
      if (learner != null) {
        out.writeInt(i);
        learner.write(out);
      }
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
    defaultPrediction = in.readString();
    setDefaultFeature();
    baseLearner = Learner.readLearner(in);
    int N = in.readInt();
    network = new OVector(N);
    int M = in.readInt();
    for (int i = 0; i < M; ++i)
      network.set(in.readInt(), Learner.readLearner(in));
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone() {
    MuxLearner clone = null;
    try { clone = (MuxLearner) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning MuxLearner: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    clone.baseLearner = (Learner) baseLearner.clone();
    int N = network.size();
    clone.network = new OVector(N);
    for (int i = 0; i < N; ++i) {
      Learner learner = (Learner) network.get(i);
      if (learner != null) clone.network.set(i, learner.clone());
    }

    return clone;
  }


  /**
    * Simply a container for all of {@link MuxLearner}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends Learner.Parameters
  {
    /**
      * Instances of this learning algorithm will be multiplexed; default
      * <code>null</code>.
     **/
    public Learner baseLearner;
    /**
      * This string is returned during testing when the multiplexed
      * <code>Learner</code> doesn't exist; default
      * {@link MuxLearner#defaultDefaultPrediction}.
     **/
    public String defaultPrediction;


    /** Sets all the default values. */
    public Parameters() {
      baseLearner = (Learner) defaultBaseLearner.clone();
      defaultPrediction = defaultDefaultPrediction;
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(Learner.Parameters p) {
      super(p);
      baseLearner = (Learner) defaultBaseLearner.clone();
      defaultPrediction = defaultDefaultPrediction;
    }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      super(p);
      baseLearner = p.baseLearner;
      defaultPrediction = p.defaultPrediction;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((MuxLearner) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();
      String name = baseLearner.getClass().getName();
      name = name.substring(name.lastIndexOf('.') + 1);

      if (!defaultPrediction.equals(MuxLearner.defaultDefaultPrediction))
        result += "defaultPrediction = " + defaultPrediction + ", ";
      result += name + ": " + baseLearner.getParameters().nonDefaultString();

      return result;
    }
  }
}

