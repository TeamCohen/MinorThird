package LBJ2.learn;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import LBJ2.classify.Classifier;
import LBJ2.classify.Feature;
import LBJ2.classify.FeatureVector;
import LBJ2.classify.RealFeature;
import LBJ2.classify.Score;
import LBJ2.classify.ScoreSet;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.OVector;


/**
  * Naive Bayes is a multi-class learner that uses prediction value counts and
  * feature counts given a particular prediction value to select the most
  * likely prediction value.  More precisely, a score <i>s<sub>v</sub></i> for
  * a given prediction value <i>v</i> is computed such that
  * <i>e<sup>s<sub>v</sub></sup></i> is proportional to
  *
  * <blockquote>
  *   <i>P(v) Prod<sub>f</sub> P(f|v)</i>
  * </blockquote>
  *
  * where <i>Prod</i> is a multiplication quantifier over <i>f</i>, and
  * <i>f</i> stands for a feature.  The value corresponding to the highest
  * score is selected as the prediction.  Feature values that were never
  * observed given a particular prediction value during training are smoothed
  * with a configurable constant that defaults to <i>e<sup>-15</sup></i>.
  *
  * <p> This {@link Learner} learns a <code>discrete</code> classifier from
  * other <code>discrete</code> classifiers.  <i>Features coming from
  * <code>real</code> classifiers are ignored</i>.  It is also assumed that a
  * single discrete label feature will be produced in association with each
  * example object.  A feature taking one of the values observed in that label
  * feature will be produced by the learned classifier.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.NaiveBayes.Parameters Parameters} as input.  The
  * documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.NaiveBayes.Parameters Parameters} class indicates the
  * default value of the parameter when using the latter type of constructor.
  *
  * @see    NaiveBayesVector
  * @author Nick Rizzolo
 **/
public class NaiveBayes extends Learner
{
  /**
    * The default conditional feature probability is
    * <i>e<sup><code>defaultSmoothing</code></sup></i>.
   **/
  public static final int defaultSmoothing = -15;


  /**
    * The exponential of this number is used as the conditional probability of
    * a feature that was never observed during training; default
    * {@link #defaultSmoothing}.
   **/
  protected double smoothing;
  /** One {@link NaiveBayesVector} for each observed prediction value. */
  protected OVector network;


  /** Default constructor. */
  public NaiveBayes() { this(""); }

