package LBJ2.infer;

import java.util.*;


/**
  * Represents the conjunction of two propositional constraints.
  *
  * @author Nick Rizzolo
 **/
public class PropositionalConjunction extends PropositionalNAryConstraint
{
  /** Default constructor. */
  private PropositionalConjunction() { }

  /**
    * If either of the arguments is itself a
    * <code>PropositionalConjunction</code>, its contents are flattened into
    * this <code>PropositionalConjunction</code>.
    *
    * @param c1 One constraint to disjunct.
    * @param c2 Another constraint to disjunct.
   **/
  public PropositionalConjunction(PropositionalConstraint c1,
                                  PropositionalConstraint c2) {
    add(c1);
    add(c2);
  }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() {
    for (Iterator I = children.iterator(); I.hasNext(); )
      if (!((PropositionalConstraint) I.next()).evaluate()) return false;
    return true;
  }


  /**
    * Produces a new, logically simplified version of this constraint,
    * preserving variable consolidation.
    *
    * @see    Constraint#consolidateVariables(java.util.AbstractMap)
    * @return A logically simplified version of this constraint.
   **/
  public PropositionalConstraint simplify() { return simplify(false); }


  /**
    * Same as <code>simplify()</code>, except this method gives the caller the
    * ability to optionally leave double implications that are immediate
    * children of this conjunction in tact.
    *
    * @param d  <code>true</code> iff double implications that are immediate
    *           children of this conjunction are to be left in tact.
    * @return A logically simplified version of this constraint.
   **/
  public PropositionalConstraint simplify(boolean d) {
    PropositionalConjunction result = new PropositionalConjunction();

    if (d) {
      for (Iterator I = children.iterator(); I.hasNext(); ) {
        PropositionalConstraint c = (PropositionalConstraint) I.next();

        if (c instanceof PropositionalDoubleImplication) {
          PropositionalDoubleImplication di =
            (PropositionalDoubleImplication) c;
          di.left = di.left.simplify();
          di.right = di.right.simplify();

          if (di.left.equals(di.right)) c = PropositionalConstant.True;
          else if (di.left.equals(PropositionalConstant.False))
            c = di.right.negate().simplify();
          else if (di.left.equals(PropositionalConstant.True)) c = di.right;
          else if (di.right.equals(PropositionalConstant.False))
            c = di.left.negate().simplify();
          else if (di.right.equals(PropositionalConstant.True)) c = di.left;
          /*
          else if (di.right instanceof PropositionalNegation
                      && di.left.equals(di.right.getChildren()[0])
                   || di.left instanceof PropositionalNegation
                      && di.right.equals(di.left.getChildren()[0]))
            c = PropositionalConstant.False;
          */
        }
        else c = c.simplify();

        result.add(c);
      }
    }
    else {
      for (Iterator I = children.iterator(); I.hasNext(); )
        result.add(((PropositionalConstraint) I.next()).simplify());
    }

    if (result.children.contains(PropositionalConstant.False))
      return PropositionalConstant.False;

    result.children.remove(PropositionalConstant.True);

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
      if (negative.contains(I.next())) return PropositionalConstant.False;

    PropositionalConstraint[] terms =
      (PropositionalConstraint[]) getChildren();
    HashSet toRemove = new HashSet();
    for (int i = 0; i < terms.length - 1; ++i)
      for (int j = i + 1; j < terms.length; ++j) {
        if (terms[i].moreGeneralThan(terms[j])) toRemove.add(new Integer(i));
        if (terms[j].moreGeneralThan(terms[i])) toRemove.add(new Integer(j));
      }

    for (Iterator I = toRemove.iterator(); I.hasNext(); )
      result.children.remove(terms[((Integer) I.next()).intValue()]);
    */

    if (result.children.size() == 0) return PropositionalConstant.True;

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

    PropositionalDisjunction result =
      new PropositionalDisjunction(array[0], array[1]);
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
    PropositionalConjunction result = new PropositionalConjunction();
    for (Iterator I = children.iterator(); I.hasNext(); )
      result.add(((PropositionalConstraint) I.next()).CNF());
    return result.simplify();
  }


