package LBJ2.learn;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

import LBJ2.classify.Classifier;
import LBJ2.classify.DiscretePrimitiveStringFeature;
import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.ScoreSet;
import LBJ2.util.ByteString;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.FVector;
import LBJ2.util.IVector;
import LBJ2.util.OVector;

/**
  * Wrapper class for the
  * <a href="http://www.csie.ntu.edu.tw/~cjlin/liblinear">
  * <code>liblinear</code> library</a> which supports support vector machine
  * classification.  That library must be downloaded separately and placed on
  * your <code>CLASSPATH</code> for this class to work correctly.  This class
  * can perform both binary classification and multi-class classification.  It
  * is assumed that {@link Learner#labeler} is a single discrete classifier
  * that produces the same feature for every example object.  Assertions will
  * produce error messages if this assumption does not hold.
  *
  * <p> When calling this algorithm in a <code>with</code> clause inside an
  * LBJ source file, there is no need to specify the <code>rounds</code>
  * clause.  At runtime, calling {@link #learn(Object)} merely performs
  * feature extraction and stores an indexed representation of the example
  * vector in memory.  The learning algorithm executes when
  * {@link #doneLearning()} is called.  This call also frees the memory in
  * which the example vectors are stored.  Thus, subsequent calls to
  * {@link #learn(Object)} and {@link #doneLearning()} will discard the
  * previous hypothesis and learn an entirely new one.
  *
  * <p> <code>liblinear</code> performs binary classification (as opposed to
  * 1-vs.-all) whenever the solver type is <b>not</b> <code>MCSVM_CS</code>
  * and exactly two class labels are observed in the training data.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.SupportVectorMachine.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.SupportVectorMachine.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Michael Paul
 **/
public class SupportVectorMachine extends Learner
{
  /** Default for {@link #solverType}. */
  public static final String defaultSolverType = "L2LOSS_SVM";
  /** Default for {@link #C}. */
  public static final double defaultC = 1.0;
  /** Default for {@link #epsilon}. */
  public static final double defaultEpsilon = 0.1;
  /** Default for {@link #bias}. */
  public static final double defaultBias = 1.0;

  /**
    * Keeps track of whether the doneLearning() warning message has been
    * printed.
   **/
  private boolean warningPrinted;

  /**
    * The type of solver; default {@link #defaultSolverType} unless there
    * are more than 2 labels observed in the training data, in which case
    * "MCSVM_CS" becomes the default. Note that if you are doing multi-class
    * classification, you can still override the "MCSVM_CS" default to use
    * another solver type.
    *
    * <p> Possible values:
    * <ul>
    *   <li> <code>"L2_LR"</code> = L2-regularized logistic regression;
    *   <li> <code>"L2LOSS_SVM_DUAL"</code> = L2-loss support vector machines
    *        (dual);
    *   <li> <code>"L2LOSS_SVM"</code> = L2-loss support vector machines
    *        (primal);
    *   <li> <code>"L1LOSS_SVM_DUAL"</code> = L1-loss support vector machines
    *        (dual);
    *   <li> <code>"MCSVM_CS"</code> = multi-class support vector machines by
    *        Crammer and Singer
    * </ul>
   **/
  protected String solverType;

  /**
    * The cost parameter C; default {@link #defaultC}
   **/
  protected double C;

  /**
    * The tolerance of termination criterion;
    * default {@link #defaultEpsilon}.
   **/
  protected double epsilon;

  /**
    * If {@link #bias} &gt;= 0, an instance vector x becomes [x; bias];
    * otherwise, if {@link #bias} &lt; 0, no bias term is added.
   **/
  protected double bias;
  /** The number of bias features; there are either 0 or 1 of them. */
  protected int biasFeatures;

  /** Controls if <code>liblinear</code>-related messages are output */
  protected boolean displayLL = false;

  /** The number of unique class labels seen during training. */
  protected int numClasses;
  /** The number of unique features seen during training. */
  protected int numFeatures;
  /** Whether or not this learner's labeler produces conjunctive features. */
  protected boolean conjunctiveLabels;

  /**
    * An array of weights representing the weight vector learned
    * after training with <code>liblinear</code>.
   **/
  protected double[] weights;

  /** The array of example labels */
  protected IVector allLabels;
  /** The array of example vectors. */
  protected OVector allExamples;

