package LBJ2.infer;


/**
  * Represents a double implication between two first order constraints.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderDoubleImplication extends FirstOrderBinaryConstraint
{
  /**
    * Initializing constructor.
    *
    * @param l  The constraint on the left of the operator.
    * @param r  The constraint on the right of the operator.
   **/
  public FirstOrderDoubleImplication(FirstOrderConstraint l,
                                     FirstOrderConstraint r) {
    super(l, r);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() { return left.evaluate() == right.evaluate(); }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    return new PropositionalDoubleImplication(left.propositionalize(),
                                              right.propositionalize());
  }


  /**
    * The hash code of a <code>FirstOrderDoubleImplication</code> is the sum
    * of the hash codes of its children plus three.
    *
    * @return The hash code for this <code>FirstOrderDoubleImplication</code>.
   **/
  public int hashCode() { return left.hashCode() + right.hashCode() + 3; }


  /**
    * Two <code>FirstOrderDoubleImplication</code>s are equivalent when
    * they are topologically equivalent, respecting the commutativity of
    * double implication.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>FirstOrderDoubleImplication</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderDoubleImplication)) return false;
    FirstOrderDoubleImplication i = (FirstOrderDoubleImplication) o;
    return left.equals(i.left) && right.equals(i.right)
           || left.equals(i.right) && right.equals(i.left);
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

