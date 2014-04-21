package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a switch statement.
  *
  * @author Nick Rizzolo
 **/
public class SwitchStatement extends Statement
{
  /**
    * (&not;&oslash;) The expression determining which statements to execute.
   **/
  public Expression expression;
  /**
    * (&not;&oslash;) The various code blocks that are executed depending on
    * the expression's value.
   **/
  public SwitchBlock block;


  /**
    * Full constructor.
    *
    * @param e          The expression determining which code to execute.
    * @param b          The code to execute.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public SwitchStatement(Expression e, SwitchBlock b, int line,
                         int byteOffset) {
    super(line, byteOffset);
    expression = e;
    block = b;
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
    I.children[0] = expression;
    I.children[1] = block;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new SwitchStatement((Expression) expression.clone(),
                               (SwitchBlock) block.clone(), -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return 31 * expression.hashCode() + 17 * block.hashCode();
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof SwitchStatement)) return false;
    SwitchStatement s = (SwitchStatement) o;
    return expression.equals(s.expression) && block.equals(s.block);
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
    buffer.append("switch (");
    expression.write(buffer);
    buffer.append(") ");
    block.write(buffer);
  }
}

