package LBJ2.infer;

import java.util.AbstractMap;


/**
  * A constraint is an expression that is either satisified or unsatisfied by
  * its constituent classifier applications.
  *
  * @author Nick Rizzolo
 **/
public abstract class Constraint
{
  /** Determines whether the constraint is satisfied. */
  abstract public boolean evaluate();


  /**
    * Replaces all unquantified variables with the unique copy stored as a
    * value of the given map; also instantiates all quantified variables and
    * stores them in the given map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  abstract public void consolidateVariables(AbstractMap m);


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  abstract public Constraint[] getChildren();


  /**
    * Calls the appropriate <code>visit(&middot;)</code> method of the given
    * <code>Inference</code> for this <code>Constraint</code>, as per the
    * visitor pattern.
    *
    * @param infer  The inference visiting this constraint.
   **/
  abstract public void runVisit(Inference infer);
}

