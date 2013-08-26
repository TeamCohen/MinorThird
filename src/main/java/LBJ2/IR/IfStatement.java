package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents an if statement.
  *
  * @author Nick Rizzolo
 **/
public class IfStatement extends Statement
{
  /**
    * (&not;&oslash;) The condition controlling execution of the
    * sub-statements.
   **/
  public Expression condition;
  /** (&not;&oslash;) The statement to execute if the condition is true. */
  public Statement thenClause;
  /**
    * (&oslash;) The statement to execute if the condition is false, if any.
   **/
  public Statement elseClause;


  /**
    * Initializing constructor.
    *
    * @param c          The condition controlling execution.
    * @param t          The statement to execute if the condition is true.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public IfStatement(Expression c, Statement t, int line, int byteOffset) {
    this(c, t, null, line, byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param c          The condition controlling execution.
    * @param t          The statement to execute if the condition is true.
    * @param e          The statement to execute if the condition is false, if
    *                   any.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public IfStatement(Expression c, Statement t, Statement e, int line,
                     int byteOffset) {
    super(line, byteOffset);
    condition = c;
    thenClause = t;
    elseClause = e;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(elseClause == null ? 2 : 3);
    I.children[0] = condition;
    I.children[1] = thenClause;
    if (elseClause != null) I.children[2] = elseClause;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new IfStatement(
        (Expression) condition.clone(),
        (Statement) thenClause.clone(),
        elseClause == null ? null : (Statement) elseClause.clone(), -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    int result = 47 * condition.hashCode() + 29 * thenClause.hashCode();
    if (elseClause != null) result += 17 * elseClause.hashCode();
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
    if (!(o instanceof IfStatement)) return false;
    IfStatement i = (IfStatement) o;
    return
      condition.equals(i.condition) && thenClause.equals(i.thenClause)
      && (elseClause == null ? i.elseClause == null
                             : elseClause.equals(i.elseClause));
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
    buffer.append("if (");
    condition.write(buffer);
    buffer.append(") ");
    thenClause.write(buffer);
    if (elseClause != null) {
      buffer.append(" else ");
      elseClause.write(buffer);
    }
  }
}

