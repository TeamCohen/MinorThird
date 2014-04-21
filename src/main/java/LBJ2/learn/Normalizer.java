package LBJ2.learn;

import LBJ2.classify.ScoreSet;


/**
  * A normalizer is a function of a <code>ScoreSet</code> producing normalized
  * scores.  It is left to the implementing subclass to define the term
  * "normalized".
  *
  * @author Nick Rizzolo
 **/
public abstract class Normalizer
{
  /**
    * Normalizes the given <code>ScoreSet</code>; its scores are modified in
    * place before it is returned.
    *
    * @param scores The set of scores to normalize.
    * @return The normalized set of scores.
   **/
  abstract public ScoreSet normalize(ScoreSet scores);
}