  /**
    * Produces a new, logically simplified version of this constraint in
    * disjunctive normal form (DNF).
    *
    * @return The disjunctive normal form of this constraint.
   **/
  public PropositionalConstraint DNF() {
    PropositionalConstraint c = factor();
    if (!(c instanceof PropositionalConjunction)) return c.DNF();

    PropositionalConjunction simplified = (PropositionalConjunction) c;

    PropositionalConjunction childrenDNF = new PropositionalConjunction();
    for (Iterator I = simplified.children.iterator(); I.hasNext(); )
      childrenDNF.add(((PropositionalConstraint) I.next()).DNF());
    if (childrenDNF.children.size() == 1)
      return (PropositionalConstraint) childrenDNF.getChildren()[0];

    PropositionalConstraint[][] children =
      new PropositionalConstraint[childrenDNF.children.size()][];
    int i = 0;
    boolean foundDisjunction = false;
    for (Iterator I = childrenDNF.children.iterator(); I.hasNext(); ++i) {
      PropositionalConstraint parent = (PropositionalConstraint) I.next();
      if (parent instanceof PropositionalDisjunction) {
        children[i] = (PropositionalConstraint[]) parent.getChildren();
        foundDisjunction = true;
      }
      else {
        children[i] = new PropositionalConstraint[1];
        children[i][0] = parent;
      }
    }

    if (!foundDisjunction) return childrenDNF;

    int[] indexes = new int[children.length];
    PropositionalConstraint result =
      new PropositionalConjunction(children[0][0], children[1][0]);
    for (i = 2; i < children.length; ++i)
      result = new PropositionalConjunction(result, children[i][0]);

    while (PropositionalDisjunction.increment(children, indexes)) {
      PropositionalConstraint combination =
        new PropositionalConjunction(children[0][indexes[0]],
                                     children[1][indexes[1]]);
      for (i = 2; i < children.length; ++i)
        combination =
          new PropositionalConjunction(combination, children[i][indexes[i]]);
      result = new PropositionalDisjunction(result, combination);
    }

    return result;
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
    return size() > 1 && contains(c);
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
    return size() > 1 && contains(c);
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
    return size() > c.size() && containsAll(c);
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
    return size() > 1 && contains(c);
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
    return size() > 1 && contains(c);
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
    return size() > 1 && contains(c);
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
    if (c instanceof PropositionalConjunction) {
      PropositionalConstraint[] terms =
        (PropositionalConstraint[]) c.getChildren();
      for (int i = 0; i < terms.length; ++i) add(terms[i]);
    }
    else children.add(c);
  }


  /**
    * Factoring a conjunction is the opposite of distributing a disjunction
    * over a conjunction.
    *
    * @return A constraint that represents a factoring of this conjunction.
   **/
  public PropositionalConstraint factor() {
    PropositionalConstraint c = simplify();
    if (!(c instanceof PropositionalConjunction)) return c;
    PropositionalConjunction simplified = (PropositionalConjunction) c;

    PropositionalConstraint[] best = new PropositionalConstraint[0];
    while (best != null) {
      int bestDisjunction = -1;
      int bestOther = -1;
      best = null;

      PropositionalConstraint[] children =
        (PropositionalConstraint[]) simplified.getChildren();
      Arrays.sort(children,
          new Comparator() {
            public int compare(Object o1, Object o2) {
              if (o1 instanceof PropositionalDisjunction) {
                if (o2 instanceof PropositionalDisjunction) return 0;
                return -1;
              }

              if (o2 instanceof PropositionalDisjunction) return 1;
              return 0;
            }
          });

      for (int i = 0;
           i < children.length - 1
             && children[i] instanceof PropositionalDisjunction;
           ++i)
        for (int j = i + 1; j < children.length; ++j) {
          PropositionalConstraint[] current =
            ((PropositionalDisjunction) children[i]).intersect(children[j]);
          if (current != null
              && (best == null || current.length > best.length)) {
            best = current;
            bestDisjunction = i;
            bestOther = j;
          }
        }

      if (best != null) {
        PropositionalConstraint toAdd = null;
        if (best.length == 1) toAdd = best[0];
        else {
          toAdd = new PropositionalDisjunction(best[0], best[1]);
          for (int i = 2; i < best.length; ++i)
            toAdd = new PropositionalDisjunction(toAdd, best[i]);
        }

        if (children[bestOther] instanceof PropositionalDisjunction) {
          PropositionalConstraint disjunct1 =
            ((PropositionalDisjunction) children[bestDisjunction])
            .subtract(best);
          PropositionalConstraint disjunct2 =
            ((PropositionalDisjunction) children[bestOther]).subtract(best);

          toAdd =
            new PropositionalDisjunction(
                toAdd,
                new PropositionalConjunction(disjunct1, disjunct2))
            .simplify();
        }

        simplified.children.remove(children[bestDisjunction]);
        simplified.children.remove(children[bestOther]);
        simplified.add(toAdd);
      }
    }

    if (simplified.children.size() == 1)
      return (PropositionalConstraint) simplified.getChildren()[0];
    return simplified;
  }


