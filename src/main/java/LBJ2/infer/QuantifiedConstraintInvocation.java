package LBJ2.infer;

import java.util.AbstractMap;
import java.util.Vector;


/**
  * Represents the invocation of a parameterized constraint nested inside at
  * least one quantification expression, where the parameter is a function of
  * the quantification variables.
  *
  * @author Nick Rizzolo
 **/
public class QuantifiedConstraintInvocation extends FirstOrderConstraint
{
  /** The parameterized constraint that has been invoked. */
  protected ParameterizedConstraint parameterized;
  /** The implementation of the function that computes the parameter. */
  protected InvocationArgumentReplacer replacer;
  /** The latest result of invoking <code>parameterized</code>. */
  protected FirstOrderConstraint constraint;


  /**
    * Initializing constructor.
    *
    * @param p    The invoked constraint.
    * @param iar  The parameter function implementation.
   **/
  public QuantifiedConstraintInvocation(ParameterizedConstraint p,
                                        InvocationArgumentReplacer iar) {
    parameterized = p;
    replacer = iar;
  }


  /**
    * If this method is called without first calling
    * <code>setQuantificationVariables(Vector)</code>, <code>false</code> will
    * be returned.
   **/
  public boolean evaluate() {
    return constraint != null && constraint.evaluate();
  }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(AbstractMap m) {
    if (constraint != null) constraint.consolidateVariables(m);
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
    if (replacer == null) {
      System.err.println(
          "LBJ ERROR: Attempting to set quantification variable with no "
          + "variable setter implementation provided.");
      System.exit(1);
    }

    replacer.setQuantificationVariables(o);
    constraint = parameterized.makeConstraint(replacer.compute());
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() { return new FirstOrderConstraint[0]; }


  /**
    * If this method is called without first calling
    * <code>setQuantificationVariables(Vector)</code>, the constant
    * representing <code>false</code> will be returned.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    if (constraint == null) return new PropositionalConstant(false);
    return constraint.propositionalize();
  }


  /**
    * The hash code of a <code>QuantifiedConstraintInvocation</code> is the
    * sum of the hash codes of its children.
    *
    * @return The hash code for this
    *         <code>QuantifiedConstraintInvocation</code>.
   **/
  public int hashCode() {
    return parameterized.hashCode() + replacer.hashCode();
  }


  /**
    * Two <code>QuantifiedConstraintInvocation</code>s are equivalent when
    * their children are equivalent.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>QuantifiedConstraintInvocation</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof QuantifiedConstraintInvocation)) return false;
    QuantifiedConstraintInvocation q = (QuantifiedConstraintInvocation) o;
    return parameterized.equals(q.parameterized) && replacer == q.replacer;
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

