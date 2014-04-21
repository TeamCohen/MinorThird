package LBJ2.IR;

import java.util.LinkedList;
import LBJ2.Pass;


/**
  * Represents a for loop.
  *
  * @author Nick Rizzolo
 **/
public class ForStatement extends Statement
{
  /**
    * (&oslash;) The initializing expression(s) in the loop header (if any).
   **/
  public ExpressionList initializers;
  /** (&oslash;) The variable declaration in the loop header (if any). */
  public VariableDeclaration initializer;
  /**
    * (&oslash;) The expression representing the loop's terminating condition.
   **/
  public Expression condition;
  /** (&oslash;) The updating expression(s) in the loop header. */
  public ExpressionList updaters;
  /** (&not;&oslash;) The body of the loop. */
  public Statement body;


  /**
    * Full constructor.
    *
    * @param i          The initializers list.
    * @param c          The terminating condition.
    * @param u          The updaters list.
    * @param b          The body of the loop.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ForStatement(ExpressionList i, Expression c, ExpressionList u,
                      Statement b, int line, int byteOffset) {
    super(line, byteOffset);
    initializers = i;
    initializer = null;
    condition = c;
    updaters = u;
    body = b;
  }

  /**
    * Full constructor.
    *
    * @param v          The initializer variable declaration.
    * @param c          The terminating condition.
    * @param u          The updaters list.
    * @param b          The body of the loop.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ForStatement(VariableDeclaration v, Expression c, ExpressionList u,
                      Statement b, int line, int byteOffset) {
    this((ExpressionList) null, c, u, b, line, byteOffset);
    initializer = v;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    LinkedList children = new LinkedList();
    if (initializers != null) children.add(initializers);
    if (initializer != null) children.add(initializer);
    if (condition != null) children.add(condition);
    children.add(body);
    if (updaters != null) children.add(updaters);

    ASTNodeIterator I = new ASTNodeIterator();
    I.children = (ASTNode[]) children.toArray(new ASTNode[children.size()]);
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    ExpressionList i =
      initializers == null ? null : (ExpressionList) initializers.clone();
    VariableDeclaration v =
      initializer == null ? null : (VariableDeclaration) initializer.clone();
    Expression c = condition == null ? null : (Expression) condition.clone();
    ExpressionList u =
      updaters == null ? null : (ExpressionList) updaters.clone();

    if (v == null) return new ForStatement(i, c, u, body, -1, -1);
    return new ForStatement(v, c, u, body, -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    int result = 59 * body.hashCode();
    if (initializers != null) result += 47 * initializers.hashCode();
    if (initializer != null) result += 37 * initializer.hashCode();
    if (condition != null) result += 23 * condition.hashCode();
    if (updaters != null) result += 7 * updaters.hashCode();
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
    if (!(o instanceof ForStatement)) return false;
    ForStatement f = (ForStatement) o;
    return
      body.equals(f.body)
      && (initializers == null ? f.initializers == null
                               : initializers.equals(f.initializers))
      && (initializer == null ? f.initializer == null
                              : initializer.equals(f.initializer))
      && (condition == null ? f.condition == null
                            : condition.equals(f.condition))
      && (updaters == null ? f.updaters == null
                           : updaters.equals(f.updaters));
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
    buffer.append("for (");

    if (initializers != null) initializers.write(buffer);
    if (initializer != null) {
      initializer.write(buffer);
      buffer.append(" ");
    }
    else buffer.append("; ");

    if (condition != null) condition.write(buffer);
    buffer.append("; ");
    if (updaters != null) updaters.write(buffer);
    buffer.append(") ");
    body.write(buffer);
  }
}

