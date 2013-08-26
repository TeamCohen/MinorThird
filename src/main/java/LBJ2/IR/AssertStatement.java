package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents an assertion statement.
  *
  * @author Nick Rizzolo
 **/
public class AssertStatement extends Statement
{
  /**
    * (&not;&oslash;) The condition that must hold; otherwise, an assertion
    * error is generated.
   **/
  public Expression condition;
  /**
    * (&oslash;) Represents the error message in the assertion error, if any.
   **/
  public Expression message;


  /**
    * Initializing constructor.
    *
    * @param c          The condition that must hold.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public AssertStatement(Expression c, int line, int byteOffset) {
    this(c, null, line, byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param c          The condition that must hold.
    * @param m          The error message.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public AssertStatement(Expression c, Expression m, int line, int byteOffset)
  {
    super(line, byteOffset);
    condition = c;
    message = m;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(message == null ? 1 : 2);
    I.children[0] = condition;
    if (message != null) I.children[1] = message;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new AssertStatement(
        (Expression) condition.clone(),
        (message == null ? null : (Expression) message.clone()), -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    int result = 31 * condition.hashCode();
    if (message != null) result += 7 * message.hashCode();
    return result;
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof AssertStatement)) return false;
    AssertStatement a = (AssertStatement) o;
    return
      condition.equals(a.condition)
      && (message == null ? a.message == null : message.equals(a.message));
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
    buffer.append("assert ");
    condition.write(buffer);
    if (message != null) {
      buffer.append(" : ");
      message.write(buffer);
    }
    buffer.append(";");
  }
}

