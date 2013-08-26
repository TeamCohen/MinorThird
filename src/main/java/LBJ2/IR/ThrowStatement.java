package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a throw statement.
  *
  * @author Nick Rizzolo
 **/
public class ThrowStatement extends Statement
{
  /** (&not;&oslash;) The expression representing the exception to throw. */
  public Expression exception;


  /**
    * Full constructor.
    *
    * @param e          The expression representing the exception to throw.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ThrowStatement(Expression e, int line, int byteOffset) {
    super(line, byteOffset);
    exception = e;
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
    I.children[0] = exception;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new ThrowStatement((Expression) exception.clone(), -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return 31 * exception.hashCode();
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof ThrowStatement)) return false;
    ThrowStatement t = (ThrowStatement) o;
    return exception.equals(t.exception);
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
    buffer.append("throw ");
    exception.write(buffer);
    buffer.append(";");
  }
}

