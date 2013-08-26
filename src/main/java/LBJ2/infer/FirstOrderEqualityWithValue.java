package LBJ2.infer;

import java.util.Vector;
import LBJ2.classify.Score;


/**
  * Represents the comparison of a classifier application with a value.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderEqualityWithValue extends FirstOrderEquality
{
  /** The variable on the left of the equality. */
  protected FirstOrderVariable left;
  /** The value on the right of the equality. */
  protected String right;


  /**
    * Initializing constructor.
    *
    * @param e  Indicates whether this is an equality or an inequality.
    * @param l  The classifier application.
    * @param r  The value.
   **/
  public FirstOrderEqualityWithValue(boolean e, FirstOrderVariable l,
                                     String r) {
    this(e, l, r, null);
  }

  /**
    * This constructor specifies a variable setter for when this equality is
    * quantified.
    *
    * @param e    Indicates whether this is an equality or an inequality.
    * @param l    The classifier application.
    * @param r    The value.
    * @param ear  An argument replacer.
   **/
  public FirstOrderEqualityWithValue(boolean e, FirstOrderVariable l,
                                     String r, EqualityArgumentReplacer ear) {
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
  public void consolidateVariables(java.util.AbstractMap m) {
    variableMap = m;
    if (m.containsKey(left)) left = (FirstOrderVariable) m.get(left);
    else m.put(left, left);
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
          "LBJ ERROR: Attempting to set quantification variable in "
          + "FirstOrderEqualityWithValue with no variable setter "
          + "implementation provided.");
      System.exit(1);
    }

    replacer.setQuantificationVariables(o);
    if (!replacer.leftConstant) {
      left =
        new FirstOrderVariable(left.getClassifier(),
                               replacer.getLeftObject());
      if (variableMap != null && variableMap.containsKey(left))
        left = (FirstOrderVariable) variableMap.get(left);
    }

    if (!replacer.rightConstant) right = replacer.getRightValue();
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() {
    return equality == left.getValue().equals(right);
  }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    Score[] leftScores = left.getScores().toArray();
    boolean found = false;
    for (int i = 0; i < leftScores.length && !found; ++i)
      found = leftScores[i].value.equals(right);

    PropositionalConstraint result = null;
    if (!found) result = new PropositionalConstant(false);
    else
      result = new PropositionalVariable(left.getClassifier(),
                                         left.getExample(), right);

    if (!equality) result = new PropositionalNegation(result);
    return result;
  }


  /**
    * The hash code of a <code>FirstOrderEqualityWithValue</code> is the sum
    * of the hash codes of its children plus 1.
    *
    * @return The hash code for this <code>FirstOrderEqualityWithValue</code>.
   **/
  public int hashCode() {
    if (replacer != null) return replacer.hashCode();
    return left.hashCode() + right.hashCode() + 1;
  }


  /**
    * Two <code>FirstOrderEqualityWithValue</code>s are equivalent when their
    * children are equivalent.
    *
    * @return <code>true</code> iff the argument is a
    *         <code>FirstOrderEqualityWithValue</code> involving the same
    *         children.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderEqualityWithValue)) return false;
    FirstOrderEqualityWithValue n = (FirstOrderEqualityWithValue) o;
    return replacer == n.replacer
           && (replacer != null
               || replacer == null
                  && left.equals(n.left) && right.equals(n.right));
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

