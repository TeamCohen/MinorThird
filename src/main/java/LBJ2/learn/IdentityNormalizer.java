package LBJ2.learn;

import LBJ2.classify.ScoreSet;


/**
  * This <code>Normalizer</code> simply returns the same <code>ScoreSet</code>
  * it was passed as input without modifying anything.
  *
  * @author Nick Rizzolo
 **/
public class IdentityNormalizer extends Normalizer
{
  /**
    * Simply returns the argument.
    *
    * @param scores The set of scores to normalize.
    * @return The normalized set of scores.
   **/
  public ScoreSet normalize(ScoreSet scores) { return scores; }
}

