package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents the assignment of a value to a storage location.
  *
  * @author Nick Rizzolo
 **/
public class Assignment extends StatementExpression
{
  /** (&not;&oslash;) The assignment operation. */
  public Operator operation;
  /** (&not;&oslash;) The left hand side of the assignment. */
  public Expression left;
  /** (&not;&oslash;) The right hand side of the assignment. */
  public Expression right;


  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the representation of the operator.
    *
    * @param op Reference to the operator's representation.
    * @param l  Reference to the left hand side's representation.
    * @param r  Reference to the right hand side's representation.
   **/
  public Assignment(Operator op, Expression l, Expression r) {
    super(op.line, op.byteOffset);
    operation = op;
    left = l;
    right = r;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    return left.hashCode() + operation.hashCode() + right.hashCode();
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof Assignment)) return false;
    Assignment a = (Assignment) o;
    return left.equals(a.left) && operation.equals(a.operation)
           && right.equals(a.right);
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(3);
    I.children[0] = right;
    I.children[1] = operation;
    I.children[2] = left;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new Assignment((Operator) operation.clone(),
                          (Expression) left.clone(),
                          (Expression) right.clone());
  }


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
    if (parenthesized) buffer.append("(");
    left.write(buffer);
    buffer.append(" ");
    operation.write(buffer);
    buffer.append(" ");
    right.write(buffer);
    if (parenthesized) buffer.append(")");
  }
}

