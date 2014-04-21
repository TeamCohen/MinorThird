package LBJ2.infer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;


/**
  * Represents the disjunction of two propositional constraints.
  *
  * @author Nick Rizzolo
 **/
public class PropositionalDisjunction extends PropositionalNAryConstraint
{
  /** Default constructor. */
  private PropositionalDisjunction() { }

  /**
    * If either of the arguments is itself a
    * <code>PropositionalDisjunction</code>, its contents are flattened into
    * this <code>PropositionalDisjunction</code>.
    *
    * @param c1 One constraint to disjunct.
    * @param c2 Another constraint to disjunct.
   **/
  public PropositionalDisjunction(PropositionalConstraint c1,
                                  PropositionalConstraint c2) {
    add(c1);
    add(c2);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() {
    for (Iterator I = children.iterator(); I.hasNext(); )
      if (((PropositionalConstraint) I.next()).evaluate()) return true;
    return false;
  }


  /**
    * Produces a new, logically simplified version of this constraint,
    * preserving variable consolidation.
    *
    * @see    Constraint#consolidateVariables(java.util.AbstractMap)
    * @return A logically simplified version of this constraint.
   **/
  public PropositionalConstraint simplify() {
    PropositionalDisjunction result = new PropositionalDisjunction();
    for (Iterator I = children.iterator(); I.hasNext(); )
      result.add(((PropositionalConstraint) I.next()).simplify());

    if (result.children.contains(PropositionalConstant.True))
      return PropositionalConstant.True;

    result.children.remove(PropositionalConstant.False);

    if (result.children.size() == 1)
      return (PropositionalConstraint) result.children.iterator().next();

    /*
    HashSet positive = new HashSet();
    HashSet negative = new HashSet();
    for (Iterator I = result.children.iterator(); I.hasNext(); ) {
      Object next = I.next();
      if (next instanceof PropositionalNegation)
        negative.add(((PropositionalConstraint) next).getChildren()[0]);
      else positive.add(next);
    }

    for (Iterator I = positive.iterator(); I.hasNext(); )
      if (negative.contains(I.next())) return PropositionalConstant.True;

    PropositionalConstraint[] terms =
      (PropositionalConstraint[]) getChildren();
    HashSet toRemove = new HashSet();
    for (int i = 0; i < terms.length - 1; ++i)
      for (int j = i + 1; j < terms.length; ++j) {
        if (terms[i].moreGeneralThan(terms[j])) toRemove.add(new Integer(j));
        if (terms[j].moreGeneralThan(terms[i])) toRemove.add(new Integer(i));
      }

    for (Iterator I = toRemove.iterator(); I.hasNext(); )
      result.children.remove(terms[((Integer) I.next()).intValue()]);
    */

    if (result.children.size() == 0) return PropositionalConstant.False;

    return result;
  }


  /**
    * Uses DeMorgan's law to compute the negation of this constraint by
    * distributing that negation to each child.
    *
    * @return A simplified constraint representing the negation of this
    *         constraint.
   **/
  public PropositionalConstraint negate() {
    if (children.size() == 1)
      return ((PropositionalConstraint) children.iterator().next()).negate();

    PropositionalConstraint[] array =
      (PropositionalConstraint[])
      children.toArray(new PropositionalConstraint[children.size()]);
    for (int i = 0; i < array.length; ++i) array[i] = array[i].negate();

    PropositionalConjunction result =
      new PropositionalConjunction(array[0], array[1]);
    for (int i = 2; i < array.length; ++i) result.add(array[i]);
    return result;
  }


  /**
    * Produces a new, logically simplified version of this constraint in
    * conjunctive normal form (CNF).
    *
    * @return The conjunctive normal form of this constraint.
   **/
  public PropositionalConstraint CNF() {
    PropositionalConstraint c = factor();
    if (!(c instanceof PropositionalDisjunction)) return c.CNF();

    PropositionalDisjunction simplified = (PropositionalDisjunction) c;

    PropositionalDisjunction childrenCNF = new PropositionalDisjunction();
    for (Iterator I = simplified.children.iterator(); I.hasNext(); )
      childrenCNF.add(((PropositionalConstraint) I.next()).CNF());
    if (childrenCNF.children.size() == 1)
      return (PropositionalConstraint) childrenCNF.getChildren()[0];

    PropositionalConstraint[][] children =
      new PropositionalConstraint[childrenCNF.children.size()][];
    int i = 0;
    boolean foundConjunction = false;
    for (Iterator I = childrenCNF.children.iterator(); I.hasNext(); ++i) {
      PropositionalConstraint parent = (PropositionalConstraint) I.next();
      if (parent instanceof PropositionalConjunction) {
        children[i] = (PropositionalConstraint[]) parent.getChildren();
        foundConjunction = true;
      }
      else {
        children[i] = new PropositionalConstraint[1];
        children[i][0] = parent;
      }
    }

    if (!foundConjunction) return childrenCNF;

    int[] indexes = new int[children.length];
    PropositionalConstraint result =
      new PropositionalDisjunction(children[0][0], children[1][0]);
    for (i = 2; i < children.length; ++i)
      result = new PropositionalDisjunction(result, children[i][0]);

    while (increment(children, indexes)) {
      PropositionalConstraint combination =
        new PropositionalDisjunction(children[0][indexes[0]],
                                     children[1][indexes[1]]);
      for (i = 2; i < children.length; ++i)
        combination =
          new PropositionalDisjunction(combination, children[i][indexes[i]]);
      result = new PropositionalConjunction(result, combination);
    }

    return result;
  }


  /**
    * Utility method for iterating through all combinations of constraint
    * children.
    *
    * @param c  Each element of this array is an array of children, exactly
    *           one child of which appears in each combination.
    * @param I  The indexes of the children in the current combination.
    * @return <code>true</code> iff <code>I</code> contains valid indexes for
    *         a new combination; <code>false</code> iff if there are no more
    *         combinations.
   **/
  public static boolean increment(PropositionalConstraint[][] c, int[] I) {
    int i = 0;
    while (i < c.length && ++I[i] == c[i].length) ++i;
    if (i == c.length) return false;
    for (--i; i >= 0; --i) I[i] = 0;
    return true;
  }


  /**
    * Produces a new, logically simplified version of this constraint in
    * disjunctive normal form (DNF).
    *
    * @return The disjunctive normal form of this constraint.
   **/
  public PropositionalConstraint DNF() {
    PropositionalDisjunction result = new PropositionalDisjunction();
    for (Iterator I = children.iterator(); I.hasNext(); )
      result.add(((PropositionalConstraint) I.next()).DNF());
    return result.simplify();
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
    return false;
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
    return c.size() > size() && c.containsAll(this);
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
    * If the given constraint has the same type as this constraint, its terms
    * are merged into this constraint; otherwise, it is added as a new term.
    *
    * @param c  The constraint to add.
   **/
  public void add(PropositionalConstraint c) {
    if (c instanceof PropositionalDisjunction) {
      PropositionalConstraint[] terms =
        (PropositionalConstraint[]) c.getChildren();
      for (int i = 0; i < terms.length; ++i) add(terms[i]);
    }
    else children.add(c);
  }


  /**
    * Factoring a disjunction is the opposite of distributing a conjunction
    * over a disjunction.
    *
    * @return A constraint that represents a factoring of this disjunction.
   **/
  public PropositionalConstraint factor() {
    PropositionalConstraint c = simplify();
    if (!(c instanceof PropositionalDisjunction)) return c;
    PropositionalDisjunction simplified = (PropositionalDisjunction) c;

    PropositionalConstraint[] best = new PropositionalConstraint[0];
    while (best != null) {
      int bestConjunction = -1;
      int bestOther = -1;
      best = null;

      PropositionalConstraint[] children =
        (PropositionalConstraint[]) simplified.getChildren();
      Arrays.sort(children,
          new Comparator() {
            public int compare(Object o1, Object o2) {
              if (o1 instanceof PropositionalConjunction) {
                if (o2 instanceof PropositionalConjunction) return 0;
                return -1;
              }

              if (o2 instanceof PropositionalConjunction) return 1;
              return 0;
            }
          });

      for (int i = 0;
           i < children.length - 1
             && children[i] instanceof PropositionalConjunction;
           ++i)
        for (int j = i + 1; j < children.length; ++j) {
          PropositionalConstraint[] current =
            ((PropositionalConjunction) children[i]).intersect(children[j]);
          if (current != null
              && (best == null || current.length > best.length)) {
            best = current;
            bestConjunction = i;
            bestOther = j;
          }
        }

      if (best != null) {
        PropositionalConstraint toAdd = null;
        if (best.length == 1) toAdd = best[0];
        else {
          toAdd = new PropositionalConjunction(best[0], best[1]);
          for (int i = 2; i < best.length; ++i)
            toAdd = new PropositionalConjunction(toAdd, best[i]);
        }

        if (children[bestOther] instanceof PropositionalConjunction) {
          PropositionalConstraint conjunct1 =
            ((PropositionalConjunction) children[bestConjunction])
            .subtract(best);
          PropositionalConstraint conjunct2 =
            ((PropositionalConjunction) children[bestOther]).subtract(best);

          toAdd =
            new PropositionalConjunction(
                toAdd,
                new PropositionalDisjunction(conjunct1, conjunct2))
            .simplify();
        }

        simplified.children.remove(children[bestConjunction]);
        simplified.children.remove(children[bestOther]);
        simplified.add(toAdd);
      }
    }

    if (simplified.children.size() == 1)
      return (PropositionalConstraint) simplified.getChildren()[0];
    return simplified;
  }


  /**
    * The intersection of two disjunctions is the set of all terms that are
    * common to both disjunctions; the intersection of a disjunction and some
    * other constraint <i>c</i> is <i>c</i> if <i>c</i> is contained in the
    * disjunction and the empty set otherwise.
    *
    * @param c  The constraint to intersect with.
    * @return The set of common terms in array form or <code>null</code> if
    *         there are none.
   **/
  public PropositionalConstraint[] intersect(PropositionalConstraint c) {
    if (!(c instanceof PropositionalDisjunction)) {
      if (children.contains(c)) return new PropositionalConstraint[]{ c };
      return null;
    }

    PropositionalDisjunction disjunction = (PropositionalDisjunction) c;
    LinkedList result = new LinkedList();
    for (Iterator I = children.iterator(); I.hasNext(); ) {
      Object next = I.next();
      if (disjunction.children.contains(next)) result.add(next);
    }

    if (result.size() == 0) return null;
    return (PropositionalConstraint[])
           result.toArray(new PropositionalConstraint[result.size()]);
  }


  /**
    * Subtraction from a disjunction simply removes all of the specified
    * terms from it; this method returns a new constraint representing the
    * subtraction.
    *
    * @param terms  The terms to remove.
    * @return A new representation of this n-ary constraint with the specified
    *         terms removed.
   **/
  public PropositionalConstraint subtract(PropositionalConstraint[] terms) {
    PropositionalDisjunction clone = (PropositionalDisjunction) clone();
    for (int i = 0; i < terms.length; ++i) clone.children.remove(terms[i]);
    if (clone.children.size() == 0) return new PropositionalConstant(false);
    if (clone.children.size() == 1)
      return (PropositionalConstraint) clone.getChildren()[0];
    return clone;
  }


  /**
    * Distributes the given conjunction over this disjunction.
    *
    * @return A simplified constraint representing the distribution of the
    *         given conjunction over this disjunction.
   **/
  public PropositionalConstraint distribute(PropositionalConjunction c) {
    PropositionalConstraint[] array =
      (PropositionalConstraint[])
      children.toArray(new PropositionalConstraint[children.size()]);
    for (int i = 0; i < array.length; ++i) {
      PropositionalConjunction clone = (PropositionalConjunction) c.clone();
      clone.add(array[i]);
      array[i] = clone;
    }

    if (array.length == 1) return array[0].simplify();

    PropositionalDisjunction result =
      new PropositionalDisjunction(array[0], array[1]);
    for (int i = 2; i < array.length; ++i) result.add(array[i]);
    return result.simplify();
  }


  /**
    * Determines whether this disjunction contains all of the terms that the
    * given disjunction contains.
    *
    * @param d  The given disjunction.
    * @return <code>true</code> iff this disjunction contains all of the terms
    *         that the given disjunction contains.
   **/
  public boolean containsAll(PropositionalDisjunction d) {
    return children.containsAll(d.children);
  }


  /**
    * The hash code of a <code>PropositionalDisjunction</code> is the sum of
    * the hash codes of its children.
    *
    * @return The hash code for this <code>PropositionalDisjunction</code>.
   **/
  public int hashCode() {
    int result = 0;
    for (Iterator I = children.iterator(); I.hasNext(); )
      result += I.next().hashCode();
    return result;
  }


  /**
    * Two <code>PropositionalDisjunction</code>s are equivalent when they are
    * topologically equivalent, respecting the associativity and commutivity
    * of disjunction.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>PropositionalDisjunction</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof PropositionalDisjunction)) return false;
    PropositionalDisjunction d = (PropositionalDisjunction) o;
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


  /**
    * Creates a string respresentation of this constraint using the string
    * representations of the objects involved.
    *
    * @param buffer The output of this method will be appended to this buffer.
   **/
  public void write(StringBuffer buffer) {
    buffer.append("(");

    PropositionalConstraint[] children =
      (PropositionalConstraint[]) getChildren();
    children[0].write(buffer);
    for (int i = 1; i < children.length; ++i) {
      buffer.append(" \\/ ");
      children[i].write(buffer);
    }

    buffer.append(")");
  }
}