  /**
    * Initializes the smoothing constant.
    *
    * @param smooth The exponential of this number is used as the conditional
    *               probability of a feature that was never observed during
    *               training.
   **/
  public NaiveBayes(double smooth) { this("", smooth); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link NaiveBayes.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public NaiveBayes(Parameters p) { this("", p); }

  /**
    * Initializes the name of the classifier.
    *
    * @param n  The classifier's name.
   **/
  public NaiveBayes(String n) { this(n, defaultSmoothing); }

  /**
    * Initializes the name and smoothing constant.
    *
    * @param name   The classifier's name.
    * @param smooth The exponential of this number is used as the conditional
    *               probability of a feature that was never observed during
    *               training.
   **/
  public NaiveBayes(String name, double smooth) {
    super(name);
    network = new OVector();
    smoothing = smooth;
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link NaiveBayes.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public NaiveBayes(String n, Parameters p) {
    super(n);
    network = new OVector();
    setParameters(p);
  }


  /**
    * Sets the values of parameters that control the behavior of this learning
    * algorithm.
    *
    * @param p  The parameters.
   **/
  public void setParameters(Parameters p) {
    smoothing = p.smoothing;
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() {
    Parameters p = new Parameters(super.getParameters());
    p.smoothing = smoothing;
    return p;
  }


  /**
    * Sets the smoothing parameter to the specified value.
    *
    * @param s  The new value for the smoothing parameter.
   **/
  public void setSmoothing(double s) { smoothing = s; }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(Classifier l) {
    if (!l.getOutputType().equals("discrete")) {
      System.err.println(
          "LBJ WARNING: NaiveBayes will only work with a label classifier "
          + "that returns discrete.");
      System.err.println(
          "             The given label classifier, " + l.getClass().getName()
          + ", returns " + l.getOutputType() + ".");
    }

    super.setLabeler(l);
  }


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
    int label = exampleLabels[0];
    int N = network.size();

    NaiveBayesVector labelVector = null;
    if (label >= N) {
      while (N++ < label)
        network.add(new NaiveBayesVector());
      labelVector = new NaiveBayesVector();
      network.add(labelVector);
    }
    else labelVector = (NaiveBayesVector) network.get(label);

    labelVector.scaledAdd(exampleFeatures, exampleValues, 1.0);
  }


  /** Clears the network. */
  public void forget() {
    super.forget();
    network = new OVector();
  }


  /**
    * The scores in the returned {@link ScoreSet} are the posterior
    * probabilities of each possible label given the example.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return A set of scores indicating the degree to which each possible
    *         discrete classification value is associated with the given
    *         example object.
   **/
  public ScoreSet scores(int[] exampleFeatures, double[] exampleValues) {
    ScoreSet s = new ScoreSet();

    for (int l = 0; l < network.size(); l++) {
      NaiveBayesVector vector = (NaiveBayesVector) network.get(l);
      double score = vector.dot(exampleFeatures, exampleValues);
      s.put(labelLexicon.lookupKey(l).getStringValue(), score);
    }

    Score[] original = s.toArray();
    ScoreSet result = new ScoreSet();

    // This code would clearly run quicker if you computed each exp(score)
    // ahead of time, and divided them each by their sum.  However, each score
    // is likely to be a very negative number, so exp(score) may not be
    // numerically stable.  Subtracting two scores, however, hopefully leaves
    // you with a "less negative" number, so exp applied to the subtraction
    // hopefully behaves better.

    for (int i = 0; i < original.length; ++i) {
      double score = 1;

      for (int j = 0; j < original.length; ++j) {
        if (i == j) continue;
        score += Math.exp(original[j].score - original[i].score);
      }

      result.put(original[i].value, 1 / score);
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
    double bestScore = -Double.MAX_VALUE;
    int bestLabel = -1;

    for (int l = 0; l < network.size(); l++) {
      NaiveBayesVector vector = (NaiveBayesVector) network.get(l);
      double score = vector.dot(f, v);

      if (score > bestScore) {
        bestLabel = l;
        bestScore = score;
      }
    }

    if (bestLabel == -1) return null;
    return predictions.get(bestLabel);
  }


  /**
    * Prediction value counts and feature counts given a particular prediction
    * value are used to select the most likely prediction value.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return The most likely discrete value.
   **/
  public String discreteValue(int[] exampleFeatures, double[] exampleValues) {
    return featureValue(exampleFeatures, exampleValues).getStringValue();
  }


  /**
    * Prediction value counts and feature counts given a particular prediction
    * value are used to select the most likely prediction value.
    *
    * @param exampleFeatures  The example's array of feature indices.
    * @param exampleValues    The example's array of feature values.
    * @return A single discrete feature, set to the most likely value.
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
    for (int i = 0; i < N; ++i) {
      out.println("label: " + labelLexicon.lookupKey(i).getStringValue());
      ((NaiveBayesVector) network.get(i)).write(out);
    }

    out.println("End of NaiveBayes");
  }


  /**
    * Writes the learned function's internal representation in binary form.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeDouble(smoothing);
    int N = network.size();
    out.writeInt(N);
    for (int i = 0; i < N; ++i)
      ((NaiveBayesVector) network.get(i)).write(out);
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
    smoothing = in.readDouble();
    int N = in.readInt();
    network = new OVector(N);

    for (int i = 0; i < N; ++i) {
      NaiveBayesVector nbv = new NaiveBayesVector();
      nbv.read(in);
      network.add(nbv);
    }
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone() {
    NaiveBayes clone = (NaiveBayes) super.clone();
    int N = network.size();
    clone.network = new OVector(N);
    for (int i = 0; i < N; ++i)
      clone.network.add(((NaiveBayesVector) network.get(i)).clone());
    return clone;
  }


  /**
    * Simply a container for all of {@link NaiveBayes}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends Learner.Parameters
  {
    /**
      * The exponential of this number is used as the conditional probability
      * of a feature that was never observed during training; default
      * {@link NaiveBayes#defaultSmoothing}.
     **/
    public double smoothing;


    /** Sets all the default values. */
    public Parameters() {
      smoothing = defaultSmoothing;
    }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(Learner.Parameters p) {
      super(p);
      smoothing = defaultSmoothing;
    }


    /** Copy constructor. */
    public Parameters(Parameters p) {
      super(p);
      smoothing = p.smoothing;
    }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((NaiveBayes) l).setParameters(this);
    }


    /**
      * Creates a string representation of these parameters in which only
      * those parameters that differ from their default values are mentioned.
     **/
    public String nonDefaultString() {
      String result = super.nonDefaultString();

      if (smoothing != NaiveBayes.defaultSmoothing)
        result += ", smoothing = " + smoothing;

      if (result.startsWith(", ")) result = result.substring(2);
      return result;
    }
  }


  /**
    * A <code>Count</code> object stores two <code>doubles</code>, one which
    * holds a accumulated count value and the other intended to hold the
    * natural logarithm of the count.  The object also contains a
    * <code>boolean</code> flag that is set when the log needs to be updated.
    *
    * @author Nick Rizzolo
   **/
  protected static class Count implements Cloneable, Serializable
  {
    /** The accumulated value. */
    protected double count;
    /** The natural logartihm of {@link #count} is sometimes stored here. */
    protected transient double logCount;
    /** A flag that is set iff {@link #logCount} is not up to date. */
    protected transient boolean updateLog;


    /** Sets the count to 0. */
    public Count() {
      count = 0;
      logCount = 0;
      updateLog = true;
    }


    /** Returns the integer count. */
    public double getCount() { return count; }


    /**
      * Increments the count, but does not update the log.
      *
      * @param inc  The amount the count should be incremented by.
     **/
    public void increment(double inc) {
      count += inc;
      updateLog = true;
    }


    /** Returns the log after updating it. */
    public double getLog() {
      if (updateLog) {
        logCount = Math.log(count);
        updateLog = false;
      }

      return logCount;
    }


    /**
      * The string representation of a <code>Count</code> object is simply the
      * integer count.
     **/
    public String toString() { return "" + count; }


    /**
      * Writes the count's internal representation in binary form.
      *
      * @param out  The output stream.
     **/
    public void write(ExceptionlessOutputStream out) {
      out.writeDouble(count);
    }


    /**
      * Reads the binary representation of a count into this object,
      * overwriting any data that may already be here.
      *
      * @param in The input stream.
     **/
    public void read(ExceptionlessInputStream in) {
      count = in.readDouble();
      updateLog = true;
    }


    /**
      * This method returns a shallow clone.
      *
      * @return A shallow clone.
     **/
    public Object clone() {
      Object clone = null;

      try { clone = super.clone(); }
      catch (Exception e) {
        System.err.println("Error cloning " + getClass().getName() + ":");
        e.printStackTrace();
        System.exit(1);
      }

      return clone;
    }


    /**
      * Special handling during deserialization to ensure that
      * {@link #updateLog} is set to <code>true</code>.
      *
      * @param in The stream to deserialize from.
     **/
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      updateLog = true;
    }
  }


