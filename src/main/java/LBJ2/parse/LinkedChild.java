package LBJ2.parse;

import java.io.Serializable;


/**
  * A <code>LinkedChild</code> is the child of a <code>LinkedVector</code>.
  * <code>LinkedVector</code>s use the methods of this interface to maintain
  * links between parent and child.
  *
  * @see LinkedVector
  * @author Nick Rizzolo
 **/
public abstract class LinkedChild implements Cloneable, Serializable
{
  /** A link to this child's parent. */
  public LinkedVector parent;
  /** A link to the previous child in the parent vector. */
  public LinkedChild previous;
  /** A link to the next child in the parent vector. */
  public LinkedChild next;
  /** The offset into the raw data input file at which this child starts. */
  public int start;
  /** The offset into the raw data input file at which this child ends. */
  public int end;
  /** Space for a label for this linked child. */
  public String label;


  /** Does nothing. */
  protected LinkedChild() { }

  /**
    * Useful when the information that this child represents is parsed
    * forwards.
    *
    * @param p  The previous child in the parent vector.
   **/
  protected LinkedChild(LinkedChild p) { this(p, -1, -1); }

  /**
    * Constructor that sets the byte offsets of this child.
    *
    * @param s  The offset at which this child starts.
    * @param e  The offset at which this child ends.
   **/
  public LinkedChild(int s, int e) { this(null, s, e); }

  /**
    * Useful when the information that this child represents is parsed
    * forwards.
    *
    * @param p  The previous child in the parent vector.
    * @param s  The offset at which this child starts.
    * @param e  The offset at which this child ends.
   **/
  public LinkedChild(LinkedChild p, int s, int e) {
    previous = p;
    start = s;
    end = e;
  }


  /** Returns a shallow clone of this object. */
  public Object clone() {
    LinkedChild clone = null;
    try { clone = (LinkedChild) super.clone(); }
    catch (Exception e) {
      System.err.println("Problem with LinkedChild clone: " + e);
      System.exit(1);
    }

    return clone;
  }
}

