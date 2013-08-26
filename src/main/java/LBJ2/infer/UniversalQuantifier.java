package LBJ2.infer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;


/**
  * A universal quantifier states that the constraint must hold for all
  * objects from the collection.
  *
  * @author Nick Rizzolo
 **/
public class UniversalQuantifier extends Quantifier
{
  /**
    * Initializing constructor.
    *
    * @param q    The name of the quantification variable.
    * @param col  The collection of objects to iterate over.
    * @param con  The constraint being quantified.
   **/
  public UniversalQuantifier(String q, Collection col,
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
  public UniversalQuantifier(String q, Collection col,
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

      if (!constraint.evaluate()) {
        enclosingQuantificationSettings.removeElementAt(index);
        return false;
      }
    }

    enclosingQuantificationSettings.removeElementAt(index);
    return true;
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
          new PropositionalConjunction(result, constraint.propositionalize());
    }

    enclosingQuantificationSettings.removeElementAt(index);
    if (result == null) result = new PropositionalConstant(true);
    return result;
  }


  /**
    * The hash code of a <code>UniversalQuantifier</code> is the sum of the
    * hash codes of its children.
    *
    * @return The hash code for this <code>UniversalQuantifier</code>.
   **/
  public int hashCode() { return super.hashCode(); }


  /**
    * Two <code>UniversalQuantifier</code>s are equivalent when their children
    * are equivalent.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>UniversalQuantifier</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof UniversalQuantifier)) return false;
    UniversalQuantifier q = (UniversalQuantifier) o;
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

