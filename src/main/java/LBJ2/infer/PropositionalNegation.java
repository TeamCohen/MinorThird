package LBJ2.infer;


/**
  * Represents the negation operator applied to a propositional constraint.
  *
  * @author Nick Rizzolo
 **/
public class PropositionalNegation extends PropositionalConstraint
{
  /** The constraint that the negation is applied to. */
  protected PropositionalConstraint constraint;


  /**
    * Initializing constructor.
    *
    * @param c  The constraint to negate.
   **/
  public PropositionalNegation(PropositionalConstraint c) { constraint = c; }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(java.util.AbstractMap m) {
    if (constraint instanceof PropositionalVariable) {
      if (m.containsKey(constraint))
        constraint = (PropositionalVariable) m.get(constraint);
      else m.put(constraint, constraint);
    }
    else constraint.consolidateVariables(m);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() { return !constraint.evaluate(); }


  /**
    * Produces a new, logically simplified version of this constraint,
    * preserving variable consolidation.
    *
    * @see    Constraint#consolidateVariables(java.util.AbstractMap)
    * @return A logically simplified version of this constraint.
   **/
  public PropositionalConstraint simplify() {
    if (constraint instanceof PropositionalVariable) return this;
    return constraint.negate().simplify();
  }


  /**
    * Produces a new propositional constraint equivalent to this constraint
    * and that contains no negated constraints other than variables.
    *
    * @return A constraint representing the negation of this constraint.
   **/
  public PropositionalConstraint negate() { return constraint; }


  /**
    * Produces a new, logically simplified version of this constraint in
    * conjunctive normal form (CNF).
    *
    * @return The conjunctive normal form of this constraint.
   **/
  public PropositionalConstraint CNF() {
    PropositionalConstraint simplified = simplify();
    if (simplified instanceof PropositionalNegation) return simplified;
    return simplified.CNF();
  }


  /**
    * Produces a new, logically simplified version of this constraint in
    * disjunctive normal form (DNF).
    *
    * @return The disjunctive normal form of this constraint.
   **/
  public PropositionalConstraint DNF() {
    PropositionalConstraint simplified = simplify();
    if (simplified instanceof PropositionalNegation) return simplified;
    return simplified.DNF();
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() {
    return new PropositionalConstraint[]{ constraint };
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
    return constraint.equals(children[0]) || equals(children[1]);
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
    return c.size() > 1 && c.contains(this);
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
    * The hash code of a <code>PropositionalNegation</code> is the hash code
    * of its child constraint plus 1.
    *
    * @return The hash code for this <code>PropositionalNegation</code>.
   **/
  public int hashCode() { return constraint.hashCode() + 1; }


  /**
    * Two <code>PropositionalNegation</code>s are equivalent when their
    * constraints are equivalent.
    *
    * @return <code>true</code> iff the argument is a
    *         <code>PropositionalNegation</code> of the same constraint.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof PropositionalNegation)) return false;
    PropositionalNegation n = (PropositionalNegation) o;
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


  /**
    * Creates a string respresentation of this constraint using the string
    * representations of the objects involved.
    *
    * @param buffer The output of this method will be appended to this buffer.
   **/
  public void write(StringBuffer buffer) {
    buffer.append("!");
    constraint.write(buffer);
  }
}