  /**
    * The intersection of two conjunctions is the set of all terms that are
    * common to both conjunctions; the intersection of a conjunction and some
    * other constraint <i>c</i> is <i>c</i> if <i>c</i> is contained in the
    * conjunction and the empty set otherwise.
    *
    * @param c  The constraint to intersect with.
    * @return The set of common terms in array form or <code>null</code> if
    *         there are none.
   **/
  public PropositionalConstraint[] intersect(PropositionalConstraint c) {
    if (!(c instanceof PropositionalConjunction)) {
      if (children.contains(c)) return new PropositionalConstraint[]{ c };
      return null;
    }

    PropositionalConjunction conjunction = (PropositionalConjunction) c;
    LinkedList result = new LinkedList();
    for (Iterator I = children.iterator(); I.hasNext(); ) {
      Object next = I.next();
      if (conjunction.children.contains(next)) result.add(next);
    }

    if (result.size() == 0) return null;
    return (PropositionalConstraint[])
           result.toArray(new PropositionalConstraint[result.size()]);
  }


  /**
    * Subtraction from a conjunction simply removes all of the specified
    * terms from it; this method returns a new constraint representing the
    * subtraction.
    *
    * @param terms  The terms to remove.
    * @return A new representation of this n-ary constraint with the specified
    *         terms removed.
   **/
  public PropositionalConstraint subtract(PropositionalConstraint[] terms) {
    PropositionalConjunction clone = (PropositionalConjunction) clone();
    for (int i = 0; i < terms.length; ++i) clone.children.remove(terms[i]);
    if (clone.children.size() == 0) return new PropositionalConstant(true);
    if (clone.children.size() == 1)
      return (PropositionalConstraint) clone.getChildren()[0];
    return clone;
  }


  /**
    * Distributes the given disjunction over this conjunction.
    *
    * @return A simplified constraint representing the distribution of the
    *         given disjunction over this conjunction.
   **/
  public PropositionalConstraint distribute(PropositionalDisjunction d) {
    PropositionalConstraint[] array =
      (PropositionalConstraint[])
      children.toArray(new PropositionalConstraint[children.size()]);
    for (int i = 0; i < array.length; ++i) {
      PropositionalDisjunction clone = (PropositionalDisjunction) d.clone();
      clone.add(array[i]);
      array[i] = clone;
    }

    if (array.length == 1) return array[0].simplify();

    PropositionalConjunction result =
      new PropositionalConjunction(array[0], array[1]);
    for (int i = 2; i < array.length; ++i) result.add(array[i]);
    return result.simplify();
  }


  /**
    * Determines whether this conjunction contains all of the terms that the
    * given conjunction contains.
    *
    * @param c  The given conjunction.
    * @return <code>true</code> iff this conjunction contains all of the terms
    *         that the given conjunction contains.
   **/
  public boolean containsAll(PropositionalConjunction c) {
    return children.containsAll(c.children);
  }


  /**
    * The hash code of a <code>PropositionalConjunction</code> is the sum of
    * the hash codes of its children plus one.
    *
    * @return The hash code for this <code>PropositionalConjunction</code>.
   **/
  public int hashCode() {
    int result = 1;
    for (Iterator I = children.iterator(); I.hasNext(); )
      result += I.next().hashCode();
    return result;
  }


  /**
    * Two <code>PropositionalConjunction</code>s are equivalent when they are
    * topologically equivalent, respecting the associativity and commutivity
    * of conjunction.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>PropositionalConjunction</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof PropositionalConjunction)) return false;
    PropositionalConjunction c = (PropositionalConjunction) o;
    return children.equals(c.children);
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
      buffer.append(" /\\ ");
      children[i].write(buffer);
    }

    buffer.append(")");
  }
}

