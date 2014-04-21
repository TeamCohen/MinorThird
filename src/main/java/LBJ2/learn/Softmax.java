package LBJ2.learn;

import LBJ2.classify.Score;
import LBJ2.classify.ScoreSet;


/**
  * The softmax normalization function replaces each score with the fraction
  * of its exponential out of the sum of all scores' exponentials.  In other
  * words, each score <code>s<sub>i</sub></code> is replaced by
  * <code>exp(alpha s<sub>i</sub>) / sum<sub>j</sub> exp(alpha
  * s<sub>j</sub>)</code>, where <code>alpha</code> is a user-specified
  * constant.
  *
  * @author Nick Rizzolo
 **/
public class Softmax extends Normalizer
{
  /** The user-specified constant described above. */
  protected double alpha;


  /** Default constructor; sets {@link #alpha} to 1. */
  public Softmax() { this(1); }

  /**
    * Initializing constructor.
    *
    * @param a  The setting for {@link #alpha}.
   **/
  public Softmax(double a) { alpha = a; }


  /** Retrieves the value of {@link #alpha}. */
  public double getAlpha() { return alpha; }


  /**
    * Normalizes the given <code>ScoreSet</code>; its scores are modified in
    * place before it is returned.
    *
    * @param scores The set of scores to normalize.
    * @return The normalized set of scores.
   **/
  public ScoreSet normalize(ScoreSet scores) {
    Score[] array = scores.toArray();
    double sum = 0;

    for (int i = 0; i < array.length; ++i) {
      array[i].score = Math.exp(alpha * array[i].score);
      sum += array[i].score;
    }

    for (int i = 0; i < array.length; ++i) array[i].score /= sum;
    return scores;
  }
}

