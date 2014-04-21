package LBJ2.infer;

import java.util.Vector;


/**
  * Represents a first order constraint involving a binary operator.
  *
  * @author Nick Rizzolo
 **/
public abstract class FirstOrderBinaryConstraint extends FirstOrderConstraint
{
  /** The constraint on the left of the operator. */
  protected FirstOrderConstraint left;
  /** The constraint on the right of the operator. */
  protected FirstOrderConstraint right;


  /**
    * Initializing constructor.
    *
    * @param l  The constraint on the left of the operator.
    * @param r  The constraint on the right of the operator.
   **/
  public FirstOrderBinaryConstraint(FirstOrderConstraint l,
                                    FirstOrderConstraint r) {
    left = l;
    right = r;
  }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(java.util.AbstractMap m) {
    left.consolidateVariables(m);
    right.consolidateVariables(m);
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
    left.setQuantificationVariables(o);
    right.setQuantificationVariables(o);
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() {
    return new FirstOrderConstraint[]{ left, right };
  }
}

