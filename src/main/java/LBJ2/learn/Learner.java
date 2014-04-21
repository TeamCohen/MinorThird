package LBJ2.learn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URL;

import LBJ2.classify.Classifier;
import LBJ2.classify.DiscreteFeature;
import LBJ2.classify.DiscretePrimitiveStringFeature;
import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.FeatureVectorReturner;
import LBJ2.classify.LabelVectorReturner;
import LBJ2.classify.RealFeature;
import LBJ2.classify.ScoreSet;
import LBJ2.util.ClassUtils;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.FVector;


/**
  * Extend this class to create a new {@link Classifier} that learns to mimic
  * one an oracle classifier given a feature extracting classifier and example
  * objects.
  *
  * @author Nick Rizzolo
 **/
public abstract class Learner extends Classifier
{
  /** Stores the classifier used to produce labels. */
  protected Classifier labeler;
  /** Stores the classifiers used to produce features. */
  protected Classifier extractor;
  /** Stores the feature {@link Lexicon}. */
  protected Lexicon lexicon;
  /** Stores the label {@link Lexicon}. */
  protected Lexicon labelLexicon;
  /** The encoding used by this learner's feature lexicon. */
  protected String encoding;
  /**
    * Stores the set of predictions that this learner will choose from when
    * classifying a new example.
   **/
  protected FVector predictions;
  /** Caches the location of this learner's offline binary representation. */
  protected URL lcFilePath;
  /** Caches the location of this learner's offline lexicon. */
  protected URL lexFilePath;
  /**
    * Informs this learner that it can and should read its feature lexicon on
    * demand.
   **/
  protected boolean readLexiconOnDemand;


  /**
    * This constructor is used by the LBJ2 compiler; it should never be called
    * by a programmer.
   **/
  protected Learner() { }

  /**
    * Initializes the name.
    *
    * @param n  The name of the classifier.
   **/
  protected Learner(String n) {
    super(n);
    lexicon = new Lexicon();
    labelLexicon = new Lexicon();
    predictions = new FVector();
  }

  /**
    * Constructor for unsupervised learning.
    *
    * @param n  The name of the classifier.
    * @param e  The feature extracting classifier.
   **/
  protected Learner(String n, Classifier e) { this(n, null, e); }

  /**
    * Constructor for supervised learning.
    *
    * @param n  The name of the classifier.
    * @param l  The labeling classifier.
    * @param e  The feature extracting classifier.
   **/
  protected Learner(String n, Classifier l, Classifier e) {
    super(n);
    setLabeler(l);
    setExtractor(e);
    lexicon = new Lexicon();
    labelLexicon = new Lexicon();
    predictions = new FVector();
  }


  /**
    * Sets the values of parameters that control the behavior of this learning
    * algorithm.
    *
    * @param p  The parameters.
   **/
  public void setParameters(Parameters p) { p.setParameters(this); }
  /** Retrieves the parameters that are set in this learner. */
  public Parameters getParameters() { return new Parameters(); }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(Classifier l) { labeler = l; }
  /** Returns the labeler. */
  public Classifier getLabeler() { return labeler; }


  /**
    * Sets the extractor.
    *
    * @param e  A feature extracting classifier.
   **/
  public void setExtractor(Classifier e) { extractor = e; }
  /** Returns the extractor. */
  public Classifier getExtractor() { return extractor; }


  /**
    * Sets the feature lexicon.  If set to <code>null</code>, the JVM's
    * garbage collector is invoked.
    *
    * @param l  A feature lexicon.
   **/
  public void setLexicon(Lexicon l) {
    lexicon = l;
    if (l == null) System.gc();
    else l.setEncoding(encoding);
  }


  /** Returns the feature lexicon. */
  public Lexicon getLexicon() {
    demandLexicon();
    return lexicon;
  }


  /**
    * Sets the label lexicon.
    *
    * @param l  A feature lexicon.
   **/
  public void setLabelLexicon(Lexicon l) {
    labelLexicon = l;
    if (labelLexicon == null) {
      predictions = null;
      return;
    }

    int N = labelLexicon.size();
    predictions = new FVector(N);
    for (int i = 0; i < N; ++i) createPrediction(i);
  }


  /** Returns the label lexicon. */
  public Lexicon getLabelLexicon() { return labelLexicon; }


  /**
    * Sets the encoding to use in this learner's feature lexicon.
    *
    * @param e  The encoding.
   **/
  public void setEncoding(String e) {
    encoding = e;
    lexicon.setEncoding(e);
  }