  /** The label producing classifier's allowable values. */
  protected String[] allowableValues;

  /**
    * Created during {@link #doneLearning()} in case the training examples
    * observed by {@link #learn(int[],double[],int[],double[])} are only a
    * subset of a larger, pre-extracted set.  If this is not the case, it
    * will simply be a duplicate reference to {@link #labelLexicon}.
   **/
  protected Lexicon newLabelLexicon;


  /**
    * Default constructor.  C, epsilon, the bias, and the solver type
    * take the default values while the name of the classifier
    * gets the empty string.
   **/
  public SupportVectorMachine() { this(""); }

  /**
    * Initializing constructor. The name of the classifier gets
    * the empty string.
    *
    * @param c  The desired C value.
   **/
  public SupportVectorMachine(double c) { this(c, defaultEpsilon); }

  /**
    * Initializing constructor. The name of the classifier gets
    * the empty string.
    *
    * @param c  The desired C value.
    * @param e  The desired epsilon value.
   **/
  public SupportVectorMachine(double c, double e) { this(c, e, defaultBias); }

  /**
    * Initializing constructor. The name of the classifier gets
    * the empty string.
    *
    * @param c  The desired C value.
    * @param e  The desired epsilon value.
    * @param b  The desired bias.
   **/
  public SupportVectorMachine(double c, double e, double b) {
    this(c, e, b, "");
  }

  /**
    * Initializing constructor. The name of the classifier gets
    * the empty string.
    *
    * @param c  The desired C value.
    * @param e  The desired epsilon value.
    * @param b  The desired bias.
    * @param s  The solver type.
   **/
  public SupportVectorMachine(double c, double e, double b, String s) {
    this("", c, e, b, s, false);
  }

  /**
    * Initializing constructor. The name of the classifier gets
    * the empty string.
    *
    * @param c  The desired C value.
    * @param e  The desired epsilon value.
    * @param b  The desired bias.
    * @param s  The solver type.
    * @param d  Toggles if the <code>liblinear</code>-related output should be
    *           displayed.
   **/
  public SupportVectorMachine(double c, double e, double b, String s,
                              boolean d) {
    this("", c, e, b, s, d);
  }

  /**
    * Initializing constructor.  C, epsilon, the bias, and the solver type
    * take the default values.
    *
    * @param n  The name of the classifier.
   **/
  public SupportVectorMachine(String n) {
    this(n, new Parameters());
  }

  /**
    * Initializing constructor.
    *
    * @param n  The name of the classifier.
    * @param c  The desired C value.
   **/
  public SupportVectorMachine(String n, double c) {
    this(n, c, defaultEpsilon);
  }

  /**
    * Initializing constructor.
    *
    * @param n  The name of the classifier.
    * @param c  The desired C value.
    * @param e  The desired epsilon value.
   **/
  public SupportVectorMachine(String n, double c, double e) {
    this(n, c, e, defaultBias);
  }

  /**
    * Initializing constructor.
    *
    * @param n  The name of the classifier.
    * @param c  The desired C value.
    * @param e  The desired epsilon value.
    * @param b  The desired bias.
   **/
  public SupportVectorMachine(String n, double c, double e, double b) {
    this(n, c, e, b, "");
  }

  /**
    * Initializing constructor.
    *
    * @param n  The name of the classifier.
    * @param c  The desired C value.
    * @param e  The desired epsilon value.
    * @param b  The desired bias.
    * @param s  The solver type.
   **/
  public SupportVectorMachine(String n, double c, double e, double b,
                              String s) {
    this(n, c, e, b, s, false);
  }

