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
public class ClassifierExpressionList extends List
{
  /** Default constructor. */
  public ClassifierExpressionList() { super(-1, -1, ", "); }


  /**
    * Initializing constructor.  Does not require its argument to be
    * non-<code>null</code>.
    *
    * @param e  A single <code>Expression</code> with which to initialize this
    *           list.
   **/
  public ClassifierExpressionList(ClassifierExpression e) {
    super(e == null ? -1 : e.line, e == null ? -1 : e.byteOffset, ", ");
    list.add(e);
  }


  /**
    * Adds another <code>ClassifierExpression</code> to the end of the list.
    *
    * @param e  A reference to the <code>ClassifierExpression</code> to be
    *           added.
   **/
  public void add(ClassifierExpression e) { list.add(e); }


  /**
    * Adds all the <code>ClassifierExpression</code>s in another
    * <code>ClassifierExpressionList</code> to the end of this
    * <code>ClassifierExpressionList</code>.
    *
    * @param e  The list to be added.
   **/
  public void addAll(ClassifierExpressionList e) { list.addAll(e.list); }


  /**
    * Transforms the list into an array of expressions.
    *
    * @return An array of expressions containing references to every
    *         expression in the list.
   **/
  public ClassifierExpression[] toArray() {
    return (ClassifierExpression[])
           list.toArray(new ClassifierExpression[list.size()]);
  }


  /**
    * Returns an iterator used specifically to access the elements of this
    * list.
    *
    * @return An iterator used specifically to access the elements of this
    *         list.
   **/
  public ClassifierExpressionListIterator listIterator() {
    return new ClassifierExpressionListIterator();
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
    ClassifierExpressionList clone = new ClassifierExpressionList();
    for (Iterator i = list.iterator(); i.hasNext(); )
      clone.list.add(((ClassifierExpression) i.next()).clone());
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
  public class ClassifierExpressionListIterator extends NodeListIterator
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
    public ClassifierExpression nextItem() {
      return (ClassifierExpression) I.next();
    }


    /**
      * Returns the previous element in the list. This method may be called
      * repeatedly to iterate through the list backwards, or intermixed with
      * calls to next to go back and forth. (Note that alternating calls to
      * next and previous will return the same element repeatedly.)
      *
      * @return The previous AST node in the list.
     **/
    public ClassifierExpression previousItem() {
      return (ClassifierExpression) I.previous();
    }
  }
}

