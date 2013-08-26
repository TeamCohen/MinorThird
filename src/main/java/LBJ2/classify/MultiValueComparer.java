package LBJ2.classify;


/**
  * This classifier applies another classifier to the example object and
  * returns a Boolean feature (with value "true" or "false") indicating
  * whether a given feature value appeared in the output of the classifier.
  * This behavior differs from that of {@link ValueComparer} because it does
  * not assume that the given classifier will return only a single feature.
  *
  * @author Nick Rizzolo
 **/
public class MultiValueComparer extends ValueComparer
{
  /**
    * Constructor.
    *
    * @param c  The classifier whose value will be compared.
    * @param v  The value to compare with.
   **/
  public MultiValueComparer(Classifier c, String v) { super(c, v); }


  /**
    * Returns a Boolean feature (with value "true" or "false") indicating
    * whether the output of {@link ValueComparer#labeler} applied to the
    * argument object contained the feature value referenced by
    * {@link ValueComparer#value}.
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
    short p = shortValue(o);
    return
      new DiscretePrimitiveStringFeature(
            "", "MultiValueComparer", "", DiscreteFeature.BooleanValues[p], p,
            (short) 2);
  }


  /**
    * Returns the value of the discrete feature that would be returned by this
    * classifier.
    *
    * @param o  The object to classify.
    * @return The value of the feature produced for the input object.
   **/
  public String discreteValue(Object o) {
    return DiscreteFeature.BooleanValues[shortValue(o)];
  }


  /**
    * Returns the prediction of this classifier as a <code>short</code> that
    * acts as a pointer into {@link DiscreteFeature#BooleanValues}.
    *
    * @param o  The object to classify.
    * @return The classification of <code>o</code> as a <code>short</code>.
   **/
  public short shortValue(Object o) {
    boolean prediction = false;
    FeatureVector v = labeler.classify(o);
    int N = v.featuresSize();

    for (int i = 0; i < N && !prediction; ++i)
      prediction = v.getFeature(i).getStringValue().equals(value);

    return prediction ? (short) 1 : (short) 0;
  }


  /**
    * The <code>String</code> representation of a <code>ValueComparer</code>
    * has the form <code>"ValueComparer(</code><i>child</i><code>)</code>,
    * where <i>child</i> is the <code>String</code> representation of the
    * classifier whose value is being compared.
    *
    * @return A string of the form described above.
   **/
  public String toString() {
    return "MultiValueComparer(" + labeler + ")";
  }
}

