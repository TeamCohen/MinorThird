package LBJ2.learn;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;

import LBJ2.classify.DiscretePrimitiveStringFeature;
import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.RealPrimitiveStringFeature;
import LBJ2.classify.ScoreSet;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;


/**
  * Translates LBJ's internal problem representation into that which can
  * be handled by WEKA learning algorithms.  This translation involves storing
  * all examples in memory so they can be passed to WEKA at one time.
  *
  * <p> WEKA must be available on your <code>CLASSPATH</code> in order to use
  * this class.  WEKA source code and pre-compiled jar distributions are
  * available at:
  * <a href="http://www.cs.waikato.ac.nz/ml/weka/">http://www.cs.waikato.ac.nz/ml/weka/</a>
  *
  * <p> To use this class in the <code>with</code> clause of a learning
  * classifier expression, the following restrictions must be recognized:
  * <ul>
  *   <li> Feature pre-extraction must be enabled.
  *   <li> No hard-coded feature generators may be referenced in the
  *        <code>using</code> clause.
  *   <li> No array producing classifiers may be referenced in the
  *        <code>using</code> clause.
  *   <li> The names of classifiers referenced in the <code>using</code>
  *        clause may not contain the underscore character ('<code>_</code>').
  *   <li> The values produced by discrete classifiers referenced in the
  *        <code>using</code> clause may not contain the underscore, colon,
  *        or comma characters ('<code>_</code>', '<code>:</code>', or
  *        '<code>,</code>').
  * </ul>
  *
  * <p> To use this class in a Java application, the following restrictions
  * must be recognized:
  * <ul>
  *   <li> {@link #doneLearning()} must be called before calls to
  *        {@link #classify(Object)} can be made.
  *   <li> After {@link #doneLearning()} is called, {@link #learn(Object)} may
  *        not be called without first calling {@link #forget()}.
  * </ul>
  *
  * @author Dan Muriello
 **/
public class WekaWrapper extends Learner
{
  /** Default for the {@link #attributeString} field. */
  public static final String defaultAttributeString = "";
  /** Default for the {@link #baseClassifier} field. */
  public static final AbstractClassifier defaultBaseClassifier = new weka.classifiers.bayes.NaiveBayes();


  /** A string encoding of the attributes used by this learner. */
  protected String attributeString;
  /**
    * Stores the instance of the WEKA classifier which we are training;
    * default is <code>bayes.NaiveBayes</code>.
   **/
  protected AbstractClassifier baseClassifier;
  /**
    * Stores a fresh instance of the WEKA classifier for the purposes of
    * forgetting.
   **/
  protected Classifier freshClassifier;
  /**
    * Information about the features this learner takes as input is parsed
    * from an attribute string and stored here.  This information is crucial
    * in the task of interfacing with the WEKA algorithms, and must be present
    * before the {@link #learn(Object)} method can be called.
    *
    * <p> Here is an example of a valid attribute string:
    * <code>nom_SimpleLabel_1,2,3,:str_First:nom_Second_a,b,c,d,r,t,:num_Third:</code>
    *
    * <p> <code>nom</code> stands for "Nominal", i.e. the feature
    * <code>SimpleLabel</code> was declared as <code>discrete</code>, and had
    * the value list <code>{"1","2","3"}</code>.
    *
    * <p> <code>str</code> stands for "Stirng", i.e. the feature
    * <code>First</code> was declared to be <code>discrete</code>, but was not
    * provided with a value list.  When using the <code>WekaWrapper</code>, it
    * is best to provide value lists whenever possible, because very few WEKA
    * classifiers can handle string attributes.
    *
    * <p> <code>num</code> stands for "Numerical", i.e. the feature
    * <code>Third</code> was declared to be <code>real</code>.
   **/
  protected FastVector attributeInfo = new FastVector();
  /** The main collection of Instance objects.*/
  protected Instances instances;
  /**
    * Indicates whether the {@link #doneLearning()} method has been called
    * and the {@link #forget()} method has not yet been called.
   **/
  protected boolean trained = false;
  /** The label producing classifier's allowable values. */
  protected String[] allowableValues;