  /**
    * Initializing constructor.
    *
    * @param n  The name of the classifier.
    * @param c  The desired C value.
    * @param e  The desired epsilon value.
    * @param b  The desired bias.
    * @param s  The solver type.
    * @param d  Toggles if the <code>liblinear</code>-related output should be
    *           displayed.
   **/
  public SupportVectorMachine(String n, double c, double e, double b,
                              String s, boolean d) {
    super(n);
    newLabelLexicon = labelLexicon;
    Parameters p = new Parameters();
    p.C = c;
    p.epsilon = e;
    p.bias = b;
    p.solverType = s;
    p.displayLL = d;
    allowableValues = new String[0];
    setParameters(p);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SupportVectorMachine.Parameters} object.
    * The name of the classifier gets the empty string.
    *
    * @param p  The settings of all parameters.
   **/
  public SupportVectorMachine(Parameters p) { this("", p); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SupportVectorMachine.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SupportVectorMachine(String n, Parameters p) {
    super(n);
    newLabelLexicon = labelLexicon;
    allowableValues = new String[0];
    setParameters(p);
  }


  /**
    * Sets the values of parameters that control the behavior of this learning
    * algorithm.
    *
    * @param p  The parameters.
   **/
  public void setParameters(Parameters p) {
    C = p.C;
    epsilon = p.epsilon;
    bias = p.bias;
    biasFeatures = (bias >= 0) ? 1 : 0;
    solverType = p.solverType;
    displayLL = p.displayLL;
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p = new Parameters(super.getParameters());
    p.C = C;
    p.epsilon = epsilon;
    p.bias = bias;
    p.solverType = solverType;
    p.displayLL = displayLL;
    return p;
  }


  /**
    * Sets the labels list.
    *
    * @param l  A new label producing classifier.
   **/
  public void setLabeler(Classifier l) {
    super.setLabeler(l);
    allowableValues = l == null ? null : l.allowableValues();
    if (allowableValues == null) allowableValues = new String[0];
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
    * Initializes the example vector arrays.
    *
    * @param ne   The number of examples to train.
    * @param nf   The number of features.
   **/
  public void initialize(int ne, int nf) {
    allLabels = new IVector(ne);
    allExamples = new OVector(ne);
  }


  /**
    * This method adds the example's features and labels to the arrays storing
    * the training examples.  These examples will eventually be passed to
    * <code>Linear.train()</code> for training.
    *
    * <p> Note that learning via the <code>liblinear</code> library does not
    * actually take place until {@link #doneLearning()} is called.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @param exampleLabels    The example's array of label indices.
    * @param labelValues      The example's array of label values.
   **/
  public void learn(final int[] exampleFeatures, double[] exampleValues,
                    int[] exampleLabels, double[] labelValues) {
    // Expand the size of the example arrays if they are full.

    if (allLabels == null || allLabels.size() == 0) {
      if (allLabels == null) {
        allLabels = new IVector();
        allExamples = new OVector();
      }
      weights = null;
      warningPrinted = false;
    }

    // Add the label to the examples array.

    assert exampleLabels.length == 1
      : "Example must have a single label.";

    allLabels.add(exampleLabels[0]);

    // Add the example to the examples list.  Space for the bias feature is
    // allocated, but it isn't instantiated because we don't know its index
    // yet.

    int F = exampleFeatures.length;
    FeatureNode[] liblinearExample = new FeatureNode[F + biasFeatures];
    allExamples.add(liblinearExample);

    for (int i = 0; i < F; ++i) {
      int featureIndex = exampleFeatures[i] + 1;
      numFeatures = Math.max(numFeatures, featureIndex);
      liblinearExample[i] = new FeatureNode(featureIndex, exampleValues[i]);
    }

    Arrays.sort(liblinearExample, 0, F,
                new Comparator() {
                  public int compare(Object o1, Object o2) {
                    FeatureNode f1 = (FeatureNode) o1;
                    FeatureNode f2 = (FeatureNode) o2;
                    return f1.index - f2.index;
                  }
                });

    // Check for duplicate features.  If there are any, add up all strengths
    // corresponding to a given feature index and put them in a single
    // feature.

    int previousI = -1;
    int realCount = F;

    for (int i = 0; i < F; i++) {
      int f = liblinearExample[i].index;

      if (previousI != -1 && f == liblinearExample[previousI].index) {
        realCount--;
        liblinearExample[previousI] =
          new FeatureNode(f,
                          liblinearExample[previousI].value
                          + liblinearExample[i].value);
        liblinearExample[i] = null;
      }
      else previousI = i;
    }

    // If duplicate features were observed, rebuild the example array without
    // the duplicates.

    if (realCount < F) {
      FeatureNode[] temp = new FeatureNode[realCount + biasFeatures];
      int k = 0;
      for (int i = 0; i < F; i++)
        if (liblinearExample[i] != null)
          temp[k++] = liblinearExample[i];
      allExamples.set(allExamples.size() - 1, temp);
    }
  }


  /**
    * This method converts the arrays of examples stored in this class
    * into input for the <code>liblinear</code> training method.
    * The learned weight vector is stored in {@link #weights}.
   **/
  public void doneLearning() {
    super.doneLearning();

    // Create the new lexicon of labels given the examples seen during
    // training.  This is necessary when doing cross-validation, where the
    // supplied lexicon is based on all of the examples and so the lexicon
    // might not match up with the subset of examples seen during the current
    // fold.

    // liblinear expects that it sees y labels in increasing order, which
    // might not be the case during some folds of cross-validation, where the
    // label lexicon is not created during the particular fold.  liblinear
    // also only allocates space in the weight vector for the labels it
    // encounters during training -- if the label lexicon contains more labels
    // than what are observed during training, then liblinear's label
    // representation will not match up with our label lexicon.  Thus,
    // creating a new lexicon here solves both of these problems.

    if (labelLexicon.size() > 2 || solverType.equals("MCSVM_CS")) {
      newLabelLexicon = new Lexicon();
      boolean same = true;
      for (int i = 0; i < allExamples.size(); i++) {
        Feature label = labelLexicon.lookupKey(allLabels.get(i));
        int newLabel = newLabelLexicon.lookup(label, true);
        same &= newLabel == allLabels.get(i);
        allLabels.set(i, newLabel);
      }

      if (same && newLabelLexicon.size() == labelLexicon.size())
        newLabelLexicon = labelLexicon;
      else if (newLabelLexicon.size() > labelLexicon.size()) {
        System.err.println(
            "LBJ ERROR: SupportVectorMachine: new label lexicon is too big!");
        new Exception().printStackTrace();
        System.exit(1);
      }
      else {
        int N = newLabelLexicon.size();
        predictions = new FVector(N);
        for (int i = 0; i < N; ++i)
          createPrediction(newLabelLexicon, i);
      }
    }

    if (displayLL)
      System.out.println("  Training via liblinear at " + new Date());
    if (allLabels == null) {
      if (displayLL) {
        System.out.println("    No training examples; no action taken.");
        System.out.println("  Finished training at " + new Date());
      }
      return;
    }

    if (solverType.length() == 0) solverType = defaultSolverType;
    numClasses = newLabelLexicon.size();
    for (int i = 0; i < numClasses && !conjunctiveLabels; ++i)
      conjunctiveLabels = newLabelLexicon.lookupKey(i).isConjunctive();

    int l = allExamples.size(); // number of examples
    int n = numFeatures + biasFeatures; // number of features

    if (biasFeatures == 1)
      for (int i = 0; i < l; i++) {
        FeatureNode[] ex = (FeatureNode[]) allExamples.get(i);
        ex[ex.length - 1] = new FeatureNode(n, bias);
      }

    // In the binary case, liblinear will consider the integer label it sees
    // on the first example to represent "positive".  We need the string in
    // allowableValues[1] to mean "positive".
    boolean fixLabels =
      !solverType.equals("MCSVM_CS") && numClasses == 2
      && allowableValues.length == 2;

    if (l > 0 && fixLabels) {
      Feature f =
        new DiscretePrimitiveStringFeature(
              labeler.containingPackage, labeler.name, "", allowableValues[1],
              (short) 1, (short) 2);
      int p = newLabelLexicon.lookup(f);
      int positive = 0;

      while (positive < l && allLabels.get(positive) == 1 - p) ++positive;
      if (positive > 0 && positive < l) {
        allLabels.set(0, p);
        allLabels.set(positive, 1 - p);
        allExamples.set(0, allExamples.set(positive, allExamples.get(0)));

        newLabelLexicon = new Lexicon();
        newLabelLexicon.lookup(f, true);
        newLabelLexicon.lookup(
            new DiscretePrimitiveStringFeature(
                  labeler.containingPackage, labeler.name, "",
                  allowableValues[0], (short) 0, (short) 2),
            true);
        predictions = new FVector(2);
        createPrediction(newLabelLexicon, 0);
        createPrediction(newLabelLexicon, 1);
      }
    }

    Problem prob = new Problem();
    prob.bias = bias;
    prob.l = l;
    prob.n = n;
    prob.x = new FeatureNode[l][];
    for (int i = 0; i < l; ++i)
      prob.x[i] = (FeatureNode[]) allExamples.get(i);
    for(int i=0; i < allLabels.size(); i++)
    	prob.y[i] = allLabels.get(i);

    Parameter params =
      new Parameter(Parameters.getSolverType(solverType), C,
                              epsilon);

    Model trainedModel = Linear.train(prob, params);
    weights = trainedModel.getFeatureWeights();
    allExamples = null;
    allLabels = null;

    if (displayLL)
      System.out.println("  Finished training at " + new Date());
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #C}, {@link #epsilon},
    * {@link #bias}, and finally {@link #solverType}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    demandLexicon();
    out.println(name + ": " + C + ", " + epsilon + ", "
                + bias + ", " + solverType);

    if (weights != null) {
      out.println();

      out.println("Feature weights:");
      out.println("=========================================");

      int F = numFeatures;
      if (bias >= 0) F++;

      // only display one weight vector if binary solver
      if (!solverType.equals("MCSVM_CS") && numClasses <= 2) numClasses = 1;

      for (int c = 0; c < numClasses; c++) {
        if (numClasses > 1) {
          String className = newLabelLexicon.lookupKey(c).getStringValue();
          out.println("Class = " + className);
        }

        for (int f = 0; f < F; f++) {
          if (f < numFeatures) out.print(lexicon.lookupKey(f));
          else out.print("[bias]");
          double weight = weights[f*numClasses + c];
          out.println("\t\t\t" + weight);
        }
      }
      out.println("=========================================");
    }

    out.println("End of SupportVectorMachine");
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeString(solverType);
    out.writeDouble(C);
    out.writeDouble(epsilon);
    out.writeDouble(bias);
    out.writeBoolean(displayLL);
    out.writeInt(numClasses);
    out.writeInt(numFeatures);
    out.writeBoolean(conjunctiveLabels);

    out.writeInt(allowableValues.length);
    for (int i = 0; i < allowableValues.length; ++i)
      out.writeString(allowableValues[i]);

    if (newLabelLexicon == labelLexicon) out.writeBoolean(false);
    else {
      out.writeBoolean(true);
      newLabelLexicon.write(out);
    }

    if (weights == null) out.writeInt(0);
    else {
      out.writeInt(weights.length);
      for (int i = 0; i < weights.length; ++i)
        out.writeDouble(weights[i]);
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
    solverType = in.readString();
    C = in.readDouble();
    epsilon = in.readDouble();
    bias = in.readDouble();
    biasFeatures = (bias >= 0) ? 1 : 0;
    displayLL = in.readBoolean();
    numClasses = in.readInt();
    numFeatures = in.readInt();
    conjunctiveLabels = in.readBoolean();

    int N = in.readInt();
    allowableValues = new String[N];
    for (int i = 0; i < N; ++i)
      allowableValues[i] = in.readString();

    if (in.readBoolean()) newLabelLexicon = Lexicon.readLexicon(in);
    else newLabelLexicon = labelLexicon;

    N = in.readInt();
    weights = new double[N];
    for (int i = 0; i < N; ++i)
      weights[i] = in.readDouble();
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
    if (weights == null && allLabels != null && !warningPrinted) {
      System.err.println(
          "LBJ WARNING: SupportVectorMachine's doneLearning() method should "
          + "be called before attempting to make predictions.");
      warningPrinted = true;
    }

    if (weights == null) return null;
    double bestScore = Double.NEGATIVE_INFINITY;
    int prediction = 0;

    if (numClasses > 2 || solverType.equals("MCSVM_CS")) {
      for (int c = 0; c < numClasses; c++) {
        double s = score(f, v, c);

        if (s > bestScore) {
          bestScore = s;
          prediction = c;
        }
      }
    }
    else {
      double s = score(f, v, 0);
      if (s < 0) prediction = 1;
    }

    return predictions.get(prediction);
  }


  /**
    * The evaluate method returns the class label which yields the
    * highest score for this example.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @return The computed feature (in a vector).
   **/
  public String discreteValue(int[] exampleFeatures, double[] exampleValues) {
    return featureValue(exampleFeatures, exampleValues).getStringValue();
  }


  /**
    * Evaluates the given example using <code>liblinear</code>'s prediction
    * method.  Returns a {@link DiscretePrimitiveStringFeature} set to the
    * label value.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
   **/
  public FeatureVector classify(int[] exampleFeatures, double[] exampleValues)
  {
    return new FeatureVector(featureValue(exampleFeatures, exampleValues));
  }


  /**
    * An SVM returns a classification score for each class.  The score for
    * each class is the result of {@link #score(int[],double[],int)}.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @return The set of scores as described above.
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues) {
    // score the example and save the results
    ScoreSet result = new ScoreSet();

    if (weights == null) {
      if (allLabels != null && !warningPrinted) {
        System.err.println(
          "LBJ WARNING: SupportVectorMachine's doneLearning() method should "
          + "be called before attempting to make predictions.");
        warningPrinted = true;
      }

      return result;
    }

    if (numClasses > 2 || solverType.equals("MCSVM_CS")) {
      for (int c = 0; c < numClasses; c++) {
        String className = newLabelLexicon.lookupKey(c).getStringValue();
        double s = score(exampleFeatures, exampleValues, c);
        result.put(className, s);
      }
    }
    else {
      String className = newLabelLexicon.lookupKey(0).getStringValue();
      double s = score(exampleFeatures, exampleValues, 0);
      result.put(className, s);
      className = newLabelLexicon.lookupKey(1).getStringValue();
      result.put(className, -s);
    }

    return result;
  }


  /**
    * Computes the dot product of the specified example vector
    * and the weight vector associated with the supplied class.
    * If no label is specified, it defaults to a label of 0
    * (that is, a positive example), but this should only be done
    * in binary classification.
    *
    * @param example  The example object.
    * @return The score for the given example vector.
   **/
  public double score(Object example) {
    assert !solverType.equals("MCSVM_CS") && numClasses == 2
        : "Cannot call score(Object) in a multi-class classifier.";

    return score(example, 0);
  }


  /**
    * Computes the dot product of the specified example vector
    * and the weight vector associated with the supplied class.
    *
    * @param example  The example object.
    * @param label    The class label
    * @return The score for the given example vector.
   **/
  public double score(Object example, int label) {
    Object[] exampleArray = getExampleArray(example, false);
    return score((int[]) exampleArray[0], (double[]) exampleArray[1], label);
  }


  /**
    * Computes the dot product of the specified feature vector
    * and the weight vector associated with the supplied class.
    *
    * @param exampleFeatures  The example's array of feature indices
    * @param exampleValues    The example's array of feature values
    * @param label            The class label
    * @return The score for the given example vector.
   **/
  public double score(int[] exampleFeatures, double[] exampleValues,
                      int label) {
    assert exampleFeatures.length == exampleValues.length
        : "Array mismatch; improperly formatted input.";

    double s = 0;

    if (weights == null) {
      if (allLabels != null && !warningPrinted) {
        System.err.println(
          "LBJ WARNING: SupportVectorMachine's doneLearning() method should "
          + "be called before attempting to make predictions.");
        warningPrinted = true;
      }

      return 0;
    }

    // If binary classification, no special offset for the weight vector.
    // Negate the final score if it is a negative example
    boolean negate = false;
    if (!(numClasses > 2 || solverType.equals("MCSVM_CS"))) {
      if (label == 1) negate = true;

      numClasses = 1;
      label = 0;
    }

    for (int i = 0; i < exampleFeatures.length; i++) {
      int f = exampleFeatures[i];

      if (f < numFeatures) {
        double value = exampleValues[i];
        double weight = weights[f*numClasses + label];

        s += weight*value;
      }
    }

    if (bias >= 0) s += bias * weights[numFeatures*numClasses + label];

    return negate ? -s : s;
  }


  /**
    * Using this method, the winner-take-all competition is narrowed to
    * involve only those labels contained in the specified list.  The list
    * must contain only {@link ByteString}s.
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
    if (weights == null && allLabels != null && !warningPrinted) {
      System.err.println(
          "LBJ WARNING: SupportVectorMachine's doneLearning() method should "
          + "be called before attempting to make predictions.");
      warningPrinted = true;
    }

    if (weights == null) return null;

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
                valueIndexOf(label), (short) allowableValues.length);

        int key = -1;
        if (newLabelLexicon.contains(f)) {
          key = newLabelLexicon.lookup(f);
          score = score(exampleFeatures, exampleValues, key);
        }

        if (score > bestScore) {
          bestValue = key;
          bestScore = score;
        }
      }
    }
    else {
      for (int l = 0; l < numClasses; l++) {
        double score = score(exampleFeatures, exampleValues, l);
        if (score > bestScore) {
          bestValue = l;
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
  protected Feature conjunctiveValueOf(int[] exampleFeatures,
                                       double[] exampleValues, Iterator I) {
    double bestScore = Double.NEGATIVE_INFINITY;
    int bestValue = -1;

    while (I.hasNext()) {
      String label = (String) I.next();

      for (int i = 0; i < numClasses; ++i) {
        if (!labelLexicon.lookupKey(i).valueEquals(label)) continue;
        double score = score(exampleFeatures, exampleValues, i);
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
    * Resets the internal bookkeeping.
   **/
  public void forget() {
    super.forget();

    numClasses = numFeatures = 0;
    allLabels = null;
    allExamples = null;
    weights = null;
    conjunctiveLabels = false;
  }


  /**
    * A container for all of {@link SupportVectorMachine}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Michael Paul
   **/
  public static class Parameters extends Learner.Parameters
  {
    /**
      * The type of solver; default
      * {@link SupportVectorMachine#defaultSolverType}.
      *
      * <p> Possible values:
      * <ul>
      *   <li> "L2_LR" = L2-regularized logistic regression;
      *   <li> "L2LOSS_SVM_DUAL" = L2-loss support vector machines (dual);
      *   <li> "L2LOSS_SVM" = L2-loss support vector machines (primal);
      *   <li> "L1LOSS_SVM_DUAL" = L1-loss support vector machines (dual);
      *   <li> "MCSVM_CS" = multi-class support vector machines by Crammer and
      *        Singer
      * </ul>
     **/
    public String solverType;
    /**
      * The cost parameter C; default {@link SupportVectorMachine#defaultC}
     **/
    public double C;
    /**
      * The tolerance of termination criterion;
      * default {@link SupportVectorMachine#defaultEpsilon}.
     **/
    public double epsilon;
    /**
      * If {@link SupportVectorMachine#bias} &gt;= 0, an instance vector x
      * becomes [x; bias]; otherwise, if {@link SupportVectorMachine#bias}
      * &lt; 0, no bias term is added.
     **/
    public double bias;
    /**
      * Determines if <code>liblinear</code>-related output should be
      * displayed; default <code>false</code>
     **/
    public boolean displayLL;


    /** Sets all the default values. */
    public Parameters() {
      solverType = "";
      C = defaultC;
      epsilon = defaultEpsilon;
      bias = defaultBias;
      displayLL = false;
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(Learner.Parameters p) {
      super(p);
      solverType = "";
      C = defaultC;
      epsilon = defaultEpsilon;
      bias = defaultBias;
      displayLL = false;
    }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      super(p);
      solverType = p.solverType;
      C = p.C;
      epsilon = p.epsilon;
      bias = p.bias;
      displayLL = p.displayLL;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((SupportVectorMachine) l).setParameters(this);
    }


    /**
      * Converts the string representation of the solver type
      * into a <code>SolverType</code> object to be used
      * by <code>liblinear</code> during training.
      *
      * @param stype  The solver type string.
      * @return The corresponding <code>SolverType</code> object.
     **/
    public static SolverType getSolverType(String stype) {
      if (stype.equals("L2_LR"))
        return SolverType.L2R_LR;
      else if (stype.equals("L2LOSS_SVM_DUAL"))
        return SolverType.L2R_L2LOSS_SVC_DUAL;
      else if (stype.equals("L2LOSS_SVM"))
        return SolverType.L2R_L2LOSS_SVC;
      else if (stype.equals("L1LOSS_SVM_DUAL"))
        return SolverType.L1R_L2LOSS_SVC;
      else if (stype.equals("MCSVM_CS"))
        return SolverType.MCSVM_CS;
      else
        return SolverType.L2R_L2LOSS_SVC;
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();

      if (!solverType.equals(SupportVectorMachine.defaultSolverType))
        result += ", solverType = \"" + solverType + "\"";
      if (C != SupportVectorMachine.defaultC)
        result += ", C = " + C;
      if (epsilon != SupportVectorMachine.defaultEpsilon)
        result += ", epsilon = " + epsilon;
      if (bias != SupportVectorMachine.defaultBias)
        result += ", bias = " + bias;

      if (result.startsWith(", ")) result = result.substring(2);
      return result;
    }
  }
}

