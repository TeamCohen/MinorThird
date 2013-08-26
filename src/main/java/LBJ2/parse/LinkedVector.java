package LBJ2.parse;

import java.util.Vector;


/**
  * A <code>LinkedVector</code> is used to store a vector of
  * <code>LinkedChild</code>ren which all maintain links between each other
  * and the parent <code>LinkedVector</code>.
  *
  * @see LinkedChild
  * @author Nick Rizzolo
 **/
public class LinkedVector extends LinkedChild
{
  /** The linked vector is simply represented as a vector of children. */
  protected Vector children;


  /** Initializes the vector. */
  public LinkedVector() { children = new Vector(); }

  /**
    * Constructor for when only a single child from anywhere in this vector is
    * available.  It is assumed that the <code>previous</code> and
    * <code>next</code> links are filled in by every child.
    *
    * @param c  Any child in this vector.
   **/
  public LinkedVector(LinkedChild c) {
    children = new Vector();

    while (c.previous != null) c = c.previous;
    start = c.start;

    for (; c.next != null; c = c.next) {
      c.parent = this;
      children.add(c);
    }

    c.parent = this;
    children.add(c);
    end = c.end;
  }

  /**
    * Useful when the information that this child represents is parsed
    * forwards.
    *
    * @param p  The previous child in the parent vector.
   **/
  public LinkedVector(LinkedVector p) {
    super(p);
    children = new Vector();
  }

  /**
    * Constructor that sets the character offsets of this vector.
    *
    * @param s  The offset at which this sentence starts.
    * @param e  The offset at which this sentence ends.
   **/
  public LinkedVector(int s, int e) {
    super(s, e);
    children = new Vector();
  }

  /**
    * Constructor for when only a single child from anywhere in this vector is
    * available.  It is assumed that the <code>previous</code> and
    * <code>next</code> links are filled in by every child.
    *
    * @param c  Any child in this vector.
    * @param s  The offset at which this sentence starts.
    * @param e  The offset at which this sentence ends.
   **/
  public LinkedVector(LinkedChild c, int s, int e) {
    super(s, e);
    children = new Vector();
    while (c.previous != null) c = c.previous;
    for (; c != null; c = c.next) add(c);
  }

  /**
    * Useful when the information that this child represents is parsed
    * forwards.
    *
    * @param p  The previous child in the parent vector.
    * @param s  The offset at which this sentence starts.
    * @param e  The offset at which this sentence ends.
   **/
  public LinkedVector(LinkedVector p, int s, int e) {
    super(p, s, e);
    children = new Vector();
  }


  /**
    * Adds the specified child to the end of the vector, informing the child
    * of its parent and index and linking the child to its only neighbor
    * (which was previously the last child in the vector).
    *
    * @param c  The child to add.
   **/
  public boolean add(LinkedChild c) {
    c.parent = this;
    if (children.size() > 0) {
      LinkedChild p = get(children.size() - 1);
      p.next = c;
      c.previous = p;
    }

    return children.add(c);
  }


  /**
    * Removes the child at the specified index.
    *
    * @param i  The index of the child to remove.
    * @return The child removed, or <code>null</code> if there was no child at
    *         that index.
   **/
  public LinkedChild remove(int i) {
    LinkedChild before =
      (i - 1 < 0) ? null : (LinkedChild) children.get(i - 1);
    LinkedChild after =
      (i + 1 >= children.size()) ? null : (LinkedChild) children.get(i + 1);

    if (before != null) before.next = after;
    if (after != null) after.previous = before;

    LinkedChild removed = null;
    try { removed = (LinkedChild) children.remove(i); }
    catch (ArrayIndexOutOfBoundsException e) { return null; }

    removed.parent = null;
    removed.next = removed.previous = null;
    return removed;
  }


  /**
    * Inserts the specified child into the specified index.  All children that
    * previously had index greater than or equal to the specified index are
    * shifted up one.
    *
    * @param c  The child to insert.
    * @param i  The index at which to insert the child.
    * @return <code>true</code> if and only if the insert was successful.
   **/
  public boolean insert(LinkedChild c, int i) {
    try { children.insertElementAt(c, i); }
    catch (ArrayIndexOutOfBoundsException e) { return false; }

    c.parent = this;
    c.previous = (i - 1 < 0) ? null : (LinkedChild) children.get(i - 1);
    c.next =
      (i + 1 >= children.size()) ? null : (LinkedChild) children.get(i + 1);

    if (c.previous != null) c.previous.next = c;
    if (c.next != null) c.next.previous = c;

    return true;
  }


  /**
    * Retrieves the child at the specified index in the vector.
    *
    * @param i  The index from which to retrieve a child.
    * @return The child at the specified index, or <code>null</code> if there
    *         was no child at that index.
   **/
  public LinkedChild get(int i) {
    try { return (LinkedChild) children.get(i); }
    catch (ArrayIndexOutOfBoundsException e) { }
    return null;
  }


  /**
    * Returns the size of the vector.
    *
    * @return The size of the vector.
   **/
  public int size() { return children.size(); }


  /**
    * Returns a clone of this object that is deep in the sense that all of the
    * children objects are cloned.
    *
    * @return A deep clone of this object.
   **/
  public Object clone() {
    LinkedVector clone = (LinkedVector) super.clone();
    clone.children = (Vector) clone.children.clone();
    // This may look inefficient, but there is a purpose.  Subclasses of
    // LinkedVector that don't define any new non-primitive member fields will
    // not need to override this method, as it already produces an object of
    // the subclass's type.
    for (int i = 0; i < size(); ++i)
      clone.insert((LinkedChild) clone.remove(i).clone(), i);
    return clone;
  }
}