  /**
    * Sets the location of the model as a regular file on this file system.
    *
    * @param p  The file's path.
   **/
  public void setModelLocation(String p) {
    try { lcFilePath = new URL("file:" + p); }
    catch (Exception e) {
      System.err.println("ERROR: Can't create URL for file '" + p + "':");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
    * Sets the location of the model as a <code>URL</code>.
    *
    * @param u  The model's location.
   **/
  public void setModelLocation(URL u) { lcFilePath = u; }

  /** Returns the model's location. */
  public URL getModelLocation() { return lcFilePath; }


  /**
    * Sets the location of the lexicon as a regular file on this file system.
    *
    * @param p  The file's path.
   **/
  public void setLexiconLocation(String p) {
    try { lexFilePath = new URL("file:" + p); }
    catch (Exception e) {
      System.err.println("ERROR: Can't create URL for file '" + p + "':");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
    * Sets the location of the model as a <code>URL</code>.
    *
    * @param u  The model's location.
   **/
  public void setLexiconLocation(URL u) { lexFilePath = u; }

  /** Returns the lexicon's location. */
  public URL getLexiconLocation() { return lexFilePath; }


  /**
    * Establishes a new feature counting policy for this learner's lexicon.
    *
    * @param policy The new feature counting policy.
   **/
  public void countFeatures(Lexicon.CountPolicy policy) {
    if (policy == Lexicon.CountPolicy.perClass
        && !getOutputType().equals("discrete"))
      throw new IllegalArgumentException(
          "LBJ ERROR: Learner.countFeatures: Can't do 'per class' feature "
          + "counting unless the learner is discrete.");
    demandLexicon();
    lexicon.countFeatures(policy);
  }


  /**
    * Returns this learner's feature lexicon after discarding any feature
    * counts it may have been storing.  This method is likely only useful when
    * the lexicon and its counts are currently stored on disk and
    * {@link #readLexiconOnDemand(String)} or
    * {@link #readLexiconOnDemand(URL)} has already been called, in which case
    * the lexicon is read from disk without wasting time loading the counts.
   **/
  public Lexicon getLexiconDiscardCounts() {
    if (readLexiconOnDemand && (lexicon == null || lexicon.size() == 0))
      lexicon = Lexicon.readLexicon(lexFilePath, false);
    else lexicon.countFeatures(Lexicon.CountPolicy.none);
    return lexicon;
  }


  /**
    * Returns a new, emtpy learner into which all of the parameters that
    * control the behavior of the algorithm have been copied.  Here, "emtpy"
    * means no learning has taken place.
   **/
  public Learner emptyClone() {
    Learner clone = (Learner) super.clone();
    clone.forget();
    return clone;
  }


  /**
    * Trains the learning algorithm given an object as an example.
    * By default, this simply converts the example object into arrays
    * and passes it to {@link #learn(int[],double[],int[],double[])}.
    *
    * @param example  An example of the desired learned classifier's behavior.
   **/
  public void learn(Object example) {
    Object[] exampleArray = getExampleArray(example);
    learn((int[]) exampleArray[0], (double[]) exampleArray[1],
          (int[]) exampleArray[2], (double[]) exampleArray[3]);
  }


  /**
    * Trains the learning algorithm given a feature vector as an example.
    * This simply converts the example object into arrays and passes it to
    * {@link #learn(int[],double[],int[],double[])}.
    *
    * @param vector An example of the desired learned classifier's behavior.
   **/
  public void learn(FeatureVector vector) {
    Classifier saveExtractor = getExtractor();
    Classifier saveLabeler = getLabeler();
    setExtractor(new FeatureVectorReturner());
    setLabeler(new LabelVectorReturner());

    learn((Object) vector);

    setExtractor(saveExtractor);
    setLabeler(saveLabeler);
  }


  /**
    * Trains the learning algorithm given an example formatted as
    * arrays of feature indices, their values, and the example labels.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param exampleLabels    The example's label(s).
    * @param labelValues      The values of the labels.
   **/
  abstract public void learn(int[] exampleFeatures, double[] exampleValues,
                             int[] exampleLabels, double[] labelValues);


  /**
    * Trains the learning algorithm given many objects as examples.  This
    * implementation simply calls {@link #learn(Object)} on each of the
    * objects in the input array and finishes by calling
    * {@link #doneLearning()}.  It should be overridden if there is a more
    * efficient implementation.
    *
    * @param examples Examples of the desired learned classifier's behavior.
   **/
  public void learn(Object[] examples) {
    for (int i = 0; i < examples.length; ++i)
      learn(examples[i]);
    doneLearning();
  }


  /**
    * Trains the learning algorithm given many feature vectors as examples.
    * This implementation simply calls {@link #learn(FeatureVector)} on each
    * of the vectors in the input array and finishes by calling
    * {@link #doneLearning()}.  It should be overridden if there is a more
    * efficient implementation.
    *
    * @param examples Examples of the desired learned classifier's behavior.
   **/
  public void learn(FeatureVector[] examples) {
    for (int i = 0; i < examples.length; ++i)
      learn(examples[i]);
    doneLearning();
  }


  /**
    * This method makes one or more decisions about a single object, returning
    * those decisions as {@link Feature}s in a vector.
    *
    * @param example  The object to make decisions about.
    * @return A vector of {@link Feature}s about the input object.
   **/
  public FeatureVector classify(Object example) {
    Object[] exampleArray = getExampleArray(example, false);
    return classify((int[]) exampleArray[0], (double[]) exampleArray[1]);
  }


  /**
    * This method makes one or more decisions about a single feature vector,
    * returning those decisions as {@link Feature}s in a vector.
    *
    * @param vector The vector to make decisions about.
    * @return A vector of {@link Feature}s about the input vector.
   **/
  public FeatureVector classify(FeatureVector vector) {
    Classifier saveExtractor = getExtractor();
    Classifier saveLabeler = getLabeler();
    setExtractor(new FeatureVectorReturner());
    setLabeler(new LabelVectorReturner());

    FeatureVector result = classify((Object) vector);

    setExtractor(saveExtractor);
    setLabeler(saveLabeler);
    return result;
  }


  /**
    * This method makes one or more decisions about a single object, returning
    * those decisions as {@link Feature}s in a vector.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return A vector of {@link Feature}s about the input object.
   **/
  abstract public FeatureVector classify(int[] exampleFeatures,
                                         double[] exampleValues);


  /**
    * Use this method to make a batch of classification decisions about
    * several objects.  This function is implemented in the most naive way
    * (simply calling {@link #classify(FeatureVector)} repeatedly) and should
    * be overridden if there is a more efficient implementation.
    *
    * @param vectors  The vectors to make decisions about.
    * @return An array of feature vectors, one per input vector.
   **/
  public FeatureVector[] classify(FeatureVector[] vectors) {
    FeatureVector[] result = new FeatureVector[vectors.length];
    for (int i = 0; i < vectors.length; ++i)
      result[i] = classify(vectors[i]);
    return result;
  }


  /**
    * Use this method to make a batch of classification decisions about
    * several examples.  This function is implemented in the most naive way
    * (simply calling {@link #classify(int[],double[])} repeatedly) and should
    * be overridden if there is a more efficient implementation.
    *
    * @param e  The examples to make decisions about, represented as arrays of
    *           indices and strengths.
    * @return An array of feature vectors, one per input object.
   **/
  public FeatureVector[] classify(Object[][] e) {
    FeatureVector[] result = new FeatureVector[e.length];
    for (int i = 0; i < e.length; ++i)
      result[i] = classify((int[]) e[i][0], (double[]) e[i][1]);
    return result;
  }


  /**
    * Returns the classification of the given example object as a single
    * feature instead of a {@link FeatureVector}.
    *
    * @param example  The object to classify.
    * @return The classification of <code>example</code> as a feature.
   **/
  public Feature featureValue(Object example) {
    Object[] exampleArray = getExampleArray(example, false);
    return featureValue((int[]) exampleArray[0], (double[]) exampleArray[1]);
  }


  /**
    * Returns the classification of the given feature vector as a single
    * feature instead of a {@link FeatureVector}.
    *
    * @param vector The vector to classify.
    * @return The classification of <code>vector</code> as a feature.
   **/
  public Feature featureValue(FeatureVector vector) {
    Classifier saveExtractor = getExtractor();
    Classifier saveLabeler = getLabeler();
    setExtractor(new FeatureVectorReturner());
    setLabeler(new LabelVectorReturner());

    Feature result = featureValue((Object) vector);

    setExtractor(saveExtractor);
    setLabeler(saveLabeler);
    return result;
  }


  /**
    * Returns the classification of the given example as a single feature
    * instead of a {@link FeatureVector}.
    *
    * @param f  The features array.
    * @param v  The values array.
    * @return The classification of <code>o</code> as a feature.
   **/
  public Feature featureValue(int[] f, double[] v) {
    throw
      new UnsupportedOperationException(
        "The featureValue(int[], double[]) method has not been overridden in "
        + "class '" + getClass().getName() + "'.");
  }


  /**
    * Returns the value of the discrete prediction that this learner would
    * make, given an example.
    *
    * @param example  The example object.
    * @return The discrete value.
   **/
  public String discreteValue(Object example) {
    Object[] exampleArray = getExampleArray(example, false);
    return
      discreteValue((int[]) exampleArray[0], (double[]) exampleArray[1]);
  }


  /**
    * Returns the value of the discrete prediction that this learner would
    * make, given a feature vector.
    *
    * @param vector The example vector.
    * @return The discrete value.
   **/
  public String discreteValue(FeatureVector vector) {
    Classifier saveExtractor = getExtractor();
    Classifier saveLabeler = getLabeler();
    setExtractor(new FeatureVectorReturner());
    setLabeler(new LabelVectorReturner());

    String result = discreteValue((Object) vector);

    setExtractor(saveExtractor);
    setLabeler(saveLabeler);
    return result;
  }


  /**
    * Returns the value of the discrete feature that would be returned by this
    * classifier.  This method should only be called when overridden by a
    * classifier returning a single discrete feature.
    *
    * @param f  The features array.
    * @param v  The values array.
    * @return The value of the feature produced for the input object.
   **/
  public String discreteValue(int[] f, double[] v) {
    throw
      new UnsupportedOperationException(
        "The discreteValue(Object) method has not been overridden in class '"
        + getClass().getName() + "'.");
  }


  /**
    * Returns the value of the real prediction that this learner would
    * make, given an example.
    *
    * @param example  The example object.
    * @return The real value.
   **/
  public double realValue(Object example) {
    Object[] exampleArray = getExampleArray(example, false);
    return realValue((int[])exampleArray[0], (double[])exampleArray[1]);
  }


  /**
    * Returns the value of the real prediction that this learner would
    * make, given a feature vector.
    *
    * @param vector The example vector.
    * @return The real value.
   **/
  public double realValue(FeatureVector vector) {
    Classifier saveExtractor = getExtractor();
    Classifier saveLabeler = getLabeler();
    setExtractor(new FeatureVectorReturner());
    setLabeler(new LabelVectorReturner());

    double result = realValue((Object) vector);

    setExtractor(saveExtractor);
    setLabeler(saveLabeler);
    return result;
  }


  /**
    * Returns the value of the real feature that would be returned by this
    * classifier.  This method should only be called when overridden by a
    * classifier returning a single real feature.
    *
    * @param f  The features array.
    * @param v  The values array.
    * @return The value of the feature produced for the input object.
   **/
  public double realValue(int[] f, double[] v) {
    throw
      new UnsupportedOperationException(
        "The realValue(Object) method has not been overridden in class '"
        + getClass().getName() + "'.");
  }


  /**
    * Overridden by subclasses to perform any required post-processing
    * computations after all training examples have been observed through
    * {@link #learn(Object)} and {@link #learn(Object[])}.  By default this
    * method does nothing.
   **/
  public void doneLearning() {
  }


  /**
    * This method is sometimes called before training begins, although it is
    * not guaranteed to be called at all.  It allows the number of examples
    * and number of features to be passed to the Learner, in case this
    * information is available, such as after pre-extraction.  By default this
    * method does nothing.
    *
    * @param numExamples  The number of examples that will be observed during
    *                     training.
    * @param numFeatures  The number of features that will be observed during
    *                     training.
   **/
  public void initialize(int numExamples, int numFeatures) {
  }


  /** Called after each round of training.  Does nothing by default. */
  public void doneWithRound() {
  }


  /**
    * Converts an example object into an array of arrays representing the
    * example including its labels.  The first array contains the integer keys
    * of the example's features, as indexed in the lexicon.  The second array
    * gives the double values corresponding to the strengths of the features
    * in the first array.  The third and fourth arrays play the same roles as
    * the first and second arrays respectively, except they describe the
    * labels.
    *
    * @param example  The example object.
    * @return The converted example array.
   **/
  public Object[] getExampleArray(Object example) {
    return getExampleArray(example, true);
  }


  /**
    * Converts an example object into an array of arrays representing the
    * example.  The first array contains the integer keys of the example's
    * features, as indexed in the lexicon.  The second array gives the double
    * values corresponding to the strengths of the features in the first
    * array.  The third and fourth arrays will only be present if
    * <code>training</code> is set to <code>true</code>.  They play the same
    * roles as the first and second arrays respectively, except they describe
    * the labels.
    *
    * @param example  The example object.
    * @param training Whether or not labels should be extracted.
    * @return The converted example array.
   **/
  public Object[] getExampleArray(Object example, boolean training) {
    if (example instanceof Object[]
        && ((Object[]) example)[0] instanceof int[]
        && ((Object[]) example)[1] instanceof double[])
      return (Object[]) example;

    if (readLexiconOnDemand && (lexicon == null || lexicon.size() == 0)) {
      readLexicon(lexFilePath);
      readLexiconOnDemand = false;
    }

    Object[] exampleArray = null;
    Lexicon.CountPolicy countPolicy = lexicon.getCountPolicy();
    int labelIndex = -1;

    // Get example labels
    if (training) {
      FeatureVector labelVector = labeler.classify(example);
      int F = labelVector.featuresSize();
      int[] labelArray = new int[F];
      double[] labelValues = new double[F];

      for (int f = 0; f < F; ++f) {
        Feature label = labelVector.getFeature(f);
        if (label.isDiscrete())
          labelArray[f] = labelLexicon.lookup(label, true);
        else
          labelArray[f] =
            labelLexicon.lookup(label.getFeatureKey(labelLexicon), true);
        labelValues[f] += label.getStrength();
        createPrediction(labelArray[f]);
      }

      exampleArray = new Object[]{ null, null, labelArray, labelValues };
      if (countPolicy == Lexicon.CountPolicy.perClass)
          //&& labeler.getOutputType().equals("discrete") && F == 1)
          // Don't really want to do this comparison for every example; we'll
          // trust the user not to do per class feature counting when it isn't
          // true.  Plus, the countFeatures(CountPolicy) method in this class
          // checks for it.
        labelIndex = labelArray[0];
    }
    else exampleArray = new Object[2];

    // Get example features.
    FeatureVector featureVector = extractor.classify(example);
    int F = featureVector.featuresSize();
    int[] exampleArrayFeatures = new int[F];
    double[] exampleArrayValues = new double[F];
    exampleArray[0] = exampleArrayFeatures;
    exampleArray[1] = exampleArrayValues;

    for (int f = 0; f < F; ++f) {
      Feature feature = featureVector.getFeature(f);
      exampleArrayFeatures[f] =
        lexicon.lookup(feature.getFeatureKey(lexicon, training, labelIndex),
                       training, labelIndex);
      exampleArrayValues[f] += feature.getStrength();
    }

    return exampleArray;
  }


  /**
    * If it hasn't been created already, this method will create the
    * prediction feature in {@link #predictions} associated with the label
    * feature at the given index of {@link #labelLexicon}.  This method does
    * not create {@link RealFeature}s in {@link #predictions} since their
    * strengths cannot be modified.  In association with
    * {@link DiscreteFeature}s it creates a
    * {@link DiscretePrimitiveStringFeature} with an empty identifier.  Its
    * <code>value</code>, <code>valueIndex</code>, and
    * <code>totalValues</code> fields are filled by calling the label
    * feature's {@link Feature#getStringValue() getStringValue()},
    * {@link Feature#getValueIndex() getValueIndex()}, and
    * {@link Feature#totalValues() totalValues()} methods respectively.
    *
    * @param index  The index of a label feature in {@link #labelLexicon}.
   **/
  protected void createPrediction(int index) {
    createPrediction(labelLexicon, index);
  }


  /**
    * If it hasn't been created already, this method will create the
    * prediction feature in {@link #predictions} associated with the label
    * feature at the given index of <code>lex</code>.  This method does
    * not create {@link RealFeature}s in {@link #predictions} since their
    * strengths cannot be modified.  In association with
    * {@link DiscreteFeature}s it creates a
    * {@link DiscretePrimitiveStringFeature} with an empty identifier.  Its
    * <code>value</code>, <code>valueIndex</code>, and
    * <code>totalValues</code> fields are filled by calling the label
    * feature's {@link Feature#getStringValue() getStringValue()},
    * {@link Feature#getValueIndex() getValueIndex()}, and
    * {@link Feature#totalValues() totalValues()} methods respectively.
    *
    * @param lex    The label lexicon to associate prediction features with.
    * @param index  The index of a label feature in <code>lex</code>.
   **/
  protected void createPrediction(Lexicon lex, int index) {
    if (predictions.get(index) != null
        || !getOutputType().equals("discrete"))
      return;
    Feature label = lex.lookupKey(index);
    predictions.set(index,
                    new DiscretePrimitiveStringFeature(
                          containingPackage, name, "", label.getStringValue(),
                          label.getValueIndex(), label.totalValues()));
  }


  /**
    * Reinitializes the learner to the state it started at before any learning
    * was performed.  By default, this sets the lexicons to blank Lexicon
    * objects and calls {@link #initialize(int,int)} to reset the number of
    * examples and features to 0, for learners that use this.
   **/
  public void forget() {
    lexicon = new Lexicon(encoding);
    labelLexicon = new Lexicon();
    predictions = new FVector();
    initialize(0, 0);
    readLexiconOnDemand = false;
  }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.  Learners that return a <code>real</code> feature or more than
    * one feature may implement this method by simply returning
    * <code>null</code>.
    *
    * @param example  The object to make decisions about.
    * @return A set of scores indicating the degree to which each possible
    *         discrete classification value is associated with the given
    *         example object.
   **/
  public ScoreSet scores(Object example) {
    Object[] exampleArray = getExampleArray(example, false);
    return scores((int[])exampleArray[0], (double[])exampleArray[1]);
  }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given feature
    * vector.  Learners that return a <code>real</code> feature or more than
    * one feature may implement this method by simply returning
    * <code>null</code>.
    *
    * @param vector The vector to make decisions about.
    * @return A set of scores indicating the degree to which each possible
    *         discrete classification value is associated with the given
    *         example vector.
   **/
  public ScoreSet scores(FeatureVector vector) {
    Classifier saveExtractor = getExtractor();
    Classifier saveLabeler = getLabeler();
    setExtractor(new FeatureVectorReturner());
    setLabeler(new LabelVectorReturner());

    ScoreSet result = scores((Object) vector);

    setExtractor(saveExtractor);
    setLabeler(saveLabeler);
    return result;
  }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.  Learners that return a <code>real</code> feature or more than
    * one feature may implement this method by simply returning
    * <code>null</code>.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of values
    * @return A set of scores indicating the degree to which each possible
    *         discrete classification value is associated with the given
    *         example object.
   **/
  abstract public ScoreSet scores(int[] exampleFeatures,
                                  double[] exampleValues);


  /**
    * Writes the learned function's internal representation as text.
    *
    * @param out  The output stream.
   **/
  abstract public void write(PrintStream out);


  /**
    * Automatically generated code will override this method to set their
    * <code>isClone</code> field to <code>false</code>.  This then allows a
    * pure java program to read a learner's representation into any instance
    * of the learner's class.  By default, this method does nothing.
   **/
  public void unclone() { }


  /**
    * Returns the size of the lexicon after any pruning that may have taken
    * place or 0 if the lexicon's location isn't known.
   **/
  public int getPrunedLexiconSize() {
    if ((lexicon == null || lexicon.size() == 0) && readLexiconOnDemand) {
      ExceptionlessInputStream in =
        ExceptionlessInputStream.openCompressedStream(lexFilePath);
      int result = Lexicon.readPrunedSize(in);
      in.close();
      return result;
    }

    return lexicon == null ? 0 : lexicon.getCutoff();
  }


  /**
    * Returns a deep (enough) clone of this learner.  The following fields are
    * cloned themselves: {@link #lexicon}, {@link #labelLexicon}, and
    * {@link #predictions}.
    *
    * <p> Note that this is an overriding implementation of
    * <code>Object</code>'s <code>clone()</code> method, and its functionality
    * is completely separate from and unrelated to that of this class's
    * {@link #unclone()} method.
   **/
  public Object clone() {
    Learner result = (Learner) super.clone();
    if (lexicon != null) result.lexicon = (Lexicon) lexicon.clone();
    if (labelLexicon != null)
      result.labelLexicon = (Lexicon) labelLexicon.clone();
    if (predictions != null)
      result.predictions = (FVector) predictions.clone();
    return result;
  }


  /**
    * Writes the binary representation of this learned function if there is a
    * location cached in {@link #lcFilePath}, and writes the binary
    * representation of the feature lexicon if there is a location cached in
    * {@link #lexFilePath}.
   **/
  public void save() {
    if (lcFilePath != null) saveModel();
    if (lexFilePath != null && lexicon != null && lexicon.size() > 0)
      saveLexicon();
  }


  /**
    * Writes the binary representation of this learned function to the
    * location specified by {@link #lcFilePath}.  If {@link #lcFilePath} is
    * not set, this method will produce an error message and exit the program.
   **/
  public void saveModel() {
    if (lcFilePath == null) {
      System.err.println(
          "LBJ ERROR: saveModel() called without a cached location");
      new Exception().printStackTrace();
      System.exit(1);
    }

    ExceptionlessOutputStream out =
      ExceptionlessOutputStream.openCompressedStream(lcFilePath);
    write(out);
    out.close();
  }


  /**
    * Writes the binary representation of the feature lexicon to the location
    * specified by {@link #lexFilePath}.  If {@link #lexFilePath} is not set,
    * this method will produce an error message and exit the program.
   **/
  public void saveLexicon() {
    if (lexFilePath == null) {
      System.err.println(
          "LBJ ERROR: saveLexicon() called without a cached location");
      new Exception().printStackTrace();
      System.exit(1);
    }

    ExceptionlessOutputStream out =
      ExceptionlessOutputStream.openCompressedStream(lexFilePath);
    if (lexicon == null) out.writeInt(0);
    else lexicon.write(out);
    out.close();
  }


  /**
    * Writes the learned function's binary internal represetation including
    * both its model and lexicons to the specified files.  These files are
    * then cached in {@link #lcFilePath} and {@link #lexFilePath}.
    *
    * @param modelFile  The name of the file in which to write the model.
    * @param lexFile    The name of the file in which to write the feature
    *                   lexicon.
   **/
  public void write(String modelFile, String lexFile) {
    writeModel(modelFile);
    if (lexicon != null && lexicon.size() > 0) writeLexicon(lexFile);
  }


  /**
    * Writes only the learned function's model (which includes the label
    * lexicon) to the specified file in binary form.  This file is then cached
    * in {@link #lcFilePath}.
    *
    * @param filename The name of the file in which to write the model.
   **/
  public void writeModel(String filename) {
    ExceptionlessOutputStream out =
      ExceptionlessOutputStream.openCompressedStream(filename);
    write(out);
    out.close();

    try { lcFilePath = new URL("file:" + filename); }
    catch (Exception e) {
      System.err.println("Error constructing URL:");
      e.printStackTrace();
      System.exit(1);
    }
  }


  /**
    * Writes the learned function's feature lexicon to the specified file.
    * This file is then cached in {@link #lexFilePath}.
    *
    * @param filename The name of the file in which to write the feature
    *                 lexicon.
   **/
  public void writeLexicon(String filename) {
    ExceptionlessOutputStream out =
      ExceptionlessOutputStream.openCompressedStream(filename);
    if (lexicon == null) out.writeInt(0);
    else lexicon.write(out);
    out.close();

    try { lexFilePath = new URL("file:" + filename); }
    catch (Exception e) {
      System.err.println("Error constructing URL:");
      e.printStackTrace();
      System.exit(1);
    }
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    out.writeString(getClass().getName());
    out.writeString(containingPackage);
    out.writeString(name);
    out.writeString(encoding);
    if (labeler == null) out.writeString(null);
    else out.writeString(labeler.getClass().getName());
    if (extractor == null) out.writeString(null);
    else out.writeString(extractor.getClass().getName());
    if (labelLexicon == null) out.writeInt(0);
    else labelLexicon.write(out);
    if (predictions == null) out.writeInt(0);
    else predictions.write(out);
  }


  /**
    * Reads the learned function's binary internal represetation including
    * both its model and lexicons from the specified files, overwriting any
    * and all data this object may have already contained.  These files are
    * then cached in {@link #lcFilePath} and {@link #lexFilePath}.
    *
    * @param modelFile  The name of the file from which to read the model.
    * @param lexFile    The name of the file from which to read the feature
    *                   lexicon.
   **/
  public void read(String modelFile, String lexFile) {
    readModel(modelFile);
    readLexicon(lexFile);
  }


  /**
    * Reads only the learned function's model and label lexicon from the
    * specified file in binary form, overwriting whatever model data may have
    * already existed in this object.  This file is then cached in
    * {@link #lcFilePath}.
    *
    * @param filename The name of the file from which to read the model.
   **/
  public void readModel(String filename) {
    try { readModel(new URL("file:" + filename)); }
    catch (Exception e) {
      System.err.println("Error constructing URL:");
      e.printStackTrace();
      System.exit(1);
    }
  }


  /**
    * Reads only the learned function's model and label lexicon from the
    * specified location in binary form, overwriting whatever model data may
    * have already existed in this object.  This location is then cached in
    * {@link #lcFilePath}.
    *
    * @param url  The location from which to read the model.
   **/
  public void readModel(URL url) {
    ExceptionlessInputStream in =
      ExceptionlessInputStream.openCompressedStream(url);
    String s = in.readString();
    String expected = getClass().getName();

    if (!s.equals(expected)) {
      System.err.println("Error reading model from '" + url + "':");
      System.err.println("  Expected '" + expected + "' but received '" + s
                         + "'");
      new Exception().printStackTrace();
      in.close();
      System.exit(1);
    }

    read(in);
    in.close();
    lcFilePath = url;
  }


  /**
    * Reads the learned function's feature lexicon from the specified file,
    * overwriting the lexicon present in this object, if any.  This file is
    * then cached in {@link #lexFilePath}.
    *
    * @param filename The name of the file from which to read the feature
    *                 lexicon.
   **/
  public void readLexicon(String filename) {
    try { readLexicon(new URL("file:" + filename)); }
    catch (Exception e) {
      System.err.println("Error constructing URL:");
      e.printStackTrace();
      System.exit(1);
    }
  }


  /**
    * Reads the learned function's feature lexicon from the specified
    * location, overwriting the lexicon present in this object, if any.  This
    * location is then cached in {@link #lexFilePath}.
    *
    * @param url  The location from which to read the feature lexicon.
   **/
  public void readLexicon(URL url) {
    lexicon = Lexicon.readLexicon(url);
    lexFilePath = url;
  }


  /**
    * Reads the binary representation of any type of learner (including the
    * label lexicon, but not including the feature lexicon) from the given
    * file.  In that file, there should first be stored a string containing
    * the fully qualified class name of the learner.  If the <i>short</i>
    * value <code>-1</code> appears instead, this method returns
    * <code>null</code>.
    *
    * <p> This method is appropriate for reading learners as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param filename The name of the file from which to read the learner.
    * @return The learner read from the file.
   **/
  public static Learner readLearner(String filename) {
    return readLearner(filename, true);
  }


  /**
    * Reads the binary representation of any type of learner (including the
    * label lexicon, but not including the feature lexicon), with the option
    * of cutting off the reading process after the label lexicon and before
    * any learned parameters.  When <code>whole</code> is <code>false</code>,
    * the reading process is cut off in this way.
    *
    * <p> This method is appropriate for reading learners as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param filename The name of the file from which to read the learner.
    * @param whole    Whether or not to read the whole model.
    * @return The learner read from the file.
   **/
  public static Learner readLearner(String filename, boolean whole) {
    URL url = null;

    try { url = new URL("file:" + filename); }
    catch (Exception e) {
      System.err.println("Error constructing URL:");
      e.printStackTrace();
      System.exit(1);
    }

    return readLearner(url, whole);
  }


  /**
    * Reads the binary representation of any type of learner (including the
    * label lexicon, but not including the feature lexicon) from the given
    * location.  At that location, there should first be stored a string
    * containing the fully qualified class name of the learner.  If the
    * <i>short</i> value <code>-1</code> appears instead, this method returns
    * <code>null</code>.  Finally, the location is cached in
    * {@link #lcFilePath}.
    *
    * <p> This method is appropriate for reading learners as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param url  The location from which to read the learner.
    * @return The learner read from the location.
   **/
  public static Learner readLearner(URL url) {
    return readLearner(url, true);
  }


  /**
    * Reads the binary representation of any type of learner (including the
    * label lexicon, but not including the feature lexicon), with the option
    * of cutting off the reading process after the label lexicon and before
    * any learned parameters.  When <code>whole</code> is <code>false</code>,
    * the reading process is cut off in this way.  Finally, the location is
    * cached in {@link #lcFilePath}.
    *
    * <p> This method is appropriate for reading learners as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param url    The location from which to read the learner.
    * @param whole  Whether or not to read the whole model.
    * @return The learner read from the location.
   **/
  public static Learner readLearner(URL url, boolean whole) {
    ExceptionlessInputStream in =
      ExceptionlessInputStream.openCompressedStream(url);
    Learner result = readLearner(in, whole);
    in.close();
    result.lcFilePath = url;
    return result;
  }


  /**
    * Reads the binary representation of any type of learner (including the
    * label lexicon, but not including the feature lexicon) from the given
    * stream.  The stream is expected to first return a string containing the
    * fully qualified class name of the learner.  If the <i>short</i> value
    * <code>-1</code> appears instead, this method returns <code>null</code>.
    *
    * <p> This method is appropriate for reading learners as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param in The input stream.
    * @return The learner read from the stream.
   **/
  public static Learner readLearner(ExceptionlessInputStream in) {
    return readLearner(in, true);
  }


  /**
    * Reads the binary representation of any type of learner (including the
    * label lexicon, but not including the feature lexicon), with the option
    * of cutting off the reading process after the label lexicon and before
    * any learned parameters.  When <code>whole</code> is <code>false</code>,
    * the reading process is cut off in this way.
    *
    * <p> This method is appropriate for reading learners as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param in     The input stream.
    * @param whole  Whether or not to read the whole model.
    * @return The learner read from the stream.
   **/
  public static Learner readLearner(ExceptionlessInputStream in,
                                    boolean whole) {
    String name = in.readString();
    if (name == null) return null;
    Learner result = ClassUtils.getLearner(name);
    result.unclone();
    if (whole) result.read(in);     // Overridden by decendents
    else {
      result.readLabelLexicon(in);  // Should not be overridden by decendents
      Lexicon labelLexicon = result.getLabelLexicon();
      result.forget();
      result.setLabelLexicon(labelLexicon);
    }
    return result;
  }


  /**
    * Reads the binary representation of a learner with this object's run-time
    * type, overwriting any and all learned or manually specified parameters
    * as well as the label lexicon but without modifying the feature lexicon.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) { readLabelLexicon(in); }


  /**
    * Reads the initial portion of the model file, including the containing
    * package and name strings, the names of the labeler and extractor, and
    * finally the label lexicon.  This method will not read any further model
    * parameters, however.
    *
    * @param in The input stream.
   **/
  public void readLabelLexicon(ExceptionlessInputStream in) {
    containingPackage = in.readString().intern();
    name = in.readString().intern();
    encoding = in.readString();
    if (encoding != null) encoding = encoding.intern();
    String s = in.readString();
    labeler = s == null ? null : ClassUtils.getClassifier(s);
    s = in.readString();
    extractor = s == null ? null : ClassUtils.getClassifier(s);
    labelLexicon = Lexicon.readLexicon(in);
    if (predictions == null) predictions = new FVector();
    predictions.read(in);
  }


  /**
    * Prepares this learner to read in its feature lexicon from the specified
    * location on demand; has no effect if this learner already has a
    * non-empty lexicon.
    *
    * @param file The file from which to read the feature lexicon.
   **/
  public void readLexiconOnDemand(String file) {
    URL url = null;

    try { url = new URL("file:" + file); }
    catch (Exception e) {
      System.err.println("Error constructing URL:");
      e.printStackTrace();
      System.exit(1);
    }

    readLexiconOnDemand(url);
  }


  /**
    * Prepares this learner to read in its feature lexicon from the specified
    * location on demand; has no effect if this learner already has a
    * non-empty lexicon.
    *
    * @param url  The location from which to read the feature lexicon.
   **/
  public void readLexiconOnDemand(URL url) {
    lexFilePath = url;
    readLexiconOnDemand = true;
  }


  /**
    * Forces this learner to read in its lexicon representation, but only if
    * the lexicon currently available in this object is empty and the learner
    * has been scheduled to read its lexicon on demand with
    * {@link #readLexiconOnDemand(URL)}.
    *
    * @see #readLexiconOnDemand
    * @return The lexicon just read into {@link #lexicon}.
   **/
  public Lexicon demandLexicon() {
    if (readLexiconOnDemand && (lexicon == null || lexicon.size() == 0)) {
      readLexicon(lexFilePath);
      readLexiconOnDemand = false;
    }
    return lexicon;
  }


  /**
    * Serializes a {@link Learner.Parameters} object to the specified file.
    *
    * @param p    The parameters to serialize.
    * @param file The file in which to serialize them.
   **/
  public static void writeParameters(Parameters p, String file) {
    ObjectOutputStream oos = null;
    try {
      oos =
        new ObjectOutputStream(
          new BufferedOutputStream(
              new FileOutputStream(file)));
    }
    catch (Exception e) {
      System.err.println(
          "Can't create object output stream in '" + file + "': " + e);
      System.exit(1);
    }

    try { oos.writeObject(p); }
    catch (Exception e) {
      System.err.println(
          "Can't write to object output stream in '" + file + "': " + e);
      System.exit(1);
    }

    try { oos.close(); }
    catch (Exception e) {
      System.err.println("Can't close object stream in '" + file + "': " + e);
      System.exit(1);
    }
  }


  /**
    * Deserializes a {@link Learner.Parameters} object out of the specified
    * locaiton.
    *
    * @param url  The location from which to read the object.
    * @return The parameters object.
   **/
  public static Parameters readParameters(URL url) {
    ObjectInputStream ois = null;

    try {
      ois =
        new ObjectInputStream(
            new BufferedInputStream(url.openStream()));
    }
    catch (Exception e) {
      System.err.println("Can't open '" + url + "' for input: " + e);
      System.exit(1);
    }

    Parameters result = null;

    try { result = (Parameters) ois.readObject(); }
    catch (Exception e) {
      System.err.println("Can't read from '" + url + "': " + e);
      System.exit(1);
    }

    try { ois.close(); }
    catch (Exception e) {
      System.err.println("Can't close '" + url + "': " + e);
      System.exit(1);
    }

    return result;
  }


  /**
    * <code>Parameters</code> classes are used to hold values for learning
    * algorithm parameters, and all learning algorithm implementations must
    * provide a constructor that takes such an object as input.  All algorithm
    * specific <code>Parameters</code> classes extend this class.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters implements Serializable
  {
    /**
      * The number of rounds of training; but wait; this parameter doesn't
      * actually affect the behavior of any learners as the number of training
      * rounds is specified via other mechanisms.  Nonetheless, it comes in
      * handy to have it here as a communication vehicle when tuning
      * parameters.
     **/
    public int rounds;


    /** Sets all the default values. */
    public Parameters() { }

    /** Copy constructor. */
    public Parameters(Parameters p) { }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      Class c = getClass();
      if (Learner.class.equals(c))
        throw new UnsupportedOperationException(
            "LBJ ERROR: Learner.Parameters.setParameters should never be "
            + "called.");
      else
        throw new UnsupportedOperationException(
            "LBJ ERROR: " + c.getName() + ".Parameters.setParameters has not "
            + "been implemented.");
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() { return ""; }
  }
}

