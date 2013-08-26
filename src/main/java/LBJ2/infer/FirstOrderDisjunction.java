package LBJ2.infer;

import java.util.Iterator;


/**
  * Represents the disjunction of first order constraints.
  *
  * @author Nick Rizzolo
 **/
public class FirstOrderDisjunction extends FirstOrderNAryConstraint
{
  /**
    * If either of the arguments is itself a
    * <code>FirstOrderDisjunction</code>, its contents are flattened into
    * this <code>FirstOrderDisjunction</code>.
    *
    * @param c1 One constraint to disjunct.
    * @param c2 Another constraint to disjunct.
   **/
  public FirstOrderDisjunction(FirstOrderConstraint c1,
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
    if (c instanceof FirstOrderDisjunction) {
      Iterator I = ((FirstOrderDisjunction) c).children.iterator();
      while (I.hasNext()) add((FirstOrderConstraint) I.next());
    }
    else children.add(c);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() {
    for (Iterator I = children.iterator(); I.hasNext(); )
      if (((FirstOrderConstraint) I.next()).evaluate()) return true;
    return false;
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

    PropositionalDisjunction result =
      new PropositionalDisjunction(c[0].propositionalize(),
                                   c[1].propositionalize());
    for (int i = 2; i < c.length; ++i)
      result = new PropositionalDisjunction(result, c[i].propositionalize());

    return result;
  }


  /**
    * The hash code of a <code>FirstOrderDisjunction</code> is the sum of
    * the hash codes of its children.
    *
    * @return The hash code for this <code>FirstOrderDisjunction</code>.
   **/
  public int hashCode() {
    int result = 0;
    for (Iterator I = children.iterator(); I.hasNext(); )
      result += I.next().hashCode();
    return result;
  }


  /**
    * Two <code>FirstOrderDisjunction</code>s are equivalent when they are
    * topologically equivalent, respecting the associativity and commutivity
    * of disjunction.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>FirstOrderDisjunction</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FirstOrderDisjunction)) return false;
    FirstOrderDisjunction d = (FirstOrderDisjunction) o;
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

