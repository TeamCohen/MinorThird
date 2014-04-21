package LBJ2.infer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;


/**
  * An "at most" quantifier states that the constraint must hold for no more
  * than <i>m</i> of the objects in the collection.
  *
  * @author Nick Rizzolo
 **/
public class AtMostQuantifier extends Quantifier
{
  /** The maximum number of objects for which the constraint must hold. */
  protected int m;


  /**
    * Initializing constructor.
    *
    * @param q    The name of the quantification variable.
    * @param col  The collection of objects to iterate over.
    * @param con  The constraint being quantified.
    * @param m    The number of objects for which the constraint must hold.
   **/
  public AtMostQuantifier(String q, Collection col, FirstOrderConstraint con,
                          int m) {
    this(q, col, con, m, null);
  }

  /**
    * This constructor specifies a variable setter for when this quantifier is
    * itself quantified.
    *
    * @param q    The name of the quantification variable.
    * @param col  The collection of objects to iterate over.
    * @param con  The constraint being quantified.
    * @param m    The number of objects for which the constraint must hold.
    * @param qar  The variable setter.
   **/
  public AtMostQuantifier(String q, Collection col, FirstOrderConstraint con,
                          int m, QuantifierArgumentReplacer qar) {
    super(q, col, con, qar);
    this.m = Math.max(m, 0);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() {
    int satisfied = 0;

    int index = initialize();
    for (Iterator I = collection.iterator(); I.hasNext() && satisfied <= m; )
    {
      enclosingQuantificationSettings.set(index, I.next());
      constraint.setQuantificationVariables(enclosingQuantificationSettings);
      if (constraint.evaluate()) ++satisfied;
    }

    enclosingQuantificationSettings.removeElementAt(index);
    return satisfied <= m;
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
      if (!replacer.collectionConstant) collection = replacer.getCollection();
      if (!replacer.boundConstant) m = replacer.getBound();
    }
  }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    return
      new AtLeastQuantifier(quantificationVariable, collection,
                            new FirstOrderNegation(constraint),
                            collection.size() - m)
      .propositionalize();
  }


  /**
    * The hash code of a <code>AtMostQuantifier</code> is the sum of the hash
    * codes of its children.
    *
    * @return The hash code for this <code>AtMostQuantifier</code>.
   **/
  public int hashCode() { return super.hashCode() + m; }


  /**
    * Two <code>AtMostQuantifier</code>s are equivalent when their children
    * are equivalent.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>AtMostQuantifier</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof AtMostQuantifier)) return false;
    AtMostQuantifier q = (AtMostQuantifier) o;
    return super.equals(q) && m == q.m;
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

