package LBJ2.classify;


/**
  * This classifier applies another classifier to the example object and
  * returns a Boolean feature (with value "true" or "false") representing the
  * equality of the argument classifier's feature value to a given value.  The
  * argument classifier must be <code>discrete</code>.
  *
  * @see    MultiValueComparer
  * @author Nick Rizzolo
 **/
public class ValueComparer extends Classifier
{
  /** The classifier whose value will be compared. */
  protected Classifier labeler;
  /** The value to compare with. */
  protected String value;


  /**
    * Constructor.
    *
    * @param c  The classifier whose value will be compared.
    * @param v  The value to compare with.
   **/
  public ValueComparer(Classifier c, String v) {
    super("ValueComparer");
    labeler = c;
    value = v;
  }


  /** Sets the value of {@link #labeler}. */
  public void setLabeler(Classifier l) { labeler = l; }


  /**
    * Returns a Boolean feature (with value "true" or "false") representing
    * the equality of the output of {@link #labeler} applied to the argument
    * object and {@link #value}.
    *
    * @param o  The object to make decisions about.
    * @return A feature vector containing the feature described above.
   **/
  public FeatureVector classify(Object o) {
    return new FeatureVector(featureValue(o));
  }


  /**
    * Returns the classification of the given example object as a single
    * feature instead of a {@link FeatureVector}.
    *
    * @param o  The object to classify.
    * @return The classification of <code>o</code> as a feature.
   **/
  public Feature featureValue(Object o) {
    String label = labeler.discreteValue(o);
    short prediction = label.equals(value) ? (short) 1 : (short) 0;
    return
      new DiscretePrimitiveStringFeature(
            "LBJ2.classify", "ValueComparer", "",
            DiscreteFeature.BooleanValues[prediction], prediction, (short) 2);
  }


  /**
    * Returns the value of the discrete feature that would be returned by this
    * classifier.
    *
    * @param o  The object to classify.
    * @return The value of the feature produced for the input object.
   **/
  public String discreteValue(Object o) {
    String label = labeler.discreteValue(o);
    return DiscreteFeature.BooleanValues[label.equals(value) ? 1 : 0];
  }


  /**
    * Returns a string describing the input type of this classifier.
    *
    * @return A string describing the input type of this classifier.
   **/
  public String getInputType() { return labeler.getInputType(); }


  /**
    * Returns the array of allowable values that a feature returned by this
    * classifier may take.
    *
    * @see    DiscreteFeature#BooleanValues
    * @return <code>DiscreteFeature.BooleanValues</code>
   **/
  public String[] allowableValues() { return DiscreteFeature.BooleanValues; }


  /**
    * The <code>String</code> representation of a <code>ValueComparer</code>
    * has the form <code>"ValueComparer(</code><i>child</i><code>)</code>,
    * where <i>child</i> is the <code>String</code> representation of the
    * classifier whose value is being compared.
    *
    * @return A string of the form described above.
   **/
  public String toString() { return "ValueComparer(" + labeler + ")"; }
}

