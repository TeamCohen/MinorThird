package LBJ2.infer;

import LBJ2.learn.Learner;


/**
  * Every propositional variable is Boolean and represents one possible
  * prediction from a classifier application.  If the variable is
  * <code>true</code>, then the classifier application did result in the
  * specified prediction value.
  *
  * @author Nick Rizzolo
 **/
public class PropositionalVariable extends PropositionalConstraint
{
  /** The classifier being applied. */
  protected Learner classifier;
  /** The classifier is applied to this example object. */
  protected Object example;
  /**
    * The prediction that the classifier must produce for this variable to be
    * <code>true</code>.
   **/
  protected String prediction;
  /** The value imposed on this variable. */
  public boolean value;


  /**
    * Initializing constructor; the <code>value</code> member variable is set
    * to <code>false</code>.
    *
    * @param c  The classifier being applied.
    * @param e  The classifier is applied to this example object.
    * @param p  The prediction associated with this variable.
   **/
  public PropositionalVariable(Learner c, Object e, String p) {
    classifier = c;
    example = e;
    prediction = p;
    value = false;
  }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(java.util.AbstractMap m) { }


  /** Retrieves the classifier. */
  public Learner getClassifier() { return classifier; }


  /** Retrieves the example object. */
  public Object getExample() { return example; }


  /** Retrieves the prediction. */
  public String getPrediction() { return prediction; }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() { return value; }


  /**
    * Produces a new, logically simplified version of this constraint,
    * preserving variable consolidation.
    *
    * @see    Constraint#consolidateVariables(java.util.AbstractMap)
    * @return A logically simplified version of this constraint.
   **/
  public PropositionalConstraint simplify() { return this; }


  /**
    * Produces a new propositional constraint equivalent to this constraint
    * and that contains no negated constraints other than variables.
    *
    * @return A constraint representing the negation of this constraint.
   **/
  public PropositionalConstraint negate() {
    return new PropositionalNegation(this);
  }


  /**
    * Produces a new, logically simplified version of this constraint in
    * conjunctive normal form (CNF).
    *
    * @return The conjunctive normal form of this constraint.
   **/
  public PropositionalConstraint CNF() { return this; }


  /**
    * Produces a new, logically simplified version of this constraint in
    * disjunctive normal form (DNF).
    *
    * @return The disjunctive normal form of this constraint.
   **/
  public PropositionalConstraint DNF() { return this; }


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
    PropositionalConstraint[] children =
      (PropositionalConstraint[]) c.getChildren();
    return new PropositionalNegation(this).equals(children[0])
           || equals(children[1]);
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
    * The hash code of a <code>PropositionalVariable</code> is the hash code
    * of the string representation of the classifier plus the system's hash
    * code for the example object plus the hash code of the prediction.
    *
    * @return The hash code of this <code>PropositionalVariable</code>.
   **/
  public int hashCode() {
    return classifier.toString().hashCode() + System.identityHashCode(example)
           + prediction.hashCode();
  }


  /**
    * Two <code>PropositionalVariable</code>s are equivalent when the string
    * representations of their classifiers are equivalent, they store the
    * same example object, and their values are equivalent.
    *
    * @param o  The object to test equivalence with.
    * @return <code>true</code> iff this object is equivalent to the argument
    *         object.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof PropositionalVariable)) return false;
    PropositionalVariable v = (PropositionalVariable) o;
    return classifier.equals(v.classifier) && example == v.example
           && prediction.equals(v.prediction);
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
    buffer.append(classifier);
    buffer.append("(");
    buffer.append(example);
    buffer.append(") :: ");
    buffer.append(prediction);
  }
}

