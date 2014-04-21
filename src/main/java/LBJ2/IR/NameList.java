package LBJ2.IR;

import java.util.Iterator;
import LBJ2.Pass;


/**
  * Currently, this is just a wrapper class for <code>LinkedList</code>.  The
  * code that uses it looks a little cleaner when casts are all taken care of
  * automatically.
  *
  * @author Nick Rizzolo
 **/
public class NameList extends List
{
  /** Default constructor. */
  public NameList() { super(-1, -1, ", "); }

  /**
    * Initializing constructor.  Requires its argument to be
    * non-<code>null</code>.
    *
    * @param n  A single <code>Name</code> with which to initialize this list.
   **/
  public NameList(Name n) {
    super(n.line, n.byteOffset, ", ");
    list.add(n);
  }


  /**
    * Adds another <code>Name</code> to the end of the list.
    *
    * @param n  A reference to the <code>Name</code> to be added.
   **/
  public void add(Name n) { list.add(n); }


  /**
    * Adds all the <code>Name</code>s in another <code>NameList</code> to the
    * end of this <code>NameList</code>.
    *
    * @param l  The list to be added.
   **/
  public void addAll(NameList l) { list.addAll(l.list); }


  /**
    * Transforms the list into an array of expressions.
    *
    * @return An array of constants containing references to every constant in
    *         the list.
   **/
  public Name[] toArray() {
    return (Name[]) list.toArray(new Name[list.size()]);
  }


  /**
    * Returns an iterator used specifically to access the elements of this
    * list.
    *
    * @return An iterator used specifically to access the elements of this
    *         list.
   **/
  public NameListIterator listIterator() { return new NameListIterator(); }


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
    NameList clone = new NameList();
    for (Iterator i = list.iterator(); i.hasNext(); )
      clone.list.add(((Name) i.next()).clone());
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
  public class NameListIterator extends NodeListIterator
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
    public Name nextItem() { return (Name) I.next(); }


    /**
      * Returns the previous element in the list. This method may be called
      * repeatedly to iterate through the list backwards, or intermixed with
      * calls to next to go back and forth. (Note that alternating calls to
      * next and previous will return the same element repeatedly.)
      *
      * @return The previous AST node in the list.
     **/
    public Name previousItem() { return (Name) I.previous(); }
  }
}

