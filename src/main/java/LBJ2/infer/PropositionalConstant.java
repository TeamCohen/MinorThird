package LBJ2.infer;


/**
  * A propositional constant is either <code>true</code> or
  * <code>false</code>.
  *
  * @author Nick Rizzolo
 **/
public class PropositionalConstant extends PropositionalConstraint
{
  /** <code>true</code> */
  public static final PropositionalConstant True =
    new PropositionalConstant(true);
  /** <code>false</code> */
  public static final PropositionalConstant False =
    new PropositionalConstant(false);


  /** The constant value. */
  protected boolean constant;


  /**
    * Initializing constructor.
    *
    * @param v  The value of this constant.
   **/
  public PropositionalConstant(boolean v) { constant = v; }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(java.util.AbstractMap m) { }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() { return constant; }


  /**
    * Produces a new, logically simplified version of this constraint,
    * preserving variable consolidation.
    *
    * @see    Constraint#consolidateVariables(java.util.AbstractMap)
    * @return A logically simplified version of this constraint.
   **/
  public PropositionalConstraint simplify() {
    return constant ? True : False;
  }


  /**
    * Produces a new propositional constraint equivalent to this constraint
    * and that contains no negated constraints other than variables.
    *
    * @return A constraint representing the negation of this constraint.
   **/
  public PropositionalConstraint negate() { return constant ? False : True; }


  /**
    * Produces a new, logically simplified version of this constraint in
    * conjunctive normal form (CNF).
    *
    * @return The conjunctive normal form of this constraint.
   **/
  public PropositionalConstraint CNF() { return simplify(); }


  /**
    * Produces a new, logically simplified version of this constraint in
    * disjunctive normal form (DNF).
    *
    * @return The disjunctive normal form of this constraint.
   **/
  public PropositionalConstraint DNF() { return simplify(); }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() { return new PropositionalConstraint[0]; }


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
    return !constant;
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
    return !constant;
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
    return !constant;
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
    return !constant;
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
    return !constant;
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
    return !constant;
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
    return !constant;
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
    return c.evaluate() && !constant;
  }


  /**
    * The hash code of a <code>PropositionalConstant</code> is the hash code
    * of the <code>Boolean</code> object formed from the constant.
    *
    * @return The hash code for this <code>PropositionalConstant</code>.
   **/
  public int hashCode() { return new Boolean(constant).hashCode(); }


  /**
    * Two <code>PropositionalConstant</code>s are equivalent when their
    * constants are equal.
    *
    * @return <code>true</code> iff the argument is a
    *         <code>PropositionalConstant</code> set to the same value as this
    *         constant.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof PropositionalConstant)) return false;
    PropositionalConstant c = (PropositionalConstant) o;
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


  /**
    * Creates a string respresentation of this constraint using the string
    * representations of the objects involved.
    *
    * @param buffer The output of this method will be appended to this buffer.
   **/
  public void write(StringBuffer buffer) {
    buffer.append(constant);
  }
}

