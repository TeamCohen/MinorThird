package LBJ2.infer;

import java.util.Iterator;


/**
  * Represents the conjunction of first order constraints.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderConjunction extends FirstOrderNAryConstraint
{
  /**
    * If either of the arguments is itself a
    * <code>FirstOrderConjunction</code>, its contents are flattened into
    * this <code>FirstOrderConjunction</code>.
    *
    * @param c1 One constraint to disjunct.
    * @param c2 Another constraint to disjunct.
   **/
  public FirstOrderConjunction(FirstOrderConstraint c1,
                               FirstOrderConstraint c2) {
    add(c1);
    add(c2);
  }


  /**
    * If the given constraint has the same type as this constraint, its terms
    * are merged into this constraint; otherwise, it is added as a new term.
    *
    * @param c  The constraint to add.
   **/
  public void add(FirstOrderConstraint c) {
    if (c instanceof FirstOrderConjunction) {
      Iterator I = ((FirstOrderConjunction) c).children.iterator();
      while (I.hasNext()) add((FirstOrderConstraint) I.next());
    }
    else children.add(c);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() {
    for (Iterator I = children.iterator(); I.hasNext(); )
      if (!((FirstOrderConstraint) I.next()).evaluate()) return false;
    return true;
  }


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  public PropositionalConstraint propositionalize() {
    if (children.size() == 0) return new PropositionalConstant(true);

    FirstOrderConstraint[] c =
      (FirstOrderConstraint[]) children.toArray(new FirstOrderConstraint[0]);
    if (c.length == 1) return c[0].propositionalize();

    PropositionalConjunction result =
      new PropositionalConjunction(c[0].propositionalize(),
                                   c[1].propositionalize());
    for (int i = 2; i < c.length; ++i)
      result = new PropositionalConjunction(result, c[i].propositionalize());

    return result;
  }


  /**
    * The hash code of a <code>FirstOrderConjunction</code> is the sum of
    * the hash codes of its children plus one.
    *
    * @return The hash code for this <code>FirstOrderConjunction</code>.
   **/
  public int hashCode() {
    int result = 1;
    for (Iterator I = children.iterator(); I.hasNext(); )
      result += I.next().hashCode();
    return result;
  }


  /**
    * Two <code>FirstOrderConjunction</code>s are equivalent when they are
    * topologically equivalent, respecting the associativity and commutivity
    * of disjunction.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>FirstOrderConjunction</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderConjunction)) return false;
    FirstOrderConjunction d = (FirstOrderConjunction) o;
    return children.equals(d.children);
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

