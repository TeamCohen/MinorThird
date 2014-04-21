package LBJ2.learn;

import LBJ2.classify.FeatureVector;


/**
  * A simple implementation of a learner that learns from examples with
  * multiple labels and is capable of predicting multiple labels on new
  * examples.  A separate {@link LinearThresholdUnit} is learned independently
  * to predict whether each label is appropriate for a given example.  Any
  * {@link LinearThresholdUnit} may be used, so long as it implements its
  * <code>clone()</code> method and a public constructor that takes no
  * arguments.  During testing, the {@link #classify(Object)} method returns a
  * separate feature for each {@link LinearThresholdUnit} whose score on the
  * example object exceeds the threshold.
  *
  * @author Nick Rizzolo
 **/
public class MultiLabelLearner extends SparseNetworkLearner
{
  /**
    * Instantiates this multi-label learner with the default learning
    * algorithm: {@link SparsePerceptron}.
   **/
  public MultiLabelLearner() { this(""); }

  /**
    * Instantiates this multi-label learner using the specified algorithm to
    * learn each class separately as a binary classifier.  This constructor
    * will normally only be called by the compiler.
    *
    * @param ltu  The linear threshold unit used to learn binary classifiers.
   **/
  public MultiLabelLearner(LinearThresholdUnit ltu) { this("", ltu); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link MultiLabelLearner.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public MultiLabelLearner(Parameters p) { this("", p); }

  /**
    * Instantiates this multi-label learner with the default learning
    * algorithm: {@link SparsePerceptron}.
    *
    * @param n  The name of the classifier.
   **/
  public MultiLabelLearner(String n) { super(n); }

  /**
    * Instantiates this multi-label learner using the specified algorithm to
    * learn each class separately as a binary classifier.
    *
    * @param n    The name of the classifier.
    * @param ltu  The linear threshold unit used to learn binary classifiers.
   **/
  public MultiLabelLearner(String n, LinearThresholdUnit ltu) {
    super(n, ltu);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link MultiLabelLearner.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public MultiLabelLearner(String n, Parameters p) {
    super(n, p);
  }


  /**
    * Retrieves the parameters that are set in this learner.
    *
    * @return An object containing all the values of the parameters that
    *         control the behavior of this learning algorithm.
   **/
  public Learner.Parameters getParameters() { return new Parameters(); }


  /** This learner's output type is <code>"discrete%"</code>. */
  public String getOutputType() { return "discrete%"; }


  /**
    * Returns a separate feature for each {@link LinearThresholdUnit} whose
    * score on the example object exceeds the threshold.
    *
    * @param exampleFeatures  The example's feature indices.
    * @param exampleValues    The feature values.
    * @return A vector containing the features described above.
   **/
  public FeatureVector classify(int[] exampleFeatures, double[] exampleValues)
  {
    FeatureVector result = new FeatureVector();

    for (int i = 0; i < network.size(); ++i) {
      LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(i);
      double score = ltu.score(exampleFeatures, exampleValues);
      if (score >= 0) result.addFeature(predictions.get(i));
    }

    return result;
  }


  /**
    * Simply a container for all of {@link MultiLabelLearner}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends SparseNetworkLearner.Parameters
  {
    /** Sets all the default values. */
    public Parameters() { }


    /**
      * Sets the parameters from the parent's parameters object, giving
      * defaults to all parameters declared in this object.
     **/
    public Parameters(SparseNetworkLearner.Parameters p) { super(p); }


    /** Copy constructor. */
    public Parameters(Parameters p) { super(p); }


    /**
      * Calls the appropriate <code>Learner.setParameters(Parameters)</code>
      * method for this <code>Parameters</code> object.
      *
      * @param l  The learner whose parameters will be set.
     **/
    public void setParameters(Learner l) {
      ((SparseNetworkLearner) l).setParameters(this);
    }
  }
}

