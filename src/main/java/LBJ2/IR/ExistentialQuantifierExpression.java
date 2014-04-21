package LBJ2.IR;

import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * An existential quantifier has the form:
  * <blockquote>
  *   <code>exists <i>argument</i> in (<i>expression</i>)
  *   <i>constraint-expression</i>
  * </blockquote>
  * where <code><i>expression</i></code> must evaluate to a
  * <code>Collection</code>, and the existential quantifier expression is
  * sastisfied iff <code><i>constraint-expression</i></code> is satisfied for
  * any setting of <code><i>argument</i></code> taken from the
  * <code>Collection</code>.
  *
  * @author Nick Rizzolo
 **/
public class ExistentialQuantifierExpression
       extends QuantifiedConstraintExpression
{
  /**
    * Full constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param a          The quantification variable specification.
    * @param c          Evaluates to the collection of objects.
    * @param co         The quantified constraint.
   **/
  public ExistentialQuantifierExpression(int line, int byteOffset, Argument a,
                                         Expression c,
                                         ConstraintExpression co) {
    super(line, byteOffset, a, c, co);
  }

  /**
    * Parser's constructor.  Line and byte offset information are taken from
    * the token.
    *
    * @param t  The token containing line and byte offset information.
    * @param a  The quantification variable specification.
    * @param c  Evaluates to the collection of objects.
    * @param co The quantified constraint.
   **/
  public ExistentialQuantifierExpression(TokenValue t, Argument a,
                                         Expression c,
                                         ConstraintExpression co) {
    this(t.line, t.byteOffset, a, c, co);
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return
      new ExistentialQuantifierExpression(
          -1, -1, (Argument) argument.clone(),
          (Expression) collection.clone(),
          (ConstraintExpression) constraint.clone());
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
    buffer.append("exists ");
    argument.write(buffer);
    buffer.append(" in (");
    collection.write(buffer);
    buffer.append(") ");
    constraint.write(buffer);
  }
}

