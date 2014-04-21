package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents a constraint expression involving a binary operator.
  *
  * @author Nick Rizzolo
 **/
public class BinaryConstraintExpression extends ConstraintExpression
{
  /** (&not;&oslash;) The binary operation. */
  public Operator operation;
  /** (&not;&oslash;) The left hand side of the binary expression. */
  public ConstraintExpression left;
  /** (&not;&oslash;) The right hand side of the binary expression. */
  public ConstraintExpression right;


  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the representation of the operator.
    *
    * @param op Reference to the operator's representation.
    * @param l  Reference to the left hand side's representation.
    * @param r  Reference to the right hand side's representation.
   **/
  public BinaryConstraintExpression(Operator op, ConstraintExpression l,
                                    ConstraintExpression r) {
    super(op.line, op.byteOffset);
    operation = op;
    left = l;
    right = r;
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
    I.children[0] = left;
    I.children[1] = operation;
    I.children[2] = right;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return
      new BinaryConstraintExpression((Operator) operation.clone(),
                                     (ConstraintExpression) left.clone(),
                                     (ConstraintExpression) right.clone());
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

