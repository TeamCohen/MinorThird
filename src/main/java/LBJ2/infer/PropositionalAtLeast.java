package LBJ2.infer;


/**
  * Represents the constraint that at least <code>m</code> of the children
  * constraints must be true.
  *
  * @author Nick Rizzolo
 **/
public class PropositionalAtLeast extends PropositionalNAryConstraint
{
  /** The children are stored in an array in this class. */
  protected PropositionalConstraint[] children;
  /** The number of child constraints that must be true. */
  protected int m;


  /** Default constructor. */
  private PropositionalAtLeast() { }

  /**
    * Initializing constructor.
    *
    * @param c  A collection of children constraints.
    * @param m  The number of children that must be true.
   **/
  public PropositionalAtLeast(PropositionalConstraint[] c, int m) {
    this.m = m;
    children = c;
    super.children = null;
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() {
    return (PropositionalConstraint[]) children.clone();
  }


  /** Returns the value of <code>m</code>. */
  public int getM() { return m; }


  /**
    * Determines whether the given constraint is a term of this constraint.
    *
    * @param c  The given constraint.
    * @return <code>true</code> iff the given constraint is contained in this
    *         constraint.
   **/
  public boolean contains(PropositionalConstraint c) {
    for (int i = 0; i < children.length; ++i)
      if (c.equals(children[i])) return true;
    return false;
  }


  /**
    * Returns the number of terms in this constraint.
    *
    * @return The number of terms in this constraint.
   **/
  public int size() { return children.length; }


  /** Determines whether the constraint is satisfied. */
  public boolean evaluate() {
    int trueChildren = 0;
    for (int i = 0; i < children.length && trueChildren < m; ++i)
      if (children[i].evaluate()) ++trueChildren;
    return trueChildren == m;
  }


  /**
    * Replaces the <code>children</code> array with a new array containing all
    * the same elements except the element with the given index.
    *
    * @param r  The index of the child to remove.
   **/
  public void remove(int r) {
    PropositionalConstraint[] temp =
      new PropositionalConstraint[children.length - 1];

    for (int i = 0, j = 0; i < children.length; ++i) {
      if (i == r) continue;
      temp[j++] = children[i];
    }

    children = temp;
  }


  /**
    * Produces a new, logically simplified version of this constraint,
    * preserving variable consolidation.
    *
    * @see    Constraint#consolidateVariables(java.util.AbstractMap)
    * @return A logically simplified version of this constraint.
   **/
  public PropositionalConstraint simplify() {
    if (m <= 0) return PropositionalConstant.True;
    if (m > children.length) return PropositionalConstant.False;

    PropositionalAtLeast result = new PropositionalAtLeast();
    result.m = m;
    result.children = new PropositionalConstraint[children.length];
    for (int i = 0; i < children.length; ++i)
      result.children[i] = children[i].simplify();

    for (int i = result.children.length - 1; i >= 0; --i) {
      if (result.children[i] == PropositionalConstant.True) {
        result.remove(i);
        --result.m;
      }
      else if (result.children[i] == PropositionalConstant.False)
        result.remove(i);
    }

    /*
    HashSet positive = new HashSet();
    HashSet negative = new HashSet();
    for (int i = 0; i < result.children.length; ++i) {
      if (result.children[i] instanceof PropositionalNegation)
        negative.add(result.children[i].getChildren()[0]);
      else positive.add(result.children[i]);
    }

    for (Iterator I = positive.iterator(); I.hasNext(); ) {
      PropositionalConstraint p = (PropositionalConstraint) I.next();
      if (negative.contains(p)) {
        LinkedList positiveIndexes = new LinkedList();
        LinkedList negativeIndexes = new LinkedList();
        for (int i = 0; i < result.children.length; ++i) {
          if (result.children[i].equals(p))
            positiveIndexes.add(new Integer(i));
          else if (result.children[i].equals(new PropositionalNegation(p)))
            negativeIndexes.add(new Integer(i));
        }

        int toRemove = positiveIndexes.size();
        if (negativeIndexes.size() < toRemove)
          toRemove = negativeIndexes.size();

        Integer[] removedIndexes = new Integer[toRemove * 2];
        for (int i = 0; i < toRemove; ++i) {
          removedIndexes[2 * i] = (Integer) positiveIndexes.removeLast();
          removedIndexes[2 * i + 1] = (Integer) negativeIndexes.removeLast();
        }

        Arrays.sort(removedIndexes);
        for (int i = removedIndexes.length - 1; i >= 0; --i)
          result.remove(removedIndexes[i].intValue());
        result.m -= toRemove;
      }
    }
    */

    if (result.m <= 0) return PropositionalConstant.True;
    if (result.m > result.children.length) return PropositionalConstant.False;
    if (result.children.length == 1) return result.children[0];
    if (result.m == 1) {
      PropositionalDisjunction disjunction =
        new PropositionalDisjunction(result.children[0], result.children[1]);
      for (int i = 2; i < result.children.length; ++i)
        disjunction =
          new PropositionalDisjunction(disjunction, result.children[i]);
      return disjunction.simplify();
    }

    return result;
  }


  /**
    * The negation of an at-least(m) is the at-least(n-m+1) of the negated
    * children.
    *
    * @return A simplified constraint representing the negation of this
    *         constraint.
   **/
  public PropositionalConstraint negate() {
    PropositionalAtLeast result = new PropositionalAtLeast();
    result.children = new PropositionalConstraint[children.length];
    for (int i = 0; i < children.length; ++i)
      result.children[i] = children[i].negate();
    result.m = children.length - m + 1;
    return result;
  }


  /**
    * Produces a new, logically simplified version of this constraint in
    * conjunctive normal form (CNF).
    *
    * @return The conjunctive normal form of this constraint.
   **/
  public PropositionalConstraint CNF() { return DNF().CNF(); }


  /**
    * Produces a new, logically simplified version of this constraint in
    * disjunctive normal form (DNF).
    *
    * @return The disjunctive normal form of this constraint.
   **/
  public PropositionalConstraint DNF() {
    PropositionalConstraint result = null;

    if (m == 1) {
      result = new PropositionalDisjunction(children[0], children[1]);
      for (int i = 2; i < m; ++i)
        result = new PropositionalDisjunction(result, children[i]);
    }
    else {
      result = new PropositionalConjunction(children[0], children[1]);
      for (int i = 2; i < m; ++i)
        result = new PropositionalConjunction(result, children[i]);

      int[] indexes = new int[m];
      for (int i = 0; i < m; ++i) indexes[i] = i;

      while (nextChoice(indexes, children.length - 1)) {
        PropositionalConjunction term =
          new PropositionalConjunction(children[indexes[0]],
                                       children[indexes[1]]);
        for (int i = 2; i < m; ++i)
          term = new PropositionalConjunction(term, children[indexes[i]]);
        result = new PropositionalDisjunction(result, term);
      }
    }

    return result;
  }


  /**
    * Given a particular choice of k of the first n non-negative integers,
    * this method computes the next logical choice of k integers, modifying
    * the input array to contain that choice.  The parameter <code>I</code>
    * contains the current choice, and it must be sorted in ascending order.
    * The parameter <code>max</code> contains the largest allowable value for
    * any integer in <code>I</code>.  Therefore, n = <code>max</code> + 1, and
    * k = <code>I.length</code>.  It is also assumed that the "first" choice
    * is the integers 0 through k - 1 inclusive and the "last" choice is
    * <code>max</code> - k + 1 through <code>max</code> inclusive.
    *
    * @param I    The current choice of k out of the first n non-negative
    *             integers, sorted in decreasing order.
    * @param max  The largest value allowed to appear in <code>I</code> (n -
    *             1).
    * @return <code>true</code> iff the input did not represent the last
    *         choice.
   **/
  protected static boolean nextChoice(int[] I, int max) {
    int i = 1;
    while (i < I.length && I[i] - I[i - 1] == 1) ++i;
    if (i == I.length && I[i - 1] == max) return false;
    ++I[i - 1];
    for (int j = 0; j < i - 1; ++j) I[j] = j;
    return true;
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
    *         constraint is more general than the given implication.
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
    *         constraint is more general than the given double implication.
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
    *         constraint is more general than the given conjunction.
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
    *         constraint is more general than the given disjunction.
   **/
  public boolean moreSpecificThan(PropositionalDisjunction c) {
    return false;
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
    if (c.children.length != children.length) return false;
    for (int i = 0; i < children.length; ++i)
      if (!children[i].equals(c.children[i])) return false;
    return m >= c.m;
  }


  /**
    * Compares topology to determine if this constraint is more specific than
    * the given negation; <i>note: this method is not required to be correct
    * when it answers <code>false</code></i>.
    *
    * @param c  The given negation.
    * @return <code>true</code> if a topological analysis determined that this
    *         constraint is more general than the given negation.
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
    *         constraint is more general than the given variable.
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
    *         constraint is more general than the given constant.
   **/
  public boolean moreSpecificThan(PropositionalConstant c) {
    return c.evaluate();
  }


  /**
    * The hash code of a <code>PropositionalAtLeast</code> is the sum of
    * the hash codes of its children plus two.
    *
    * @return The hash code for this <code>PropositionalConjunction</code>.
   **/
  public int hashCode() {
    int result = 2;
    for (int i = 0; i < children.length; ++i)
      result += children[i].hashCode();
    return result;
  }


  /**
    * Two <code>PropositionalAtLeast</code>s are equivalent when they are
    * topologically equivalent; this implementation currently does not respect
    * the associativity and commutativity of at-least.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>PropositionalAtLeast</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof PropositionalAtLeast)) return false;
    PropositionalAtLeast a = (PropositionalAtLeast) o;
    if (children.length != a.children.length) return false;
    for (int i = 0; i < children.length; ++i)
      if (!children[i].equals(a.children[i])) return false;
    return true;
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
    buffer.append("(atleast " + m + " of ");

    children[0].write(buffer);
    for (int i = 1; i < children.length; ++i) {
      buffer.append(", ");
      children[i].write(buffer);
    }

    buffer.append(")");
  }


  /**
    * This method returns a shallow clone.
    *
    * @return A shallow clone.
   **/
  public Object clone() {
    PropositionalAtLeast clone = null;

    try { clone = (PropositionalAtLeast) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning PropositionalAtLeast:");
      e.printStackTrace();
      System.exit(1);
    }

    clone.children = (PropositionalConstraint[]) clone.children.clone();
    return clone;
  }
}