  /**
    * Keeps track of all the counts associated with a given label.
    * Features are associated with {@link NaiveBayes.Count}s.  Those not
    * appearing in this vector are assumed to have a count of 0.  The
    * invocation of either of the <code>scaledAdd</code> methods increments
    * the prior count for the label.
    *
    * <p> {@link RealFeature}s' strengths are ignored by this vector; they are
    * assumed to be equal to 1, as if the feature were an active Boolean
    * feature.
    *
    * @author Nick Rizzolo
   **/
  protected class NaiveBayesVector extends SparseWeightVector
  {
    /** The counts in the vector indexed by their {@link Lexicon} key. */
    protected OVector counts;
    /**
      * The prior count is the number of times either <code>scaledAdd</code>
      * method has been called.
     **/
    protected Count priorCount;


    /** Simply instantiates {@link NaiveBayes.NaiveBayesVector#counts}. */
    public NaiveBayesVector() { this(new OVector(defaultCapacity)); }

    /**
      * Simply initializes {@link #counts}.
      *
      * @param w  An array of counts.
     **/
    public NaiveBayesVector(Count[] w) { this(new OVector(w)); }

    /**
      * Simply initializes {@link #counts}.
      *
      * @param w  A vector of counts.
     **/
    public NaiveBayesVector(OVector w) {
      counts = w;
      priorCount = new Count();
    }


