package LBJ2.IR;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;


/**
  * Currently, this is just a wrapper class for <code>LinkedList</code>.  The
  * code that uses it looks a little cleaner when casts are all taken care of
  * automatically.
  *
  * @author Nick Rizzolo
 **/
abstract public class List extends ASTNode
{
  /** (&not;&oslash;) The list being wrapped. */
  protected LinkedList list;
  /**
    * The characters appearing in between elements of the list in its string
    * representation.
   **/
  protected String separator;


  /**
    * Full constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param s          The list's element separator.
   **/
  public List(int line, int byteOffset, String s) {
    super(line, byteOffset);
    list = new LinkedList();
    separator = s;
  }


  /**
    * Returns the size of the list.
    *
    * @return The number of elements currently in the list.
   **/
  public int size() { return list.size(); }


  /**
    * Returns the separating characters.
    *
    * @return The value of <code>separator</code>.
   **/
  public String getSeparator() { return separator; }


  /** Sorts the list according to their natural ordering. */
  public void sort() { Collections.sort(list); }


  /**
    * Sorts the list according to the order induced by the specified
    * comparator.
    *
    * @param c  A comparator that determines the relative ordering of two
    *           elements in the list.
   **/
  public void sort(Comparator c) { Collections.sort(list, c); }


  /**
    * Writes a string representation of this <code>ASTNode</code> to the
    * specified buffer.  The representation written is parsable by the LBJ2
    * compiler, but not very readable.
    *
    * @param buffer The buffer to write to.
   **/
  protected void writeBuffer(StringBuffer buffer, String separate) {
    ASTNodeIterator I = iterator();
    if (!I.hasNext()) return;

    I.next().write(buffer);
    while (I.hasNext()) {
      buffer.append(separate);
      I.next().write(buffer);
    }
  }


  /**
    * Writes a string representation of this <code>ASTNode</code> to the
    * specified buffer.  The representation written is parsable by the LBJ2
    * compiler, but not very readable.
    *
    * @param buffer The buffer to write to.
   **/
  public void write(StringBuffer buffer) { writeBuffer(buffer, separator); }


  /**
    * Determines whether this list is equivalent to another object.
    *
    * @param o  The other object.
    * @return <code>true</code> iff this list is equivalent to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (o == null || !o.getClass().equals(getClass())) return false;
    List list = (List) o;
    if (size() != list.size()) return false;
    NodeListIterator I1 = new NodeListIterator();
    NodeListIterator I2 = list.new NodeListIterator();
    while (I1.hasNext())
      if (!I1.next().equals(I2.next())) return false;
    return true;
  }


  /** A hash code based on the hash codes of the elements of the list. */
  public int hashCode() {
    int result = 53;
    for (NodeListIterator I = new NodeListIterator(); I.hasNext(); ) {
      Object element = I.next();
      result = 31 * result + (element == null ? 7 : element.hashCode());
    }
    return result;
  }


  /**
    * Used to iterate though the children of a list of AST nodes.  The entire
    * interface of <code>java.util.ListIterator</code> is exposed through this
    * class.
    *
    * @author Nick Rizzolo
   **/
  public class NodeListIterator extends ASTNodeIterator
  {
    /** An iterator into <code>list</code>. */
    protected ListIterator I;


    /** Initializes <code>I</code>. */
    public NodeListIterator() { I = list.listIterator(); }


    /**
      * Inserts the specified node into the list.  The element is inserted
      * immediately before the next element that would be returned by
      * <code>next()</code>, if any, and after the next element that would be
      * returned by <code>previous()</code>, if any.  (If the list contains no
      * elements, the new element becomes the sole element on the list.)  The
      * new element is inserted before the implicit cursor: a subsequent call
      * to <code>next()</code> would be unaffected, and a subsequent call to
      * <code>previous()</code> would return the new element.  (This call
      * increases by one the value that would be returned by a call to
      * <code>nextIndex</code> or <code>previousIndex</code>.)
      *
      * @param n  The node to add.
     **/
    public void add(ASTNode n) { I.add(n); }


    /**
      * Returns <code>true</code> if this list iterator has more elements when
      * traversing the list in the forward direction.
      *
      * @return <code>true</code> if this list iterator has more elements when
      *         traversing the list in the forward direction.
     **/
    public boolean hasNext() { return I.hasNext(); }


    /**
      * Returns <code>true</code> if this list iterator has more elements when
      * traversing the list in the reverse direction.
      *
      * @return <code>true</code> if this list iterator has more elements when
      *         traversing the list in the reverse direction.
     **/
    public boolean hasPrevious() { return I.hasPrevious(); }


    /**
      * Returns the next AST node in the list.  This method may be called
      * repeatedly to iterate through the list, or intermixed with calls to
      * <code>previous()</code> to go back and forth.  (Note that alternating
      * calls to <code>next()</code> and <code>previous()</code> will return
      * the same element repeatedly.)
      *
      * @return The next AST node in the list.
     **/
    public ASTNode next() { return (ASTNode) I.next(); }


    /**
      * Returns the index of the node that would be returned by a subsequent
      * call to <code>next()</code>.  (Returns list size if the list iterator
      * is at the end of the list.)
      *
      * @return The index of the element that would be returned by a
      *         subsequent call to <code>next()</code>, or list size if list
      *         iterator is at end of list.
     **/
    public int nextIndex() { return I.nextIndex(); }


    /**
      * Returns the previous element in the list. This method may be called
      * repeatedly to iterate through the list backwards, or intermixed with
      * calls to next to go back and forth. (Note that alternating calls to
      * next and previous will return the same element repeatedly.)
      *
      * @return The previous AST node in the list.
     **/
    public ASTNode previous() { return (ASTNode) I.previous(); }


    /**
      * Returns the index of the node that would be returned by a subsequent
      * call to <code>previous()</code>.  (Returns -1 if the list iterator is
      * at the beginning of the list.)
      *
      * @return The index of the element that would be returned by a
      *         subsequent call to <code>previous()</code>, or -1 if list
      *         iterator is at the beginning of the list.
     **/
    public int previousIndex() { return I.previousIndex(); }


    /**
      * Removes from the list the last element that was returned by
      * <code>next()</code> or <code>previous</code>.  This call can only be
      * made once per call to <code>next()</code> or <code>previous</code>.
      * It can be made only if <code>add(ASTNode)</code> has not been called
      * after the last call to <code>next()</code> or <code>previous</code>.
     **/
    public void remove() { I.remove(); }


    /** Restarts the iterator. */
    public void reset() { I = list.listIterator(); }


    /**
      * Replaces the last element returned by <code>next()</code> or
      * <code>previous()</code> with the specified element.  This call can be
      * made only if neither <code>remove()</code> nor
      * <code>add(ASTNode)</code> have been called after the last call to
      * <code>next()</code> or <code>previous()</code>.
      *
      * @param n  The element with which to replace the last element returned
      *           by <code>next()</code> or <code>previous()</code>.
     **/
    public void set(ASTNode n) { I.set(n); }
  }
}

