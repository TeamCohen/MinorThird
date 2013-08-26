package LBJ2.infer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;


/**
  * An existential quantifier states that the constraint must hold for at
  * least one object from the collection.
  *
  * @author Nick Rizzolo
 **/
public class ExistentialQuantifier extends Quantifier
{
  /**
    * Initializing constructor.
    *
    * @param q    The name of the quantification variable.
    * @param col  The collection of objects to iterate over.
    * @param con  The constraint being quantified.
   **/
  public ExistentialQuantifier(String q, Collection col,
                               FirstOrderConstraint con) {
    super(q, col, con);
  }

  /**
    * This constructor specifies a variable setter for when this quantifier is
    * itself quantified.
    *
    * @param q    The name of the quantification variable.
    * @param col  The collection of objects to iterate over.
    * @param con  The constraint being quantified.
    * @param qar  The variable setter.
   **/
  public ExistentialQuantifier(String q, Collection col,
                               FirstOrderConstraint con,
                               QuantifierArgumentReplacer qar) {
    super(q, col, con, qar);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() {
    int index = initialize();

    for (Iterator I = collection.iterator(); I.hasNext(); ) {
      enclosingQuantificationSettings.set(index, I.next());
      constraint.setQuantificationVariables(enclosingQuantificationSettings);

      if (constraint.evaluate()) {
        enclosingQuantificationSettings.removeElementAt(index);
        return true;
      }
    }

    enclosingQuantificationSettings.removeElementAt(index);
    return false;
  }


  /**
    * This method sets the given quantification variables to the given object
    * references and evaluates the expressions involving those variables in
    * this constraint's children.
    *
    * @param o  The new object references for the enclosing quantification
    *           variables, in order of nesting.
   **/
  public void setQuantificationVariables(Vector o) {
    enclosingQuantificationSettings = o;

    if (replacer != null) {
      replacer.setQuantificationVariables(o);
      collection = replacer.getCollection();
    }
  }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    PropositionalConstraint result = null;

    int index = initialize();
    for (Iterator I = collection.iterator(); I.hasNext(); ) {
      enclosingQuantificationSettings.set(index, I.next());
      constraint.setQuantificationVariables(enclosingQuantificationSettings);

      if (result == null) result = constraint.propositionalize();
      else
        result =
          new PropositionalDisjunction(result, constraint.propositionalize());
    }

    enclosingQuantificationSettings.removeElementAt(index);
    if (result == null) result = new PropositionalConstant(false);
    return result;
  }


  /**
    * The hash code of a <code>ExistentialQuantifier</code> is the sum of the
    * hash codes of its children plus one.
    *
    * @return The hash code for this <code>ExistentialQuantifier</code>.
   **/
  public int hashCode() { return super.hashCode() + 1; }


  /**
    * Two <code>ExistentialQuantifier</code>s are equivalent when their
    * children are equivalent.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>ExistentialQuantifier</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof ExistentialQuantifier)) return false;
    ExistentialQuantifier q = (ExistentialQuantifier) o;
    return super.equals(q);
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

