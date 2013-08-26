package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents a conditional expression.  Conditional expressions
  * have the form <code>(c ? t : e)</code>.
  *
  * @author Nick Rizzolo
 **/
public class Conditional extends Expression
{
  /** (&not;&oslash;) The condition of the conditional expression. */
  public Expression condition;
  /**
    * (&not;&oslash;) The expression to evaluate if the condition evaluates to
    * <code>true</code>.
   **/
  public Expression thenClause;
  /**
    * (&not;&oslash;) The expression to evaluate if the condition evaluates to
    * <code>false</code>.
   **/
  public Expression elseClause;


  /**
    * Full constructor.
    *
    * @param c          The condition.
    * @param t          The expression to evaluate if the condition is
    *                   <code>true</code>.
    * @param e          The expression to evaluate if the condition is
    *                   <code>false</code>.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public Conditional(Expression c, Expression t, Expression e, int line,
                     int byteOffset) {
    super(line, byteOffset);
    condition = c;
    thenClause = t;
    elseClause = e;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    return condition.hashCode() + thenClause.hashCode()
           + elseClause.hashCode();
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof Conditional)) return false;
    Conditional c = (Conditional) o;
    return condition.equals(c.condition) && thenClause.equals(c.thenClause)
           && elseClause.equals(c.elseClause);
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
    I.children[0] = condition;
    I.children[1] = thenClause;
    I.children[2] = elseClause;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new Conditional((Expression) condition.clone(),
                           (Expression) thenClause.clone(),
                           (Expression) elseClause.clone(), -1, -1);
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
    condition.write(buffer);
    buffer.append(" ? ");
    thenClause.write(buffer);
    buffer.append(" : ");
    elseClause.write(buffer);
    if (parenthesized) buffer.append(")");
  }
}

