package LBJ2.infer;

import java.util.Vector;


/**
  * Represents the negation operator applied to a first order constraint.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderNegation extends FirstOrderConstraint
{
  /** The constraint that the negation is applied to. */
  protected FirstOrderConstraint constraint;


  /**
    * Initializing constructor.
    *
    * @param c  The constraint to negate.
   **/
  public FirstOrderNegation(FirstOrderConstraint c) { constraint = c; }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(java.util.AbstractMap m) {
    constraint.consolidateVariables(m);
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
    constraint.setQuantificationVariables(o);
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() { return new FirstOrderConstraint[0]; }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() { return !constraint.evaluate(); }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    return new PropositionalNegation(constraint.propositionalize());
  }


  /**
    * The hash code of a <code>FirstOrderNegation</code> is the hash code of
    * its child constraint plus 1.
    *
    * @return The hash code for this <code>FirstOrderNegation</code>.
   **/
  public int hashCode() { return constraint.hashCode() + 1; }


  /**
    * Two <code>FirstOrderNegation</code>s are equivalent when their
    * constraints are equivalent.
    *
    * @return <code>true</code> iff the argument is a
    *         <code>FirstOrderNegation</code> of the same constraint.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderNegation)) return false;
    FirstOrderNegation n = (FirstOrderNegation) o;
    return constraint.equals(n.constraint);
  }


  /**
    * Calls the appropriate <code>visit(&middot;)</code> method of the given
    * <code>Inference</code> for this <code>Constraint</code>, as per the
    * visitor pattern.
    *
    * @param infer  The inference visiting this constraint.
   **/
  public void runVisit(Inference infer) { infer.visit(this); }
}

