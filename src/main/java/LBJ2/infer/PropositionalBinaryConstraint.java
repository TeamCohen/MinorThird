package LBJ2.infer;


/**
  * Represents a propositional constraint involving a binary operator.
  *
  * @author Nick Rizzolo
 **/
public abstract class PropositionalBinaryConstraint
                extends PropositionalConstraint
{
  /** The constraint on the left of the operator. */
  protected PropositionalConstraint left;
  /** The constraint on the right of the operator. */
  protected PropositionalConstraint right;


  /**
    * Initializing constructor.
    *
    * @param l  The constraint on the left of the operator.
    * @param r  The constraint on the right of the operator.
   **/
  public PropositionalBinaryConstraint(PropositionalConstraint l,
                                       PropositionalConstraint r) {
    left = l;
    right = r;
  }


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(java.util.AbstractMap m) {
    if (left instanceof PropositionalVariable) {
      if (m.containsKey(left)) left = (PropositionalVariable) m.get(left);
      else m.put(left, left);
    }
    else left.consolidateVariables(m);

    if (right instanceof PropositionalVariable) {
      if (m.containsKey(right)) right = (PropositionalVariable) m.get(right);
      else m.put(right, right);
    }
    else right.consolidateVariables(m);
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() {
    return new PropositionalConstraint[]{ left, right };
  }
}

