package LBJ2.infer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;


/**
  * Represents a first order constraint with an arbitrary number of arguments,
  * usually assumed to be greater than or equal to 2.
  *
  * @author Nick Rizzolo
 **/
public abstract class FirstOrderNAryConstraint extends FirstOrderConstraint
{
  /** The children of the operator. */
  protected HashSet children;


  /** Default constructor. */
  public FirstOrderNAryConstraint() { children = new HashSet(); }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(java.util.AbstractMap m) {
    for (Iterator I = children.iterator(); I.hasNext(); )
      ((FirstOrderConstraint) I.next()).consolidateVariables(m);
  }


  /**
    * This method sets the given quantification variables to the given object
    * references and evaluates the expressions involving those variables in
    * this constraint's <code>FirstOrderEquality</code> children.
    *
    * @param o  The new object references for the enclosing quantification
    *           variables, in order of nesting.
   **/
  public void setQuantificationVariables(Vector o) {
    for (Iterator I = children.iterator(); I.hasNext(); )
      ((FirstOrderConstraint) I.next()).setQuantificationVariables(o);
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() {
    return (PropositionalConstraint[])
           children.toArray(new PropositionalConstraint[children.size()]);
  }


  /**
    * Determines whether the given constraint is a term of this constraint.
    *
    * @param c  The given constraint.
    * @return <code>true</code> iff the given constraint is contained in this
    *         constraint.
   **/
  public boolean contains(FirstOrderConstraint c) {
    return children.contains(c);
  }


  /**
    * Returns the number of terms in this constraint.
    *
    * @return The number of terms in this constraint.
   **/
  public int size() { return children.size(); }


  /**
    * If the given constraint has the same type as this constraint, its terms
    * are merged into this constraint; otherwise, it is added as a new term.
    *
    * @param c  The constraint to add.
   **/
  abstract public void add(FirstOrderConstraint c);
}

