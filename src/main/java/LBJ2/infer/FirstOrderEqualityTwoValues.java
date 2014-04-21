package LBJ2.infer;

import java.util.Vector;


/**
  * Represents the comparison of two <code>String</code> values.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderEqualityTwoValues extends FirstOrderEquality
{
  /** The value on the left of the equality. */
  protected String left;
  /** The value on the right of the equality. */
  protected String right;


  /**
    * Initializing constructor.
    *
    * @param e  Indicates whether this is an equality or an inequality.
    * @param l  The left value.
    * @param r  The right value.
   **/
  public FirstOrderEqualityTwoValues(boolean e, String l, String r) {
    this(e, l, r, null);
  }

  /**
    * This constructor specifies a variable setter for when this equality is
    * quantified.
    *
    * @param e    Indicates whether this is an equality or an inequality.
    * @param l    The left value.
    * @param r    The right value.
    * @param ear  An argument replacer.
   **/
  public FirstOrderEqualityTwoValues(boolean e, String l, String r,
                                     EqualityArgumentReplacer ear) {
    super(e, ear);
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
  public void consolidateVariables(java.util.AbstractMap m) { }


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
          "LBJ ERROR: Attempting to set quantification variable in "
          + "FirstOrderEqualityTwoValues with no variable setter "
          + "implementation provided.");
      System.exit(1);
    }

    replacer.setQuantificationVariables(o);
    if (!replacer.leftConstant) left = replacer.getLeftValue();
    if (!replacer.rightConstant) right = replacer.getRightValue();
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() { return equality == left.equals(right); }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    return new PropositionalConstant(evaluate());
  }


  /**
    * The hash code of a <code>FirstOrderEqualityTwoValues</code> is the sum
    * of the hash codes of its children.
    *
    * @return The hash code for this <code>FirstOrderEqualityTwoValues</code>.
   **/
  public int hashCode() {
    if (replacer != null) return replacer.hashCode();
    return left.hashCode() + right.hashCode();
  }


  /**
    * Two <code>FirstOrderEqualityTwoValues</code>s are equivalent when their
    * children are equivalent in either order.
    *
    * @return <code>true</code> iff the argument is a
    *         <code>FirstOrderEqualityTwoValues</code> involving the same
    *         children.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderEqualityTwoValues)) return false;
    FirstOrderEqualityTwoValues n = (FirstOrderEqualityTwoValues) o;
    return replacer == n.replacer
           && (replacer != null
               || replacer == null
                  && (left.equals(n.left) && right.equals(n.right)
                      || left.equals(n.right) && right.equals(n.left)));
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

