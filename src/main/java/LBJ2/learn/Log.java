package LBJ2.learn;

import LBJ2.classify.Score;
import LBJ2.classify.ScoreSet;


/**
  * Simply turns each score <i>s</i> in the {@link ScoreSet} returned by the
  * specified {@link Normalizer} into <i>log(s)</i>.
  *
  * @author Nick Rizzolo
 **/
public class Log extends Normalizer
{
  /** This normalizer runs before applying the <i>log</i> function. */
  protected Normalizer first;


  /** This constructor provided for use by the LBJ compiler only. */
  public Log() { }

  /**
    * Initializing constructor.
    *
    * @param n  This normalizer runs before applying the <i>log</i> function.
   **/
  public Log(Normalizer n) { first = n; }


  /**
    * Normalizes the given <code>ScoreSet</code>; its scores are modified in
    * place before it is returned.
    *
    * @param scores The set of scores to normalize.
    * @return The normalized set of scores.
   **/
  public ScoreSet normalize(ScoreSet scores) {
    Score[] array = first.normalize(scores).toArray();
    for (int i = 0; i < array.length; ++i)
      array[i].score = Math.log(array[i].score);
    return scores;
  }
}

