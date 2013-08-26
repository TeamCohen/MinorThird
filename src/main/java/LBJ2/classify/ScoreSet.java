package LBJ2.classify;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
  * A score set is simply a set of <code>Score</code>s.
  *
  * @see Score
  * @author Nick Rizzolo
 **/
public class ScoreSet implements Cloneable
{
  /** The scores in this set, indexed by the discrete classification. */
  private Map set;


  /** Default constructor. */
  public ScoreSet() { this(null, null); }

  /**
    * The elements of the two argument arrays are assumed to be pair-wise
    * associated with each other.
    *
    * @param values The classification values being scored.
    * @param scores The scores of the classification values.
   **/
  public ScoreSet(String[] values, double[] scores) {
    set = new TreeMap();
    if (values == null || scores == null) return;
    for (int i = 0; i < values.length && i < scores.length; ++i)
      put(values[i], scores[i]);
  }

  /**
    * The elements of the array are added to the set.
    *
    * @param scores The scores to add to the set.
   **/
  public ScoreSet(Score[] scores) {
    set = new TreeMap();
    if (scores == null) return;
    for (int i = 0; i < scores.length; ++i)
      set.put(scores[i].value, scores[i]);
  }


  /** Returns the number of scores in this set. */
  public int size() { return set.size(); }


  /**
    * Sets the score for a particular classification value.
    *
    * @param v  The classification value.
    * @param s  The score.
   **/
  public void put(String v, double s) { set.put(v, new Score(v, s)); }


  /**
    * Retrieves the set of values that have scores associated with them in
    * this score set.
    *
    * @return A set of <code>String</code>s.
   **/
  public Set values() { return set.keySet(); }


  /**
    * Returns the double precision score for a particular classification
    * value.
    *
    * @param v  The classification value.
    * @return The associated score.
   **/
  public double get(String v) { return ((Score) set.get(v)).score; }


  /**
    * Retrieves the {@link Score} object associated with the given
    * classification value.
    *
    * @param v  The classification value.
    * @return The associated {@link Score} object.
   **/
  public Score getScore(String v) { return (Score) set.get(v); }


  /** Retrieves the value with the highest score in this set. */
  public String highScoreValue() {
    String result = null;
    double highScore = Double.NEGATIVE_INFINITY;

    for (Iterator I = set.entrySet().iterator(); I.hasNext(); ) {
      Map.Entry e = (Map.Entry) I.next();
      double score = ((Score) e.getValue()).score;
      if (score > highScore) {
        highScore = score;
        result = (String) e.getKey();
      }
    }

    return result;
  }


  /**
    * Returns an array view of the <code>Score</code>s contained in this set.
    *
    * @return An array of <code>Score</code>s.
   **/
  public Score[] toArray() {
    return (Score[]) set.values().toArray(new Score[set.size()]);
  }


  /**
    * The string representation of a <code>ScoreSet</code> is the
    * concatenation of the string representations of each <code>Score</code>
    * in the set sorted by value, separated by commas, and surrounded by curly
    * braces.
    *
    * @return The string representation of a <code>ScoreSet</code>.
   **/
  public String toString() {
    String result = "{";

    if (set.size() > 0) {
      Score[] scores = toArray();
      Arrays.sort(scores);
      result += " " + scores[0];
      for (int i = 1; i < scores.length; ++i) result += ", " + scores[i];
    }

    return result + " }";
  }


  /**
    * Produces a deep copy of this object.
    *
    * @return A deep copy of this object.
   **/
  public Object clone() {
    Score[] scores = toArray();
    for (int i = 0; i < scores.length; ++i)
      scores[i] = (Score) scores[i].clone();
    return new ScoreSet(scores);
  }
}

