package LBJ2.IR;

import java.util.HashSet;
import java.util.Iterator;
import LBJ2.Pass;


/**
  * Currently, this is just a wrapper class for <code>LinkedList</code>.  The
  * code that uses it looks a little cleaner when casts are all taken care of
  * automatically.
  *
  * @author Nick Rizzolo
 **/
public class ExpressionList extends List
{
  /** Default constructor. */
  public ExpressionList() { super(-1, -1, ", "); }


  /**
    * Initializing constructor.  Does not require its argument to be
    * non-<code>null</code>.
    *
    * @param e  A single <code>Expression</code> with which to initialize this
    *           list.
   **/
  public ExpressionList(Expression e) {
    super(e == null ? -1 : e.line, e == null ? -1 : e.byteOffset, ", ");
    list.add(e);
  }

  /**
    * Initializing constructor.  Does not require its first argument to be
    * non-<code>null</code>, however this is required of the second argument.
    *
    * @param e  A single <code>Expression</code> with which to initialize this
    *           list.
    * @param l  A list of <code>Expression</code>s which will also be added to
    *           the list.
   **/
  public ExpressionList(Expression e, ExpressionList l) {
    super(e == null ? -1 : e.line, e == null ? -1 : e.byteOffset, ", ");
    list.add(e);
    addAll(l);
  }


  /**
    * Adds another <code>Expression</code> to the end of the list.
    *
    * @param e  A reference to the <code>Expression</code> to be added.
   **/
  public void add(Expression e) { list.add(e); }


  /**
    * Adds all the <code>Expression</code>s in another
    * <code>ExpressionList</code> to the end of this
    * <code>ExpressionList</code>.
    *
    * @param e  The list to be added.
   **/
  public void addAll(ExpressionList e) { list.addAll(e.list); }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() {
    HashSet result = new HashSet();
    for (ExpressionListIterator I = listIterator(); I.hasNext(); )
      result.addAll(I.nextItem().getVariableTypes());
    return result;
  }


  /**
    * Determines if there are any quantified variables in this expression.
    * This method cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public boolean containsQuantifiedVariable() {
    for (ExpressionListIterator I = listIterator(); I.hasNext(); )
      if (I.nextItem().containsQuantifiedVariable()) return true;
    return false;
  }


  /**
    * Transforms the list into an array of expressions.
    *
    * @return An array of expressions containing references to every
    *         expression in the list.
   **/
  public Expression[] toArray() {
    return (Expression[]) list.toArray(new Expression[list.size()]);
  }


  /**
    * Returns an iterator used specifically to access the elements of this
    * list.
    *
    * @return An iterator used specifically to access the elements of this
    *         list.
   **/
  public ExpressionListIterator listIterator() {
    return new ExpressionListIterator();
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
    ExpressionList clone = new ExpressionList();
    for (Iterator i = list.iterator(); i.hasNext(); )
      clone.list.add(((Expression) i.next()).clone());
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
  public class ExpressionListIterator extends NodeListIterator
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
    public Expression nextItem() { return (Expression) I.next(); }


    /**
      * Returns the previous element in the list. This method may be called
      * repeatedly to iterate through the list backwards, or intermixed with
      * calls to next to go back and forth. (Note that alternating calls to
      * next and previous will return the same element repeatedly.)
      *
      * @return The previous AST node in the list.
     **/
    public Expression previousItem() { return (Expression) I.previous(); }
  }
}

