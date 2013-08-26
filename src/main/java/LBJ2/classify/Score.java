package LBJ2.classify;


/**
  * A score is a number produced by a learner that indicates the degree to
  * which a particular discrete classification is appropriate for a given
  * object.  The scores for all possible discrete classifications given an
  * object need not be positive or sum to one.  A <code>Score</code> object
  * simply contains a score and the associated discrete classification.
  *
  * @author Nick Rizzolo
 **/
public class Score implements Comparable, Cloneable
{
  /** The discrete classification associated with this score. */
  public String value;
  /** The score. */
  public double score;


  /**
    * Initializes both member variables.
    *
    * @param v  The discrete classification.
    * @param s  The score.
   **/
  public Score(String v, double s) {
    value = v;
    score = s;
  }


  /**
    * This method is implemented so that a collection of <code>Score</code>s
    * will be sorted first by value and then by score.
    *
    * @param o  The object to compare against.
    * @return A negative integer, zero, or a positive integer if this object
    *         is less than, equal to, or greater than the specified object
    *         respectively.
   **/
  public int compareTo(Object o) {
    if (!(o instanceof Score)) return -1;
    Score s = (Score) o;
    int result = value.compareTo(s.value);
    if (result == 0)
      result = new Double(score).compareTo(new Double(s.score));
    return result;
  }


  /**
    * The string representation of a <code>Score</code> is the value followed
    * by the score separated by a colon.
    *
    * @return The string representation of a <code>Score</code>.
   **/
  public String toString() { return value + " : " + score; }


  /**
    * Produces a deep copy of this object.
    *
    * @return A deep copy of this object.
   **/
  public Object clone() {
    Object result = null;

    try { result = super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning " + getClass().getName() + ":");
      e.printStackTrace();
      System.exit(1);
    }

    return result;
  }
}