    /**
      * Returns the prior count of the prediction value associated with this
      * vector.
     **/
    public Count getPrior() { return priorCount; }


    /**
      * Takes the dot product of this vector with the given vector, using the
      * hard coded smoothing weight.
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @return The computed dot product.
     **/
    public double dot(int[] exampleFeatures, double[] exampleValues) {
      return
        dot(exampleFeatures, exampleValues, priorCount.getLog() + smoothing);
    }


    /**
      * Takes the dot product of this vector with the given vector,
      * using the specified default weight when encountering a feature that is
      * not yet present in this vector.  Here, weights are taken as
      * <i>log(feature count / prior count)</i>.  The output of this method is
      * related to the empirical probability of the example <i>e</i> as
      * follows: <br><br>
      *
      * <i>exp(dot(e)) / (sum of all labels' prior counts)) =</i><br>
      * <i>P(e's label && e)</i>
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @param defaultW         The default weight.
      * @return The computed dot product.
     **/
    public double dot(int[] exampleFeatures, double[] exampleValues,
                      double defaultW) {
      double sum = (1 - exampleFeatures.length) * priorCount.getLog();
      for (int i = 0; i < exampleFeatures.length; i++)
        sum += getWeight(exampleFeatures[i], defaultW);
      return sum;
    }


    /**
      * Returns the count of the given feature.
      *
      * @param featureIndex The feature index.
      * @return The count of the feature.
     **/
    public double getCount(int featureIndex) {
      while (counts.size() <= featureIndex) counts.add(new Count());
      return ((Count) counts.get(featureIndex)).getCount();
    }


    /**
      * Returns the weight of the given feature
      *
      * @param featureIndex The feature index.
      * @param defaultW     The default count.
      * @return The weight of the feature.
     **/
    public double getWeight(int featureIndex, double defaultW) {
      while (counts.size() <= featureIndex) counts.add(new Count());
      Count c = (Count) counts.get(featureIndex);
      if (c.getCount() == 0) return defaultW;
      return c.getLog();
    }


    /**
      * This method is overridden to do nothing; use
      * {@link #incrementCount(int,double)} instead.
      *
      * @param f  Unused.
      * @param w  Unused.
     **/
    public void setWeight(int f, double w) { }


    /**
      * Increments the count of the given feature.
      *
      * @param featureIndex  The index of the feature to update.
      * @param factor        The factor by which to increment.
     **/
    public void incrementCount(int featureIndex, double factor) {
      if (featureIndex < counts.size())
        ((Count) counts.get(featureIndex)).increment(factor);
      else {
        while (counts.size() < featureIndex) counts.add(new Count());
        Count c = new Count();
        c.increment(factor);
        counts.add(c);
      }
    }


    /**
      * This method is similar to the implementation in
      * {@link SparseWeightVector} except that
      * {@link NaiveBayes.NaiveBayesVector#incrementCount(int,double)}
      * is called instead of
      * {@link SparseWeightVector#setWeight(int,double)}.
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @param factor           The scaling factor.
     **/
    public void scaledAdd(int[] exampleFeatures, double[] exampleValues,
                          double factor) {
      priorCount.increment(factor);
      for (int i = 0; i < exampleFeatures.length; i++)
        incrementCount(exampleFeatures[i], factor);
    }


