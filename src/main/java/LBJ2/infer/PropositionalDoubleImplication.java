package LBJ2.infer;


/**
  * Represents a double implication between two propositional constraints.
  *
  * @author Nick Rizzolo
 **/
public class PropositionalDoubleImplication
       extends PropositionalBinaryConstraint
{
  /**
    * Initializing constructor.
    *
    * @param l  The constraint on the left of the operator.
    * @param r  The constraint on the right of the operator.
   **/
  public PropositionalDoubleImplication(PropositionalConstraint l,
                                        PropositionalConstraint r) {
    super(l, r);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() { return left.evaluate() == right.evaluate(); }


  /**
    * Produces a new, logically simplified version of this constraint,
    * preserving variable consolidation.
    *
    * @see    Constraint#consolidateVariables(java.util.AbstractMap)
    * @return A logically simplified version of this constraint.
   **/
  public PropositionalConstraint simplify() {
    return
      new PropositionalConjunction(
          new PropositionalDisjunction(left.negate(), right),
          new PropositionalDisjunction(right.negate(), left)).simplify();
  }


  /**
    * Produces a new propositional constraint equivalent to this constraint
    * and that contains no negated constraints other than variables.
    *
    * @return A constraint representing the negation of this constraint.
   **/
  public PropositionalConstraint negate() {
    return
      new PropositionalConjunction(
          new PropositionalDisjunction(left.negate(), right.negate()),
          new PropositionalDisjunction(left, right));
  }



  /**
    * Produces a new, logically simplified version of this constraint in
    * conjunctive normal form (CNF).
    *
    * @return The conjunctive normal form of this constraint.
   **/
  public PropositionalConstraint CNF() {
    return
      new PropositionalConjunction(
          new PropositionalDisjunction(
            new PropositionalNegation(left),
            right),
          new PropositionalDisjunction(
            new PropositionalNegation(right),
            left))
      .CNF();
  }


  /**
    * Produces a new, logically simplified version of this constraint in
    * disjunctive normal form (DNF).
    *
    * @return The disjunctive normal form of this constraint.
   **/
  public PropositionalConstraint DNF() {
    return
      new PropositionalDisjunction(
          new PropositionalConjunction(left, right),
          new PropositionalConjunction(
            new PropositionalNegation(left),
            new PropositionalNegation(right)))
      .DNF();
  }


  /**
    * Compares topology to determine if this constraint is more general than
    * the given constraint; <i>note: this method is not required to be correct
    * when it answers <code>false</code></i>.
    *
    * @param c  The given constraint.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more general than the given constraint.
   **/
  public boolean moreGeneralThan(PropositionalConstraint c) {
    return c.moreSpecificThan(this);
  }


  /**
    * Compares topology to determine if this constraint is more specific than
    * the given implication; <i>note: this method is not required to be
    * correct when it answers <code>false</code></i>.
    *
    * @param c  The given implication.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more specific than the given implication.
   **/
  public boolean moreSpecificThan(PropositionalImplication c) {
    PropositionalConstraint[] children =
      (PropositionalConstraint[]) c.getChildren();
    return left.equals(children[0]) && right.equals(children[1])
           || left.equals(children[1]) && right.equals(children[0]);
  }


  /**
    * Compares topology to determine if this constraint is more specific than
    * the given double implication; <i>note: this method is not required to be
    * correct when it answers <code>false</code></i>.
    *
    * @param c  The given double implication.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more specific than the given double implication.
   **/
  public boolean moreSpecificThan(PropositionalDoubleImplication c) {
    return false;
  }


  /**
    * Compares topology to determine if this constraint is more specific than
    * the given conjunction; <i>note: this method is not required to be
    * correct when it answers <code>false</code></i>.
    *
    * @param c  The given conjunction.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more specific than the given conjunction.
   **/
  public boolean moreSpecificThan(PropositionalConjunction c) {
    return false;
  }


  /**
    * Compares topology to determine if this constraint is more specific than
    * the given disjunction; <i>note: this method is not required to be
    * correct when it answers <code>false</code></i>.
    *
    * @param c  The given disjunction.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more specific than the given disjunction.
   **/
  public boolean moreSpecificThan(PropositionalDisjunction c) {
    PropositionalConstraint[] children =
      (PropositionalConstraint[]) c.getChildren();
    return children.length == 2
           && (new PropositionalNegation(left).equals(children[0])
                  && right.equals(children[1])
               || new PropositionalNegation(left).equals(children[1])
                  && right.equals(children[0])
               || left.equals(children[0])
                  && new PropositionalNegation(right).equals(children[1])
               || left.equals(children[1])
                  && new PropositionalNegation(right).equals(children[0]));
  }


  /**
    * Compares topology to determine if this constraint is more specific than
    * the given at-least; <i>note: this method is not required to be correct
    * when it answers <code>false</code></i>.
    *
    * @param c  The given at-least.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more specific than the given disjunction.
   **/
  public boolean moreSpecificThan(PropositionalAtLeast c) {
    return false;
  }


  /**
    * Compares topology to determine if this constraint is more specific than
    * the given negation; <i>note: this method is not required to be correct
    * when it answers <code>false</code></i>.
    *
    * @param c  The given negation.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more specific than the given negation.
   **/
  public boolean moreSpecificThan(PropositionalNegation c) {
    return false;
  }


  /**
    * Compares topology to determine if this constraint is more specific than
    * the given variable; <i>note: this method is not required to be correct
    * when it answers <code>false</code></i>.
    *
    * @param c  The given variable.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more specific than the given variable.
   **/
  public boolean moreSpecificThan(PropositionalVariable c) {
    return false;
  }


  /**
    * Compares topology to determine if this constraint is more specific than
    * the given constant; <i>note: this method is not required to be correct
    * when it answers <code>false</code></i>.
    *
    * @param c  The given constant.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more specific than the given constant.
   **/
  public boolean moreSpecificThan(PropositionalConstant c) {
    return c.evaluate();
  }


  /**
    * The hash code of a <code>PropositionalDoubleImplication</code> is the
    * sum of the hash codes of its children plus three.
    *
    * @return The hash code for this
    *         <code>PropositionalDoubleImplication</code>.
   **/
  public int hashCode() { return left.hashCode() + right.hashCode() + 3; }


  /**
    * Two <code>PropositionalDoubleImplication</code>s are equivalent when
    * they are topologically equivalent, respecting the commutativity of
    * double implication.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>PropositionalDoubleImplication</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof PropositionalDoubleImplication)) return false;
    PropositionalDoubleImplication i = (PropositionalDoubleImplication) o;
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


  /**
    * Creates a string respresentation of this constraint using the string
    * representations of the objects involved.
    *
    * @param buffer The output of this method will be appended to this buffer.
   **/
  public void write(StringBuffer buffer) {
    buffer.append("(");
    left.write(buffer);
    buffer.append(" <=> ");
    right.write(buffer);
    buffer.append(")");
  }
}

