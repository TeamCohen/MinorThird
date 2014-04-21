package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a while loop.
  *
  * @author Nick Rizzolo
 **/
public class DoStatement extends WhileStatement
{
  /**
    * Full constructor.
    *
    * @param b          The body of the loop.
    * @param c          The terminating condition.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public DoStatement(Statement b, Expression c, int line, int byteOffset) {
    super(c, b, line, byteOffset);
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
    I.children[0] = body;
    I.children[1] = condition;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new DoStatement((Statement) body.clone(),
                           (Expression) condition.clone(), -1, -1);
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
    buffer.append("do ");
    body.write(buffer);
    buffer.append(" while (");
    condition.write(buffer);
    buffer.append(");");
  }
}

