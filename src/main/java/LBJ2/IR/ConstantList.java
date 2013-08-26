package LBJ2.IR;

import java.util.Iterator;
import LBJ2.Pass;
import LBJ2.util.ByteString;


/**
  * Currently, this is just a wrapper class for <code>LinkedList</code>.  The
  * code that uses it looks a little cleaner when casts are all taken care of
  * automatically.
  *
  * @author Nick Rizzolo
 **/
public class ConstantList extends List
{
  /** Default constructor. */
  public ConstantList() { super(-1, -1, ", "); }

  /**
    * Initializing constructor.  Requires its argument to be
    * non-<code>null</code>.
    *
    * @param c  A single <code>Constant</code> with which to initialize this
    *           list.
   **/
  public ConstantList(Constant c) {
    super(c.line, c.byteOffset, ", ");
    list.add(c);
  }

  /**
    * Creates an entire list from an array of values.
    *
    * @param a  The array of constant values.
   **/
  public ConstantList(String[] a) {
    super(-1, -1, ", ");
    for (int i = 0; i < a.length; ++i) list.add(new Constant(a[i]));
  }

  /**
    * Creates an entire list from an array of values.
    *
    * @param a  The array of constant values.
   **/
  public ConstantList(ByteString[] a) {
    super(-1, -1, ", ");
    for (int i = 0; i < a.length; ++i)
      list.add(new Constant(a[i].toString()));
  }


  /**
    * Adds another <code>Constant</code> to the end of the list.
    *
    * @param c  A reference to the <code>Constant</code> to be added.
   **/
  public void add(Constant c) { list.add(c); }


  /**
    * Adds all the <code>Constant</code>s in another <code>ConstantList</code>
    * to the end of this <code>ConstantList</code>.
    *
    * @param l  The list to be added.
   **/
  public void addAll(ConstantList l) { list.addAll(l.list); }


  /**
    * Transforms the list into an array of expressions.
    *
    * @return An array of constants containing references to every constant in
    *         the list.
   **/
  public Constant[] toArray() {
    return (Constant[]) list.toArray(new Constant[list.size()]);
  }


  /**
    * Two <code>ConstantList</code>s are equal when they contain the same
    * elements in the same order as evaluated by the
    * <code>Constant.equals(Object)</code> method.
    *
    * @param o  The object to test equality with.
    * @return <code>true</code> iff this <code>ConstantList</code> is
    *         equivalent to the specified object as described above.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof ConstantList)) return false;
    ConstantList list = (ConstantList) o;
    if (list.size() != size()) return false;

    ASTNodeIterator I = iterator();
    for (ASTNodeIterator J = list.iterator(); J.hasNext(); )
      if (!I.next().equals(J.next())) return false;
    return true;
  }


  /** A hash code based on the hash codes of the elements of the list. */
  public int hashCode() {
    int result = 0;
    for (ASTNodeIterator I = iterator(); I.hasNext(); )
      result = 31 * result + I.next().hashCode();
    return result;
  }


  /**
    * Returns an iterator used specifically to access the elements of this
    * list.
    *
    * @return An iterator used specifically to access the elements of this
    *         list.
   **/
  public ConstantListIterator listIterator() {
    return new ConstantListIterator();
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() { return listIterator(); }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    ConstantList clone = new ConstantList();
    for (Iterator i = list.iterator(); i.hasNext(); )
      clone.list.add(((Constant) i.next()).clone());
    return clone;
  }


  /**
    * Ensures that the correct <code>run()</code> method is called for this
    * type of node.
    *
    * @param pass The pass whose <code>run()</code> method should be called.
   **/
  public void runPass(Pass pass) { pass.run(this); }


  /**
    * Used to iterate though the children of a list of AST nodes.  The entire
    * interface of <code>java.util.ListIterator</code> is exposed through this
    * class.
    *
    * @author Nick Rizzolo
   **/
  public class ConstantListIterator extends NodeListIterator
  {
    /**
      * Returns the next AST node in the list.  This method may be called
      * repeatedly to iterate through the list, or intermixed with calls to
      * <code>previous()</code> to go back and forth.  (Note that alternating
      * calls to <code>next()</code> and <code>previous()</code> will return
      * the same element repeatedly.)
      *
      * @return The next AST node in the list.
     **/
    public Constant nextItem() { return (Constant) I.next(); }


    /**
      * Returns the previous element in the list. This method may be called
      * repeatedly to iterate through the list backwards, or intermixed with
      * calls to next to go back and forth. (Note that alternating calls to
      * next and previous will return the same element repeatedly.)
      *
      * @return The previous AST node in the list.
     **/
    public Constant previousItem() { return (Constant) I.previous(); }
  }
}

