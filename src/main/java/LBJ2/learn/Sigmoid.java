package LBJ2.learn;

import LBJ2.classify.Score;
import LBJ2.classify.ScoreSet;


/**
  * The sigmoid normalization function replaces each score
  * <code>x<sub>i</sub></code> with
  * <code>1 / (1 + exp(-alpha x<sub>i</sub>))</code>, where <code>alpha</code>
  * is a user-specified constant.
  *
  * @author Nick Rizzolo
 **/
public class Sigmoid extends Normalizer
{
  /** The user-specified constant described above. */
  protected double alpha;


  /** Default constructor; sets {@link #alpha} to 1. */
  public Sigmoid() { this(1); }

  /**
    * Initializing constructor.
    *
    * @param a  The setting for {@link #alpha}.
   **/
  public Sigmoid(double a) { alpha = a; }


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
    for (int i = 0; i < array.length; ++i)
      array[i].score = 1 / (1 + Math.exp(-alpha * array[i].score));
    return scores;
  }
}

