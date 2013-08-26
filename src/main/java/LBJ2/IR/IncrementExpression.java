package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents both increment and decrement expressions.  It's
  * functionally equivalent to the <code>UnaryExpression</code> class, except
  * that it's derived from <code>StatementExpression</code> instead of just
  * <code>Expression</code>, thereby allowing it to be part of an
  * <code>ExpressionStatement</code>.
  *
  * @see  UnaryExpression
  * @see  StatementExpression
  * @see  ExpressionStatement
  * @author Nick Rizzolo
 **/
public class IncrementExpression extends StatementExpression
{
  /** (&not;&oslash;) Representation of the increment operator. */
  public Operator operation;
  /**
    * (&not;&oslash;) The expression on which the increment operator operates.
   **/
  public Expression subexpression;


  /**
    * Initializing constructor.  Line and byte offset information is taken
    * from the increment operator's representation.
    *
    * @param op   Representation of the increment operator.
    * @param sub  The expression on which the increment operator operates.
   **/
  public IncrementExpression(Operator op, Expression sub) {
    super(op.line, op.byteOffset);
    operation = op;
    subexpression = sub;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    return operation.hashCode() + subexpression.hashCode();
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof IncrementExpression)) return false;
    IncrementExpression i = (IncrementExpression) o;
    return operation.equals(i.operation)
           && subexpression.equals(i.subexpression);
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(2);

    if (operation.operation == Operator.PRE_INCREMENT
        || operation.operation == Operator.PRE_DECREMENT) {
      I.children[0] = operation;
      I.children[1] = subexpression;
    }
    else {
      I.children[0] = subexpression;
      I.children[1] = operation;
    }

    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new IncrementExpression((Operator) operation.clone(),
                                   (Expression) subexpression.clone());
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

    if (operation.operation == Operator.PRE_INCREMENT
        || operation.operation == Operator.PRE_DECREMENT) {
      operation.write(buffer);
      subexpression.write(buffer);
    }
    else {
      subexpression.write(buffer);
      operation.write(buffer);
    }

    if (parenthesized) buffer.append(")");
  }
}

