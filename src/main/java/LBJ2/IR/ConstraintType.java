package LBJ2.IR;

import LBJ2.Pass;
import LBJ2.classify.DiscreteFeature;


/**
  * A constraint's type is defined by what it takes as input.  This class does
  * not represent a syntax that appears in the source - it is constructed only
  * during semantic analysis.
  *
  * @author Nick Rizzolo
 **/
public class ConstraintType extends ClassifierType
{
  /**
    * Initializing constructor.
    *
    * @param i  The classifier's input type.
   **/
  public ConstraintType(Type i) {
    super(i,
          new ClassifierReturnType(
              ClassifierReturnType.DISCRETE,
              new ConstantList(DiscreteFeature.BooleanValues)),
          false);

    try { myClass = Class.forName("LBJ2.infer.ParameterizedConstraint"); }
    catch (Exception e) {
      System.err.println("Class 'LBJ2.infer.ParameterizedConstraint' not "
                         + "found.  Aborting.");
      System.exit(1);
    }
  }


  /**
    * Two <code>ConstraintType</code>s are equivalent when their input types
    * match.
    *
    * @param o  The object whose equality with this object needs to be tested.
    * @return <code>true</code> if the two objects are equal, and
    *         <code>false</code> otherwise.
   **/
  public boolean equals(Object o) {
    return o instanceof ConstraintType
           && input.equals(((ConstraintType) o).input);
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(1);
    I.children[0] = input;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() { return new ConstraintType((Type) input.clone()); }


  /**
    * Ensures that the correct <code>run()</code> method is called for this
    * type of node.
    *
    * @param pass The pass whose <code>run()</code> method should be called.
   **/
  public void runPass(Pass pass) { pass.run(this); }


  /**
    * Writes a string representation of this <code>ASTNode</code> to the
    * specified buffer.  The representation written is parsable by the LBJ2
    * compiler, but not very readable.
    *
    * @param buffer The buffer to write to.
   **/
  public void write(StringBuffer buffer) {
    buffer.append("Constraint { ");
    input.write(buffer);
    buffer.append(" }");
  }
}

