package LBJ2.learn;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import LBJ2.classify.Classifier;
import LBJ2.classify.Feature;
import LBJ2.util.DVector;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * An approximation to voted Perceptron, in which a weighted average of the
  * weight vectors arrived at during training becomes the weight vector used
  * to make predictions after training.
  *
  * <p> During training, after each example <i>e<sub>i</sub></i> is processed,
  * the weight vector <i>w<sub>i</sub></i> becomes the active weight vector
  * used to make predictions on future training examples.  If a mistake was
  * made on <i>e<sub>i</sub></i>, <i>w<sub>i</sub></i> will be different than
  * <i>w<sub>i - 1</sub></i>.  Otherwise, it will remain unchanged.
  *
  * <p> After training, each distinct weight vector arrived at during training
  * is associated with an integer weight equal to the number of examples whose
  * training made that weight vector active.  A new weight vector
  * <i>w<sup>*</sup></i> is computed by taking the average of all these weight
  * vectors weighted as described.  <i>w<sup>*</sup></i> is used to make all
  * predictions returned to the user through methods such as
  * {@link Classifier#classify(Object)} or
  * {@link Classifier#discreteValue(Object)}.
  *
  * <p> The above description is a useful way to think about the operation of
  * this {@link Learner}.  However, the user should note that this
  * implementation never explicitly stores <i>w<sup>*</sup></i>.  Instead, it
  * is computed efficiently on demand.  Thus, interspersed online training and
  * evaluation is efficient and operates as expected.
  *
  * <p> It is assumed that {@link Learner#labeler} is a single discrete
  * classifier that produces the same feature for every example object and
  * that the values that feature may take are available through the
  * {@link Classifier#allowableValues()} method.  The second value returned
  * from {@link Classifier#allowableValues()} is treated as "positive", and it
  * is assumed there are exactly 2 allowable values.  Assertions will produce
  * error messages if these assumptions do not hold.
  *
  * @author Nick Rizzolo
 **/
public class SparseAveragedPerceptron extends SparsePerceptron
{
  /** Default for {@link LinearThresholdUnit#weightVector}. */
  public static final AveragedWeightVector defaultWeightVector =
    new AveragedWeightVector();

  /**
    * Holds the same reference as {@link LinearThresholdUnit#weightVector}
    * casted to {@link SparseAveragedPerceptron.AveragedWeightVector}.
   **/
  protected AveragedWeightVector awv;
  /** Keeps the extra information necessary to compute the averaged bias. */
  protected double averagedBias;


  /**
    * The learning rate and threshold take default values, while the name of
    * the classifier gets the empty string.
   **/
  public SparseAveragedPerceptron() { this(""); }

  /**
    * Sets the learning rate to the specified value, and the threshold takes
    * the default, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
   **/
  public SparseAveragedPerceptron(double r) { this("", r); }

  /**
    * Sets the learning rate and threshold to the specified values, while the
    * name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
   **/
  public SparseAveragedPerceptron(double r, double t) { this("", r, t); }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public SparseAveragedPerceptron(double r, double t, double pt) {
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
  public SparseAveragedPerceptron(double r, double t, double pt, double nt) {
    this("", r, t, pt, nt);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseAveragedPerceptron.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public SparseAveragedPerceptron(SparseAveragedPerceptron.Parameters p) {
    this("", p);
  }


  /**
    * The learning rate and threshold take default values.
    *
    * @param n  The name of the classifier.
   **/
  public SparseAveragedPerceptron(String n) { this(n, defaultLearningRate); }

  /**
    * Sets the learning rate to the specified value, and the threshold takes
    * the default.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
   **/
  public SparseAveragedPerceptron(String n, double r) {
    this(n, r, defaultThreshold);
  }

  /**
    * Sets the learning rate and threshold to the specified values.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
   **/
  public SparseAveragedPerceptron(String n, double r, double t) {
    this(n, r, t, defaultThickness);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public SparseAveragedPerceptron(String n, double r, double t, double pt) {
    this(n, r, t, pt, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public SparseAveragedPerceptron(String n, double r, double t, double pt,
                                  double nt) {
    super(n);
    Parameters p = new Parameters();
    p.learningRate = r;
    p.threshold = t;
    p.positiveThickness = pt;
    p.negativeThickness = nt;
    setParameters(p);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseAveragedPerceptron.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparseAveragedPerceptron(String n,
                                  SparseAveragedPerceptron.Parameters p) {
    super(n);
    setParameters(p);
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p =
      new Parameters((SparsePerceptron.Parameters) super.getParameters());
    return p;
  }


  /**
    * Sets the values of parameters that control the behavior of this learning
    * algorithm.
    *
    * @param p  The parameters.
   **/
  public void setParameters(Parameters p) {
    super.setParameters(p);
    awv = (AveragedWeightVector) weightVector;
  }


  /**
    * The score of the specified object is equal to <code>w * x + bias</code>
    * where <code>*</code> is dot product, <code>w</code> is the weight
    * vector, and <code>x</code> is the feature vector produced by the
    * extractor.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The result of the dot product plus the bias.
   **/
  public double score(int[] exampleFeatures, double[] exampleValues) {
    double result = awv.dot(exampleFeatures, exampleValues, initialWeight);
    int examples = awv.getExamples();

    if (examples > 0)
      result += (examples * bias - averagedBias) / (double) examples;
    return result;
  }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and adds it to the weight vector.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
   **/
  public void promote(int[] exampleFeatures, double[] exampleValues,
                      double rate) {
    bias += rate;

    int examples = awv.getExamples();
    averagedBias += examples * rate;
    awv.scaledAdd(exampleFeatures, exampleValues, rate, initialWeight);
  }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and subtracts it from the weight vector.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
   **/
  public void demote(int[] exampleFeatures, double[] exampleValues,
                     double rate) {
    bias -= rate;

    int examples = awv.getExamples();
    averagedBias -= examples * rate;
    awv.scaledAdd(exampleFeatures, exampleValues, -rate, initialWeight);
  }


  /**
    * This method works just like
    * {@link LinearThresholdUnit#learn(int[],double[],int[],double[])}, except
    * it notifies its weight vector when it got an example correct in addition
    * to updating it when it makes a mistake.
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

    double s =
      awv.simpleDot(exampleFeatures, exampleValues, initialWeight) + bias;
    if (label && s < threshold + positiveThickness)
      promote(exampleFeatures, exampleValues, getLearningRate());
    else if (!label && s >= threshold - negativeThickness)
      demote(exampleFeatures, exampleValues, getLearningRate());
    else awv.correctExample();
  }


  /**
    * Initializes the weight vector array to the size of
    * the supplied number of features, with each cell taking
    * the default value of {@link #initialWeight}.
    *
    * @param numExamples   The number of examples
    * @param numFeatures   The number of features
   **/
  public void initialize(int numExamples, int numFeatures) {
    double[] weights = new double[numFeatures];
    Arrays.fill(weights, initialWeight);
    weightVector = awv = new AveragedWeightVector(weights);
  }


  /** Resets the weight vector to all zeros. */
  public void forget() {
    super.forget();
    awv = (AveragedWeightVector) weightVector;
    averagedBias = 0;
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link SparsePerceptron#learningRate},
    * {@link LinearThresholdUnit#initialWeight},
    * {@link LinearThresholdUnit#threshold},
    * {@link LinearThresholdUnit#positiveThickness},
    * {@link LinearThresholdUnit#negativeThickness},
    * {@link LinearThresholdUnit#bias}, and finally {@link #averagedBias}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out) {
    out.println(name + ": " + learningRate + ", " + initialWeight + ", "
                + threshold + ", " + positiveThickness + ", "
                + negativeThickness + ", " + bias + ", " + averagedBias);
    if (lexicon == null || lexicon.size() == 0) awv.write(out);
    else awv.write(out, lexicon);
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeDouble(averagedBias);
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
    awv = (AveragedWeightVector) weightVector;
    averagedBias = in.readDouble();
  }


  /**
    * Simply a container for all of {@link SparseAveragedPerceptron}'s
    * configurable parameters.  Using instances of this class should make code
    * more readable and constructors less complicated.  Note that if the
    * object referenced by {@link LinearThresholdUnit.Parameters#weightVector}
    * is replaced via an instance of this class, it must be replaced with an
    * {@link SparseAveragedPerceptron.AveragedWeightVector}.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends SparsePerceptron.Parameters
  {
    /** Sets all the default values. */
    public Parameters() {
      weightVector = (AveragedWeightVector) defaultWeightVector.clone();
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(SparsePerceptron.Parameters p) { super(p); }


    /** Copy constructor. */
    public Parameters(Parameters p) { super(p); }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((SparseAveragedPerceptron) l).setParameters(this);
    }
  }


  /**
    * This implementation of a sparse weight vector associates two
    * <code>double</code>s with each {@link Feature}.  The first plays the
    * role of the usual weight vector, and the second accumulates multiples of
    * examples on which mistakes were made to help implement the weighted
    * average.
    *
    * @author Nick Rizzolo
   **/
  public static class AveragedWeightVector extends SparseWeightVector
  {
    /**
      * Together with {@link SparseWeightVector#weights}, this vector provides
      * enough information to reconstruct the average of all weight vectors
      * arrived at during the course of learning.
     **/
    public DVector averagedWeights;
    /** Counts the total number of training examples this vector has seen. */
    protected int examples;


    /** Simply instantiates the weight vectors. */
    public AveragedWeightVector() { this(new DVector(defaultCapacity)); }

    /**
      * Simply initializes the weight vectors.
      *
      * @param w  An array of weights.
     **/
    public AveragedWeightVector(double[] w) { this(new DVector(w)); }

    /**
      * Simply initializes the weight vectors.
      *
      * @param w  A vector of weights.
     **/
    public AveragedWeightVector(DVector w) {
      super((DVector) w.clone());
      averagedWeights = w;
    }


    /** Increments the {@link #examples} variable. */
    public void correctExample() { ++examples; }
    /** Returns the {@link #examples} variable. */
    public int getExamples() { return examples; }


    /**
      * Returns the averaged weight of the given feature.
      *
      * @param featureIndex The feature index.
      * @param defaultW     The default weight.
      * @return The weight of the feature.
     **/
    public double getAveragedWeight(int featureIndex, double defaultW) {
      if (examples == 0) return 0;
      double aw = averagedWeights.get(featureIndex, defaultW);
      double w = getWeight(featureIndex, defaultW);
      return (examples*w - aw) / (double) examples;
    }


    /**
      * Takes the dot product of this <code>AveragedWeightVector</code> with
      * the argument vector, using the hard coded default weight.
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @return The computed dot product.
     **/
    public double dot(int[] exampleFeatures, double[] exampleValues) {
      return dot(exampleFeatures, exampleValues, defaultWeight);
    }


    /**
      * Takes the dot product of this <code>AveragedWeightVector</code> with
      * the argument vector, using the specified default weight when one is
      * not yet present in this vector.
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @param defaultW         The default weight.
      * @return The computed dot product.
     **/
    public double dot(int[] exampleFeatures, double[] exampleValues,
                      double defaultW) {
      double sum = 0;

      for (int i = 0; i < exampleFeatures.length; i++) {
        double w = getAveragedWeight(exampleFeatures[i], defaultW);
        sum += w * exampleValues[i];
      }

      return sum;
    }


    /**
      * Takes the dot product of the regular, non-averaged, Perceptron weight
      * vector with the given vector, using the hard coded default weight.
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @return The computed dot product.
     **/
    public double simpleDot(int[] exampleFeatures, double[] exampleValues) {
      return super.dot(exampleFeatures, exampleValues, defaultWeight);
    }


    /**
      * Takes the dot product of the regular, non-averaged, Perceptron weight
      * vector with the given vector, using the specified default weight when
      * a feature is not yet present in this vector.
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @param defaultW         An initial weight for new features.
      * @return The computed dot product.
     **/
    public double simpleDot(int[] exampleFeatures, double[] exampleValues,
                            double defaultW) {
      return super.dot(exampleFeatures, exampleValues, defaultW);
    }


    /**
      * Performs pairwise addition of the feature values in the given vector
      * scaled by the given factor, modifying this weight vector, using the
      * specified default weight when a feature from the given vector is not
      * yet present in this vector.  The default weight is used to initialize
      * new feature weights.
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @param factor           The scaling factor.
     **/
    public void scaledAdd(int[] exampleFeatures, double[] exampleValues,
                          double factor) {
      scaledAdd(exampleFeatures, exampleValues, factor, defaultWeight);
    }


    /**
      * Performs pairwise addition of the feature values in the given vector
      * scaled by the given factor, modifying this weight vector, using the
      * specified default weight when a feature from the given vector is not
      * yet present in this vector.
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @param factor           The scaling factor.
      * @param defaultW         An initial weight for new features.
     **/
    public void scaledAdd(int[] exampleFeatures, double[] exampleValues,
                         double factor, double defaultW) {
      for (int i = 0; i < exampleFeatures.length; i++) {
        int featureIndex = exampleFeatures[i];
        double currentWeight = getWeight(featureIndex, defaultW);
        double w = currentWeight + factor*exampleValues[i];

        double difference = w - currentWeight;
        updateAveragedWeight(featureIndex, examples*difference);

        setWeight(featureIndex, w);
      }

      ++examples;
    }


    /**
      * Adds a new value to the current averaged weight indexed
      * by the supplied feature index.
      *
      * @param featureIndex The feature index.
      * @param w            The value to add to the current weight.
     **/
    protected void updateAveragedWeight(int featureIndex, double w) {
      double newWeight = averagedWeights.get(featureIndex, defaultWeight) + w;
      averagedWeights.set(featureIndex, newWeight, defaultWeight);
    }


    /**
      * Outputs the contents of this <code>SparseWeightVector</code> into the
      * specified <code>PrintStream</code>.  The string representation starts
      * with a <code>"Begin"</code> annotation, ends with an
      * <code>"End"</code> annotation, and without a <code>Lexicon</code>
      * passed as a parameter, the weights are simply printed in the order of
      * their integer indices.
      *
      * @param out  The stream to write to.
     **/
    public void write(PrintStream out) {
      out.println("Begin AveragedWeightVector");
      for (int i = 0; i < averagedWeights.size(); ++i)
        out.println(getAveragedWeight(i, 0));
      out.println("End AveragedWeightVector");
    }


    /**
      * Outputs the contents of this <code>SparseWeightVector</code> into the
      * specified <code>PrintStream</code>.  The string representation starts
      * with a <code>"Begin"</code> annotation, ends with an
      * <code>"End"</code> annotation, and lists each feature with its
      * corresponding weight on the same, separate line in between.
      *
      * @param out  The stream to write to.
      * @param lex  The feature lexicon.
     **/
    public void write(PrintStream out, Lexicon lex) {
      out.println("Begin AveragedWeightVector");

      Map map = lex.getMap();
      Map.Entry[] entries =
        (Map.Entry[]) map.entrySet().toArray(new Map.Entry[map.size()]);
      Arrays.sort(entries,
                  new Comparator() {
                    public int compare(Object o1, Object o2) {
                      Map.Entry e1 = (Map.Entry) o1;
                      Map.Entry e2 = (Map.Entry) o2;
                      int i1 = ((Integer) e1.getValue()).intValue();
                      int i2 = ((Integer) e2.getValue()).intValue();
                      if ((i1 < weights.size()) != (i2 < weights.size()))
                        return i1 - i2;
                      return ((Feature) e1.getKey()).compareTo(e2.getKey());
                    }
                  });

      int i, biggest = 0;
      for (i = 0; i < entries.length; ++i) {
        String key =
          entries[i].getKey().toString()
          + (((Integer) entries[i].getValue()).intValue() < weights.size()
             ? "" : " (pruned)");
        biggest = Math.max(biggest, key.length());
      }

      if (biggest % 2 == 0) biggest += 2;
      else ++biggest;

      for (i = 0; i < entries.length; ++i) {
        String key =
          entries[i].getKey().toString()
          + (((Integer) entries[i].getValue()).intValue() < weights.size()
             ? "" : " (pruned)");
        out.print(key);
        for (int j = 0; key.length() + j < biggest; ++j) out.print(" ");

        int index = ((Integer) entries[i].getValue()).intValue();
        double weight = getAveragedWeight(index, 0);
        out.println(weight);
      }

      out.println("End AveragedWeightVector");
    }


    /**
      * Writes the weight vector's internal representation in binary form.
      *
      * @param out  The output stream.
     **/
    public void write(ExceptionlessOutputStream out) {
      super.write(out);
      out.writeInt(examples);
      averagedWeights.write(out);
    }


    /**
      * Reads the representation of a weight vector with this object's
      * run-time type from the given stream, overwriting the data in this
      * object.
      *
      * <p> This method is appropriate for reading weight vectors as written
      * by {@link #write(ExceptionlessOutputStream)}.
      *
      * @param in The input stream.
     **/
    public void read(ExceptionlessInputStream in) {
      super.read(in);
      examples = in.readInt();
      averagedWeights.read(in);
    }


    /**
      * Returns a copy of this <code>AveragedWeightVector</code>.
      *
      * @return A copy of this <code>AveragedWeightVector</code>.
     **/
    public Object clone() {
      AveragedWeightVector clone = (AveragedWeightVector) super.clone();
      clone.averagedWeights = (DVector) averagedWeights.clone();
      return clone;
    }


    /**
      * Returns a new, empty weight vector with the same parameter settings as
      * this one.
      *
      * @return An empty weight vector.
     **/
    public SparseWeightVector emptyClone() {
      return new AveragedWeightVector();
    }
  }
}

