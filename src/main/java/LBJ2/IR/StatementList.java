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
public class StatementList extends List
{
  /** Default constructor. */
  public StatementList() { super(-1, -1, " "); }

  /**
    * Initializing constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public StatementList(int line, int byteOffset) {
    super(line, byteOffset, " ");
  }

  /**
    * Initializing constructor.  Requires its argument to be
    * non-<code>null</code>.
    *
    * @param s  A single <code>Statement</code> with which to initialize this
    *           list.
   **/
  public StatementList(Statement s) {
    super(s.line, s.byteOffset, " ");
    list.add(s);
  }


  /**
    * Adds another <code>Statement</code> to the end of the list.
    *
    * @param s  A reference to the <code>Statement</code> to be added.
   **/
  public void add(Statement s) { list.add(s); }


  /**
    * Adds all the <code>Statement</code>s in another
    * <code>StatementList</code> to the end of this
    * <code>StatementList</code>.
    *
    * @param s  The list to be added.
   **/
  public void addAll(StatementList s) { list.addAll(s.list); }


  /**
    * Transforms the list into an array of statements.
    *
    * @return An array of statements containing references to every statement
    *         in the list.
   **/
  public Statement[] toArray() {
    return (Statement[]) list.toArray(new Statement[list.size()]);
  }


  /**
    * Returns an iterator used specifically to access the elements of this
    * list.
    *
    * @return An iterator used specifically to access the elements of this
    *         list.
   **/
  public StatementListIterator listIterator() {
    return new StatementListIterator();
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
    StatementList clone = new StatementList();
    for (Iterator i = list.iterator(); i.hasNext(); )
      clone.list.add(((Statement) i.next()).clone());
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
  public class StatementListIterator extends NodeListIterator
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
    public Statement nextItem() { return (Statement) I.next(); }


    /**
      * Returns the previous element in the list. This method may be called
      * repeatedly to iterate through the list backwards, or intermixed with
      * calls to next to go back and forth. (Note that alternating calls to
      * next and previous will return the same element repeatedly.)
      *
      * @return The previous AST node in the list.
     **/
    public Statement previousItem() { return (Statement) I.previous(); }
  }
}

