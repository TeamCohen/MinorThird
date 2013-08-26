package LBJ2.infer;

import LBJ2.classify.ScoreSet;
import LBJ2.learn.Learner;


/**
  * Represents a classifier application.  An inference algorithm may change
  * the value returned by the classifier application when satisfying
  * constraints.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderVariable implements Cloneable
{
  /** The classifier being applied. */
  private Learner classifier;
  /** The classifier is applied to this example object. */
  private Object example;
  /** The scores of the possible values this variable might be set to. */
  private ScoreSet scores;
  /** The value imposed on the classifier when applied to the example. */
  private String value;


  /**
    * Initializing constructor.
    *
    * @param c  The classifier being applied.
    * @param e  The classifier is applied to this example object.
   **/
  public FirstOrderVariable(Learner c, Object e) {
    classifier = c;
    example = e;
  }


  /** Retrieves the classifier. */
  public Learner getClassifier() { return classifier; }


  /** Retrieves the example object. */
  public Object getExample() { return example; }


  /** Retrieves the value this variable currently takes. */
  public String getValue() {
    if (value == null) {
      if (scores == null) scores = classifier.scores(example);
      value = scores.highScoreValue();
    }

    return value;
  }


  /**
    * Sets the value of this variable.
    *
    * @param v  The new value of this variable.
   **/
  public void setValue(String v) { value = v; }


  /**
    * Sets the example object.
    *
    * @param e  The new example object.
   **/
  public void setExample(Object e) { example = e; }


  /** Retrieves the score of the current value of this variable. */
  public double getScore() {
    if (scores == null) scores = classifier.scores(example);
    return scores.get(getValue());
  }


  /** Retrieves all the scores for the values this variable may take. */
  public ScoreSet getScores() {
    if (scores == null) scores = classifier.scores(example);
    return scores;
  }


  /** Returns a string representation of this variable. */
  public String toString() {
    return classifier + "(" + Inference.exampleToString(example) + ") = "
           + value;
  }


  /**
    * The hash code of a <code>FirstOrderVariable</code> is the hash code of
    * the string representation of the classifier plus the system's hash code
    * for the example object.
    *
    * @return The hash code of this <code>FirstOrderVariable</code>.
   **/
  public int hashCode() {
    return classifier.toString().hashCode()
           + System.identityHashCode(example);
  }


  /**
    * Two <code>FirstOrderVariable</code>s are equivalent when their
    * classifiers are equivalent and they store the same example object.
    *
    * @param o  The object to test equivalence with.
    * @return <code>true</code> iff this object is equivalent to the argument
    *         object.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderVariable)) return false;
    FirstOrderVariable v = (FirstOrderVariable) o;
    return classifier.equals(v.classifier) && example == v.example;
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
      System.err.println("Error cloning FirstOrderVariable:");
      e.printStackTrace();
      System.exit(1);
    }

    return clone;
  }
}

