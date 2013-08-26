package LBJ2.infer;

import java.util.Vector;


/**
  * A first order constant is either <code>true</code> or <code>false</code>.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderConstant extends FirstOrderConstraint
{
  /** The constant value. */
  private boolean constant;


  /**
    * Initializing constructor.
    *
    * @param v  The value of this constant.
   **/
  public FirstOrderConstant(boolean v) { constant = v; }


  /**
    * This method sets the given quantification variables to the given object
    * references and evaluates the expressions involving those variables in
    * this constraint's <code>FirstOrderEquality</code> children.
    *
    * @param o  The new object references for the enclosing quantification
    *           variables, in order of nesting.
   **/
  public void setQuantificationVariables(Vector o) { }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() { return new FirstOrderConstraint[0]; }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() { return constant; }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(java.util.AbstractMap m) { }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    return new PropositionalConstant(constant);
  }


  /**
    * The hash code of a <code>FirstOrderConstant</code> is the hash code of
    * the <code>Boolean</code> object formed from the constant.
    *
    * @return The hash code for this <code>FirstOrderConstant</code>.
   **/
  public int hashCode() { return new Boolean(constant).hashCode(); }


  /**
    * Two <code>FirstOrderConstant</code>s are equivalent when their constants
    * are equal.
    *
    * @return <code>true</code> iff the argument is a
    *         <code>FirstOrderConstant</code> set to the same value as this
    *         constant.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderConstant)) return false;
    FirstOrderConstant c = (FirstOrderConstant) o;
    return constant == c.constant;
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

