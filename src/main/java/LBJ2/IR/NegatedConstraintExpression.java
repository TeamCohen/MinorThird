package LBJ2.IR;

import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * Represents the negation of a constraint expression.
  *
  * @author Nick Rizzolo
 **/
public class NegatedConstraintExpression extends ConstraintExpression
{
  /** (&not;&oslash;) The constraint being negated. */
  public ConstraintExpression constraint;


  /**
    * Full constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param c          The constraint being negated.
   **/
  public NegatedConstraintExpression(int line, int byteOffset,
                                     ConstraintExpression c) {
    super(line, byteOffset);
    constraint = c;
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the token.
    *
    * @param t  The token providing line and byte offset information.
    * @param c  The constraint being negated.
   **/
  public NegatedConstraintExpression(TokenValue t, ConstraintExpression c) {
    this(t.line, t.byteOffset, c);
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
    I.children[0] = constraint;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return
      new NegatedConstraintExpression(
          -1, -1, (ConstraintExpression) constraint.clone());
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
    buffer.append("!(");
    constraint.write(buffer);
    buffer.append(") ");
  }
}

