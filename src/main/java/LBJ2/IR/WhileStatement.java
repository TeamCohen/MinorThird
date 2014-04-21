package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a while loop.
  *
  * @author Nick Rizzolo
 **/
public class WhileStatement extends Statement
{
  /**
    * (&not;&oslash;) The expression representing the loop's terminating
    * condition.
   **/
  public Expression condition;
  /** (&not;&oslash;) The body of the loop. */
  public Statement body;


  /**
    * Full constructor.
    *
    * @param c          The terminating condition.
    * @param b          The body of the loop.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public WhileStatement(Expression c, Statement b, int line, int byteOffset) {
    super(line, byteOffset);
    condition = c;
    body = b;
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
    I.children[0] = condition;
    I.children[1] = body;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new WhileStatement((Expression) condition.clone(),
                              (Statement) body.clone(), -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return 31 * condition.hashCode() + 17 * body.hashCode();
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof WhileStatement)) return false;
    WhileStatement w = (WhileStatement) o;
    return condition.equals(w.condition) && body.equals(w.body);
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
    buffer.append("while (");
    condition.write(buffer);
    buffer.append(") ");
    body.write(buffer);
  }
}

