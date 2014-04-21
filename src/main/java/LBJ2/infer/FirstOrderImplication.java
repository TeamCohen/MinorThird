package LBJ2.infer;


/**
  * Represents an implication between two first order constraints.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderImplication extends FirstOrderBinaryConstraint
{
  /**
    * Initializing constructor.
    *
    * @param l  The constraint on the left of the operator.
    * @param r  The constraint on the right of the operator.
   **/
  public FirstOrderImplication(FirstOrderConstraint l, FirstOrderConstraint r)
  {
    super(l, r);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() { return !left.evaluate() || right.evaluate(); }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    return new PropositionalImplication(left.propositionalize(),
                                        right.propositionalize());
  }


  /**
    * The hash code of a <code>FirstOrderImplication</code> is the sum of the
    * hash codes of its children plus two.
    *
    * @return The hash code for this <code>FirstOrderImplication</code>.
   **/
  public int hashCode() { return left.hashCode() + right.hashCode() + 2; }


  /**
    * Two <code>FirstOrderImplication</code>s are equivalent when they are
    * topologically equivalent.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>FirstOrderImplication</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderImplication)) return false;
    FirstOrderImplication i = (FirstOrderImplication) o;
    return left.equals(i.left) && right.equals(i.right);
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

