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
public class ImportList extends List
{
  /** Default constructor. */
  public ImportList() { super(-1, -1, " "); }

  /**
    * Initializing constructor.  Requires its argument to be
    * non-<code>null</code>.
    *
    * @param d  A single <code>ImportDeclaration</code> with which to
    *           initialize this list.
   **/
  public ImportList(ImportDeclaration d) {
    super(d.line, d.byteOffset, " ");
    list.add(d);
  }


  /**
    * Adds another <code>ImportDeclaration</code> to the end of the list.
    *
    * @param d  A reference to the <code>ImportDeclaration</code> to be added.
   **/
  public void add(ImportDeclaration d) { list.add(d); }


  /**
    * Adds all the <code>ImportDeclaration</code>s in another
    * <code>ImportList</code> to the end of this
    * <code>ImportList</code>.
    *
    * @param d  The list to be added.
   **/
  public void addAll(ImportList d) { list.addAll(d.list); }


  /**
    * Transforms the list into an array of statements.
    *
    * @return An array of statements containing references to every statement
    *         in the list.
   **/
  public ImportDeclaration[] toArray() {
    return
      (ImportDeclaration[]) list.toArray(new ImportDeclaration[list.size()]);
  }


  /**
    * Returns an iterator used specifically to access the elements of this
    * list.
    *
    * @return An iterator used specifically to access the elements of this
    *         list.
   **/
  public ImportListIterator listIterator() {
    return new ImportListIterator();
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
    ImportList clone = new ImportList();
    for (Iterator i = list.iterator(); i.hasNext(); )
      clone.list.add(((ImportDeclaration) i.next()).clone());
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
  public class ImportListIterator extends NodeListIterator
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
    public ImportDeclaration nextItem() {
      return (ImportDeclaration) I.next();
    }


    /**
      * Returns the previous element in the list. This method may be called
      * repeatedly to iterate through the list backwards, or intermixed with
      * calls to next to go back and forth. (Note that alternating calls to
      * next and previous will return the same element repeatedly.)
      *
      * @return The previous AST node in the list.
     **/
    public ImportDeclaration previousItem() {
      return (ImportDeclaration) I.previous();
    }
  }
}

