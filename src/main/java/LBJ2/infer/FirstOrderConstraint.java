package LBJ2.infer;

import java.util.Vector;


/**
  * All classes for representing first order constraints are derived from this
  * base class.  A first order constraint is:
  *
  * <ul>
  *   <li> The constant <code>true</code> or the constant <code>false</code>.
  *   <li>
  *     An equality or inequality between a classifier application and a
  *     value (which may be specified with an arbitrary java expression) or
  *     between two classifier applications.  Operators: <code>== !=</code>
  *   <li> The negation of a first order constraint: <code>~</code>
  *   <li> The conjunction of two first order constraints: <code>/\</code>
  *   <li> The disjunction of two first order constraints: <code>\/</code>
  *   <li>
  *     An implication between two first order constraints: <code>=&gt;</code>
  *   <li>
  *     A double implication between two first order constraints:
  *     <code>&lt;=&gt;</code>
  *   <li>
  *     An existential quantification: <code>exists <i>identifier</i> in
  *     <i>identifier</i>, <i>first-order-constraint</i></code> <br>
  *     The second identifier must refer to a Java <code>Collection</code>.
  *     The first identifier is a new Java variable of type
  *     <code>Object</code> that appears in the first order constraint.
  *   <li>
  *     A universal quantification: <code>forall <i>identifier</i> in
  *     <i>identifier</i>, <i>first-order-constraint</i></code> <br>
  *     The second identifier must refer to a Java <code>Collection</code>.
  *     The first identifier is a new Java variable of type
  *     <code>Object</code> that appears in the first order constraint.
  *   <li>
  *     An <i>at least</i> counting quantification: <code>atleast
  *     <i>expression</i> of <i>identifier</i> in <i>identifier</i>,
  *     <i>first-order-constraint</i></code> <br>
  *     The <i>expression</i> is arbitrary Java that must evaluate to a
  *     double.  The two identifiers play the same role as in the other
  *     quatifications.  This quatification is satisfied when the number of
  *     objects in the collection that satisfy <i>first-order-constraint</i>
  *     is greater than or equal to <i>expression</i>.
  *   <li>
  *     An <i>at most</i> counting quatification: <code>atmost
  *     <i>expression</i> of <i>identifier</i> in <i>identifier</i>,
  *     <i>first-order-constraint</i></code> <br>
  *     The <i>expression</i> is arbitrary Java that must evaluate to a
  *     double.  The two identifiers play the same role as in the other
  *     quatifications.  This quatification is satisfied when the number of
  *     objects in the collection that satisfy <i>first-order-constraint</i>
  *     is less than or equal to <i>expression</i>.
  * </ul>
 **/
public abstract class FirstOrderConstraint extends Constraint
{
  /**
    * This method sets the given quantification variables to the given object
    * references and evaluates the expressions involving those variables in
    * this constraint's <code>FirstOrderEquality</code> children.
    *
    * @param o  The new object references for the enclosing quantification
    *           variables, in order of nesting.
   **/
  abstract public void setQuantificationVariables(Vector o);


  /**
    * Transforms this first order constraint into a propositional constraint.
    *
    * @return The propositionalized constraint.
   **/
  abstract public PropositionalConstraint propositionalize();
}