  /**
    * Empty constructor.  Instantiates this wrapper with the default learning
    * algorithm: <code>bayes.NaiveBayes</code>.  Attribute
    * information must be provided before any learning can occur.
   **/
  public WekaWrapper() {
    this("");
  }

  /**
    * Partial constructor; attribute information must be provided before any
    * learning can occur.
    *
    * @param base The classifier to be used in this system.
   **/
  public WekaWrapper(AbstractClassifier base) {
    this("", base);
  }

  /**
    * Redirecting constructor.
    *
    * @param base             The classifier to be used in this system.
    * @param attributeString  The string describing the types of attributes
    *                         example objects will have.
   **/
  public WekaWrapper(AbstractClassifier base, String attributeString)
  {
    this("", base, attributeString);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link WekaWrapper.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public WekaWrapper(Parameters p) { this("", p); }

  /**
    * Empty constructor.  Instantiates this wrapper with the default learning
    * algorithm: <code>bayes.NaiveBayes</code>.  Attribute
    * information must be provided before any learning can occur.
    *
    * @param n  The name of the classifier.
   **/
  public WekaWrapper(String n) { this(n, new Parameters()); }

  /**
    * Partial constructor; attribute information must be provided before any
    * learning can occur.
    *
    * @param base The classifier to be used in this system.
   **/
  public WekaWrapper(String n, AbstractClassifier base) {
    this(n, base, defaultAttributeString);
  }

  /**
    * Default Constructor.  Instantiates this wrapper with the default
    * learning algorithm: <code>bayes.NaiveBayes</code>.
    *
    * @param n                The name of the classifier.
    * @param attributeString  The string describing the types of attributes
    *                         example objects will have.
   **/
  public WekaWrapper(String n, String attributeString) {
    this(n, defaultBaseClassifier, attributeString);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link WekaWrapper.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public WekaWrapper(String n, Parameters p) {
    super(n);
    setParameters(p);
    freshClassifier = baseClassifier;
  }

  /**
    * Full Constructor.
    *
    * @param n                The name of the classifier
    * @param base             The classifier to be used in this system.
    * @param attributeString  The string describing the types of attributes
    *                         example objects will have.
   **/
  public WekaWrapper(String n, AbstractClassifier base,
                     String attributeString) {
    super(n);
    Parameters p = new Parameters();
    p.baseClassifier = base;
    p.attributeString = attributeString;
    setParameters(p);
    freshClassifier = base;
  }


  /**
    * Sets the values of parameters that control the behavior of this learning
    * algorithm.
    *
    * @param p  The parameters.
   **/
  public void setParameters(Parameters p) {
    baseClassifier = p.baseClassifier;
    attributeString = p.attributeString;
    initializeAttributes();
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p = new Parameters(super.getParameters());
    p.baseClassifier = baseClassifier;
    p.attributeString = attributeString;
    return p;
  }


  /** This learner's output type is <code>"mixed%"</code>. */
  public String getOutputType() { return "mixed%"; }


  /**
    * Takes <code>attributeString</code> and initializes this wrapper's
    * {@link #instances} collection to take those attributes.
   **/
  public void initializeAttributes() {
    String[] atts = attributeString.split(":");

    for (int i = 0; i < atts.length; ++i) {
      String[] parts = atts[i].split("_");

      if (parts[0].equals("str")) {
        String attributeName = parts[1];
        Attribute newAttribute =
          new Attribute(attributeName, (FastVector) null);
        attributeInfo.addElement(newAttribute);
      }
      else if (parts[0].equals("nom")) {
        String[] valueStrings = parts[2].split(",");
        FastVector valueVector = new FastVector(valueStrings.length);
        for (int j = 0; j < valueStrings.length; ++j)
          valueVector.addElement(valueStrings[j]);

        Attribute a = new Attribute(parts[1], valueVector);
        attributeInfo.addElement(a);
      }
      else if (parts[0].equals("num")) {
        attributeInfo.addElement(new Attribute(parts[1]));
      }
      else {
        System.err.println(
            "WekaWrapper: Error - Malformed attribute information string: "
            + attributeString);
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

    instances = new Instances(name, attributeInfo, 0);
    instances.setClassIndex(0);
  }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(LBJ2.classify.Classifier l) {
    super.setLabeler(l);
    allowableValues = l == null ? null : l.allowableValues();
  }


  /**
    * Returns the array of allowable values that a feature returned by this
    * classifier may take.
    *
    * @return The allowable values of this learner's labeler, or an array of
    *         length zero if the labeler has not yet been established or does
    *         not specify allowable values.
   **/
  public String[] allowableValues() {
    if (allowableValues == null) return new String[0];
    return allowableValues;
  }


  /**
    * Since WEKA classifiers cannot learn online, this method causes no actual
    * learning to occur, it simply creates an <code>Instance</code> object
    * from this example and adds it to a set of examples from which the
    * classifier will be built once {@link #doneLearning()} is called.
   **/
  public void learn(int[] exampleFeatures, double[] exampleValues,
                    int[] exampleLabels, double[] labelValues) {
    instances.add(makeInstance(exampleFeatures, exampleValues,
                               exampleLabels, labelValues));
  }


  /**
    * This method makes one or more decisions about a single object, returning
    * those decisions as Features in a vector.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return A feature vector with a single feature containing the prediction
    *         for this example.
   **/
  public FeatureVector classify(int[] exampleFeatures, double[] exampleValues)
  {
    if (!trained) {
      System.err.println(
          "WekaWrapper: Error - Cannot make a classification with an "
        + "untrained classifier.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    /*
      Assuming that the first Attribute in our attributeInfo vector is the
      class attribute, decide which case we are in
    */
    Attribute classAtt = (Attribute) attributeInfo.elementAt(0);

    if (classAtt.isNominal() || classAtt.isString()) {
      double[] dist = getDistribution(exampleFeatures, exampleValues);
      int best = 0;
      for (int i = 1; i < dist.length; ++i)
        if (dist[i] > dist[best]) best = i;

      Feature label = labelLexicon.lookupKey(best);
      if (label == null) return new FeatureVector();
      String value = label.getStringValue();

      return
        new FeatureVector(
            new DiscretePrimitiveStringFeature(
                  containingPackage, name, "", value, valueIndexOf(value),
                  (short) allowableValues().length));
    }
    else if (classAtt.isNumeric()) {
      return
        new FeatureVector(
            new RealPrimitiveStringFeature(
                  containingPackage, name, "",
                  getDistribution(exampleFeatures, exampleValues)[0]));
    }
    else {
      System.err.println("WekaWrapper: Error - illegal class type.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return new FeatureVector();
  }


  /**
    * Returns a discrete distribution of the classifier's prediction values.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
   **/
  protected double[] getDistribution(int[] exampleFeatures,
                                     double[] exampleValues) {
    if (!trained) {
      System.err.println(
          "WekaWrapper: Error - Cannot make a classification with an "
        + "untrained classifier.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    Instance inQuestion = makeInstance(exampleFeatures, exampleValues,
                                       new int[0], new double[0]);

    /*
      For Numerical class values, this will return an array of size 1,
      containing the class prediction.
      For Nominal classes, an array of size equal to that of the class list,
      representing probabilities.
      For String classes, ?
    */
    double[] dist = null;
    try { dist = baseClassifier.distributionForInstance(inQuestion); }
    catch (Exception e) {
      System.err.println("WekaWrapper: Error while computing distribution.");
      e.printStackTrace();
      System.exit(1);
    }

    if (dist.length == 0) {
      System.err.println(
          "WekaWrapper: Error - The base classifier returned an empty "
        + "probability distribution when attempting to classify an "
        + "example.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return dist;
  }


  /**
    * Destroys the learned version of the WEKA classifier and empties the
    * {@link #instances} collection of examples.
   **/
  public void forget() {
    super.forget();

    try { baseClassifier = (AbstractClassifier) AbstractClassifier.makeCopy(freshClassifier); }
    catch (Exception e) {
      System.err.println(
          "LBJ ERROR: WekaWrapper.forget: Can't copy classifier:");
      e.printStackTrace();
      System.exit(1);
    }

    instances = new Instances(name, attributeInfo, 0);
    instances.setClassIndex(0);
    trained = false;
  }


  /**
    * Creates a WEKA Instance object out of a {@link FeatureVector}.
   **/
  private Instance makeInstance(int[] exampleFeatures, double[] exampleValues,
                                int[] exampleLabels, double[] labelValues) {
    // Make sure attributeInfo has been filled
    if (attributeInfo.size() == 0) {
      System.err.println(
          "WekaWrapper: Error - makeInstance was called while attributeInfo "
        + "was empty.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    // Initialize an Instance object
    Instance inst = new DenseInstance(attributeInfo.size());

    // Acknowledge that this instance will be a member of our dataset
    // 'instances'
    inst.setDataset(instances);

    // Assign values for its attributes
    /*
      Since we are iterating through this example's feature list, which does
      not contain the label feature (the label feature is the first in the
      'attribute' list), we start attIndex at 1, while we start featureIndex
      at 0.
    */
    for (int featureIndex = 0, attIndex = 1;
         featureIndex < exampleFeatures.length; ++featureIndex, ++attIndex) {
      Feature f = (Feature) lexicon.lookupKey(exampleFeatures[featureIndex]);
      Attribute att = (Attribute) attributeInfo.elementAt(attIndex);

      // make sure the feature's identifier and the attribute's name match
      if (!(att.name().equals(f.getStringIdentifier()))) {
        System.err.println(
            "WekaWrapper: Error - makeInstance encountered a misaligned "
          + "attribute-feature pair.");
        System.err.println(
            "  " + att.name() + " and " + f.getStringIdentifier()
            + " should have been identical.");
        new Exception().printStackTrace();
        System.exit(1);
      }

      if (!f.isDiscrete())
        inst.setValue(attIndex, exampleValues[featureIndex]);
      else {  // it's a discrete or conjunctive feature.
        String attValue =
          f.totalValues() == 2 ? att.value((int) exampleValues[featureIndex])
                               : f.getStringValue();
        inst.setValue(attIndex, attValue);
      }
    }

    /*
      Here, we assume that if either the labels FeatureVector is empty
      of features, or is null, then this example is to be considered
      unlabeled.
    */
    if (exampleLabels.length == 0) {
      inst.setClassMissing();
    }
    else if (exampleLabels.length > 1) {
      System.err.println(
          "WekaWrapper: Error - Weka Instances may only take a single class "
        + "value, ");
      new Exception().printStackTrace();
      System.exit(1);
    }
    else {
      Feature label = labelLexicon.lookupKey(exampleLabels[0]);

      // make sure the name of the label feature matches the name of the 0'th
      // attribute
      if (!(label.getStringIdentifier()
            .equals(((Attribute) attributeInfo.elementAt(0)).name()))) {
        System.err.println(
            "WekaWrapper: Error - makeInstance found the wrong label name.");
        new Exception().printStackTrace();
        System.exit(1);
      }

      if (!label.isDiscrete()) inst.setValue(0, labelValues[0]);
      else inst.setValue(0, label.getStringValue());
    }

    return inst;
  }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues) {
    double[] dist = getDistribution(exampleFeatures, exampleValues);

    /*
      Assuming that the first Attribute in our attributeInfo vector is the
      class attribute, decide which case we are in
    */
    Attribute classAtt = (Attribute) attributeInfo.elementAt(0);

    ScoreSet scores = new ScoreSet();

    if (classAtt.isNominal() || classAtt.isString()) {
      Enumeration enumeratedValues = classAtt.enumerateValues();

      int i = 0;
      while (enumeratedValues.hasMoreElements()) {
        if (i >= dist.length) {
          System.err.println(
              "WekaWrapper: Error - scores found more possible values than "
            + "probabilities.");
          new Exception().printStackTrace();
          System.exit(1);
        }
        double s = dist[i];
        String v = (String) enumeratedValues.nextElement();
        scores.put(v,s);
        ++i;
      }
    }
    else if (classAtt.isNumeric()) {
      System.err.println(
          "WekaWrapper: Error - The 'scores' function should not be called "
        + "when the class attribute is numeric.");
      new Exception().printStackTrace();
      System.exit(1);
    }
    else {
      System.err.println(
          "WekaWrapper: Error - ScoreSet: Class Types must be either "
        + "Nominal, String, or Numeric.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return scores;
  }


  /**
    * Indicates that the classifier is finished learning.  This method
    * <I>must</I> be called if the WEKA classifier is to learn anything.
    * Since WEKA classifiers cannot learn online, all of the training examples
    * must be gathered and committed to first.  This method invokes the WEKA
    * classifier's <code>buildClassifier(Instances)</code> method.
   **/
  public void doneLearning() {
    if (trained) {
      System.err.println(
          "WekaWrapper: Error - Cannot call 'doneLearning()' again without "
        + "first calling 'forget()'");
      new Exception().printStackTrace();
      System.exit(1);
    }

    /*
    System.out.println("\nWekaWrapper Data Summary:");
    System.out.println(instances.toSummaryString());
    */

    try { baseClassifier.buildClassifier(instances); }
    catch (Exception e) {
      System.err.println(
          "WekaWrapper: Error - There was a problem building the classifier");
      if (baseClassifier == null)
        System.out.println("WekaWrapper: baseClassifier was null.");
      e.printStackTrace();
      System.exit(1);
    }

    trained = true;
    instances = new Instances(name, attributeInfo, 0);
    instances.setClassIndex(0);
  }


  /**
    * Writes the settings of the classifier in use, and a string describing
    * the classifier, if available.
   **/
  public void write(PrintStream out) {
    out.print(name + ": ");
    String[] options = baseClassifier.getOptions();
    for (int i = 0; i < options.length; ++i)
      out.println(options[i]);
    out.println(baseClassifier);
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeBoolean(trained);

    if (allowableValues == null) out.writeInt(0);
    else {
      out.writeInt(allowableValues.length);
      for (int i = 0; i < allowableValues.length; ++i)
        out.writeString(allowableValues[i]);
    }

    ObjectOutputStream oos = null;
    try { oos = new ObjectOutputStream(out); }
    catch (Exception e) {
      System.err.println("Can't create object stream for '" + name + "': "
                         + e);
      System.exit(1);
    }

    try {
      oos.writeObject(baseClassifier);
      oos.writeObject(freshClassifier);
      oos.writeObject(attributeInfo);
      oos.writeObject(instances);
    }
    catch (Exception e) {
      System.err.println("Can't write to object stream for '" + name + "': "
                         + e);
      System.exit(1);
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
    trained = in.readBoolean();
    allowableValues = new String[in.readInt()];
    for (int i = 0; i < allowableValues.length; ++i)
      allowableValues[i] = in.readString();

    ObjectInputStream ois = null;
    try { ois = new ObjectInputStream(in); }
    catch (Exception e) {
      System.err.println("Can't create object stream for '" + name + "': "
                         + e);
      System.exit(1);
    }

    try {
      baseClassifier = (AbstractClassifier) ois.readObject();
      freshClassifier = (Classifier) ois.readObject();
      attributeInfo = (FastVector) ois.readObject();
      instances = (Instances) ois.readObject();
    }
    catch (Exception e) {
      System.err.println("Can't read from object stream for '" + name + "': "
                         + e);
      System.exit(1);
    }
  }


  /**
    * Simply a container for all of {@link WekaWrapper}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends Learner.Parameters
  {
    /**
      * Stores the instance of the WEKA classifier which we are training;
      * default {@link WekaWrapper#defaultBaseClassifier}.
     **/
    public AbstractClassifier baseClassifier;
    /**
      * A string encoding of the return types of each of the feature
      * extractors in use; default {@link WekaWrapper#defaultAttributeString}.
     **/
    public String attributeString;


    /** Sets all the default values. */
    public Parameters() {
      baseClassifier = defaultBaseClassifier;
      attributeString = defaultAttributeString;
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(Learner.Parameters p) {
      super(p);
      baseClassifier = defaultBaseClassifier;
      attributeString = defaultAttributeString;
    }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      super(p);
      baseClassifier = p.baseClassifier;
      attributeString = p.attributeString;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((WekaWrapper) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();

      if (!attributeString.equals(WekaWrapper.defaultAttributeString))
        result += ", attributeString = \"" + attributeString + "\"";

      if (result.startsWith(", ")) result = result.substring(2);
      return result;
    }
  }
}