    /**
      * This method is similar to the implementation in
      * {@link SparseWeightVector} except that the <code>defaultW</code>
      * argument is ignored and
      * {@link NaiveBayes.NaiveBayesVector#incrementCount(int,double)}
      * is called instead of
      * {@link SparseWeightVector#setWeight(int,double)}.
      *
      * @param exampleFeatures  The example's array of feature indices.
      * @param exampleValues    The example's array of feature values.
      * @param factor           The scaling factor.
      * @param defaultW         Unused.
     **/
    public void scaledAdd(int[] exampleFeatures, double[] exampleValues,
                          double factor, double defaultW) {
      scaledAdd(exampleFeatures, exampleValues, factor);
    }


    /**
      * Outputs the contents of this vector into the specified
      * <code>PrintStream</code>.  The string representation is the same as in
      * the super class, except the <code>"Begin"</code> annotation line also
      * contains the value of {@link #priorCount} in parentheses.  In
      * addition, this method has access to the lexicon, so the output of this
      * method is equivalent to that of {@link #write(PrintStream,Lexicon)}.
      *
      * @param out  The stream to write to.
     **/
    public void write(PrintStream out) {
      write(out, lexicon);
    }


    /**
      * Outputs the contents of this vector into the specified
      * <code>PrintStream</code>.  The string representation is the same as in
      * the super class, except the <code>"Begin"</code> annotation line also
      * contains the value of {@link #priorCount} in parentheses.
      *
      * @param out  The stream to write to.
      * @param lex  The feature lexicon.
     **/
    public void write(PrintStream out, Lexicon lex) {
      out.println("Begin NaiveBayesVector (" + priorCount + ")");

      Map map = lex.getMap();
      Map.Entry[] entries =
        (Map.Entry[]) map.entrySet().toArray(new Map.Entry[map.size()]);
      Arrays.sort(entries,
                  new Comparator() {
                    public int compare(Object o1, Object o2) {
                      Map.Entry e1 = (Map.Entry) o1;
                      Map.Entry e2 = (Map.Entry) o2;
                      return ((Feature) e1.getKey()).compareTo(e2.getKey());
                    }
                  });

      int i, biggest = 0;
      for (i = 0; i < entries.length; ++i) {
        String key = entries[i].getKey().toString();
        biggest = Math.max(biggest, key.length());
      }

      if (biggest % 2 == 0) biggest += 2;
      else ++biggest;

      for (i = 0; i < entries.length; ++i) {
        String key = entries[i].getKey().toString();
        int index = ((Integer) entries[i].getValue()).intValue();
        out.print(key);
        for (int j = 0; key.length() + j < biggest; ++j) out.print(" ");
        out.println(getCount(index));
      }

      out.println("End NaiveBayesVector");
    }


    /**
      * Writes the weight vector's internal representation in binary form.
      * <b>Note:</b> this method does not call
      * {@link SparseWeightVector#write(ExceptionlessOutputStream)} and does
      * not output its class name or the contents of
      * {@link SparseWeightVector#weights} since there shouldn't be any.
      *
      * @param out  The output stream.
     **/
    public void write(ExceptionlessOutputStream out) {
      priorCount.write(out);
      out.writeInt(counts.size());
      for (int i = 0; i < counts.size(); ++i)
        ((Count) counts.get(i)).write(out);
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
      priorCount = new Count();
      priorCount.read(in);
      int N = in.readInt();
      counts = new OVector(N);
      for (int i = 0; i < N; ++i) {
        Count c = new Count();
        c.read(in);
        counts.add(c);
      }
    }


    /**
      * Returns a copy of this <code>NaiveBayesVector</code>.
      *
      * @return A copy of this <code>NaiveBayesVector</code>.
     **/
    public Object clone() {
      NaiveBayesVector clone = (NaiveBayesVector) super.clone();
      Count[] array = new Count[counts.size()];
      for (int i = 0; i < counts.size(); ++i)
        array[i] = (Count) ((Count) counts.get(i)).clone();
      clone.counts = new OVector(array);
      return clone;
    }


    /**
      * Returns a new, empty weight vector with the same parameter settings as
      * this one.
      *
      * @return An empty weight vector.
     **/
    public SparseWeightVector emptyClone() {
      return new NaiveBayesVector();
    }
  }
}

