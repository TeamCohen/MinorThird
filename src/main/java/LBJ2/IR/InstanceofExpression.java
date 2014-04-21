package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents an <code>instanceof</code> expression.
  *
  * @author Nick Rizzolo
 **/
public class InstanceofExpression extends Expression
{
  /**
    * (&not;&oslash;) The expression on the left hand side of
    * <code>instanceof</code>.
   **/
  public Expression left;
  /**
    * (&not;&oslash;) The expression on the right hand side of
    * <code>instanceof</code>.
   **/
  public Type right;


  /**
    * Full constructor.
    *
    * @param l          Reference to the left hand side's representation.
    * @param r          Reference to the right hand side's representation.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public InstanceofExpression(Expression l, Type r, int line, int byteOffset)
  {
    super(line, byteOffset);
    left = l;
    right = r;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() { return left.hashCode() + right.hashCode(); }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof InstanceofExpression)) return false;
    InstanceofExpression i = (InstanceofExpression) o;
    return left.equals(i.left) && right.equals(i.right);
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
    I.children[0] = left;
    I.children[1] = right;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new InstanceofExpression((Expression) left.clone(),
                                    (Type) right.clone(), -1, -1);
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
    buffer.append(" instanceof ");
    right.write(buffer);
    if (parenthesized) buffer.append(")");
  }
}

