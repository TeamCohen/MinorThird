package LBJ2.infer;

import java.util.Vector;
import LBJ2.classify.Score;


/**
  * Represents the comparison of two classifier applications.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderEqualityWithVariable extends FirstOrderEquality
{
  /** The variable on the left of the equality. */
  protected FirstOrderVariable left;
  /** The classifier application on the right of the equality. */
  protected FirstOrderVariable right;


  /**
    * Initializing constructor.
    *
    * @param e  Indicates whether this is an equality or an inequality.
    * @param l  The left classifier application.
    * @param r  The right classifier application.
   **/
  public FirstOrderEqualityWithVariable(boolean e, FirstOrderVariable l,
                                        FirstOrderVariable r) {
    this(e, l, r, null);
  }

  /**
    * This constructor specifies a variable setter for when this equality is
    * quantified.
    *
    * @param e    Indicates whether this is an equality or an inequality.
    * @param l    The left classifier application.
    * @param r    The right classifier application.
    * @param ear  An argument replacer.
   **/
  public FirstOrderEqualityWithVariable(boolean e, FirstOrderVariable l,
                                        FirstOrderVariable r,
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
  public void consolidateVariables(java.util.AbstractMap m) {
    variableMap = m;
    if (m.containsKey(left)) left = (FirstOrderVariable) m.get(left);
    else m.put(left, left);
    if (m.containsKey(right)) right = (FirstOrderVariable) m.get(right);
    else m.put(right, right);
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
          + "FirstOrderEqualityWithVariable with no variable setter "
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

    if (!replacer.rightConstant) {
      right = new FirstOrderVariable(right.getClassifier(),
                                     replacer.getRightObject());
      if (variableMap != null && variableMap.containsKey(right))
        right = (FirstOrderVariable) variableMap.get(right);
    }
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() {
    return equality == left.getValue().equals(right.getValue());
  }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    Score[] leftScores = left.getScores().toArray();
    Score[] rightScores = right.getScores().toArray();
    if (leftScores.length == 0 || rightScores.length == 0)
      return new PropositionalConstant(false);
    if (leftScores.length == 1 && rightScores.length == 1)
      return
        new PropositionalConstant(leftScores[0].value
                                  .equals(rightScores[0].value));

    PropositionalVariable[] leftVariables =
      new PropositionalVariable[leftScores.length];
    PropositionalVariable[] rightVariables =
      new PropositionalVariable[rightScores.length];

    int size = 0;
    for (int i = 0; i < leftScores.length; ++i) {
      boolean found = false;
      for (int j = 0; j < rightScores.length && !found; ++j) {
        if (!leftScores[i].value.equals(rightScores[j].value)) continue;
        found = true;
        leftVariables[size] =
          new PropositionalVariable(left.getClassifier(), left.getExample(),
                                    leftScores[i].value);
        rightVariables[size] =
          new PropositionalVariable(right.getClassifier(), right.getExample(),
                                    rightScores[j].value);
        ++size;
      }
    }

    if (size == 0) return new PropositionalConstant(false);

    if (equality && size == leftScores.length && size == rightScores.length)
      --size;

    PropositionalConstraint rightVariable = rightVariables[0];
    if (!equality) rightVariable = new PropositionalNegation(rightVariable);
    PropositionalConstraint result =
      new PropositionalDisjunction(
          new PropositionalNegation(leftVariables[0]),
          rightVariable);
    if (equality)
      result =
        new PropositionalConjunction(
            result,
            new PropositionalDisjunction(
              new PropositionalNegation(rightVariable),
              leftVariables[0]));

    for (int i = 1; i < size; ++i) {
      rightVariable = rightVariables[i];
      if (!equality) rightVariable = new PropositionalNegation(rightVariable);
      result =
        new PropositionalConjunction(
            result,
            new PropositionalDisjunction(
              new PropositionalNegation(leftVariables[i]),
              rightVariable));
      if (equality)
        result =
          new PropositionalConjunction(
              result,
              new PropositionalDisjunction(
                new PropositionalNegation(rightVariable),
                leftVariables[i]));
    }

    return result;
  }


  /**
    * The hash code of a <code>FirstOrderEqualityWithVariable</code> is the
    * sum of the hash codes of its children plus 2.
    *
    * @return The hash code for this
    *         <code>FirstOrderEqualityWithVariable</code>.
   **/
  public int hashCode() {
    if (replacer != null) return replacer.hashCode();
    return left.hashCode() + right.hashCode() + 2;
  }


  /**
    * Two <code>FirstOrderEqualityWithVariable</code>s are equivalent when
    * their children are equivalent in either order.
    *
    * @return <code>true</code> iff the argument is a
    *         <code>FirstOrderEqualityWithVariable</code> involving the same
    *         variables.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderEqualityWithVariable)) return false;
    FirstOrderEqualityWithVariable n = (FirstOrderEqualityWithVariable) o;
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

