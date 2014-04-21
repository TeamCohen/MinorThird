package LBJ2.infer;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;


/**
  * Represents a propositional constraint with an arbitrary number of
  * arguments, usually assumed to be greater than or equal to 2.
  *
  * @author Nick Rizzolo
 **/
public abstract class PropositionalNAryConstraint
                extends PropositionalConstraint
{
  /** The children of the operator. */
  protected HashSet children;


  /** Default constructor. */
  public PropositionalNAryConstraint() { children = new HashSet(); }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(AbstractMap m) {
    LinkedList toRemove = new LinkedList();

    for (Iterator I = children.iterator(); I.hasNext(); ) {
      Object next = I.next();
      if (next instanceof PropositionalVariable) toRemove.add(next);
      else ((PropositionalConstraint) next).consolidateVariables(m);
    }

    for (Iterator I = toRemove.iterator(); I.hasNext(); ) {
      PropositionalVariable v = (PropositionalVariable) I.next();
      if (m.containsKey(v)) {
        children.remove(v);
        children.add(m.get(v));
      }
      else m.put(v, v);
    }
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() {
    return (PropositionalConstraint[])
           children.toArray(new PropositionalConstraint[children.size()]);
  }


  /**
    * Determines whether the given constraint is a term of this constraint.
    *
    * @param c  The given constraint.
    * @return <code>true</code> iff the given constraint is contained in this
    *         constraint.
   **/
  public boolean contains(PropositionalConstraint c) {
    return children.contains(c);
  }


  /**
    * Returns the number of terms in this constraint.
    *
    * @return The number of terms in this constraint.
   **/
  public int size() { return children.size(); }


  /**
    * This method returns a shallow clone.
    *
    * @return A shallow clone.
   **/
  public Object clone() {
    PropositionalNAryConstraint clone = null;

    try { clone = (PropositionalNAryConstraint) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning " + getClass().getName() + ":");
      e.printStackTrace();
      System.exit(1);
    }

    clone.children = (HashSet) clone.children.clone();
    return clone;
  }
}

