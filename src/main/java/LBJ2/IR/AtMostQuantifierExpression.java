package LBJ2.IR;

import java.util.HashSet;
import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * An "at most" quantifier has the form:
  * <blockquote>
  *   <code>atmost <i>expression</i> of <i>argument</i> in
  *   (<i>expression</i>) <i>constraint-expression</i>
  * </blockquote>
  * where the first <code><i>expression</i></code> must evaluate to an
  * <code>int</code>, the second <code><i>expression</i></code> must evaluate
  * to a <code>Collection</code>, and the "at most" quantifier expression is
  * sastisfied iff when taking settings of <code><i>argument</i></code> from
  * the <code>Collection</code>, <code><i>constraint-expression</i></code> is
  * satisfied at most as many times as the integer the first
  * <code><i>expression</i></code> evaluates to.
  *
  * @author Nick Rizzolo
 **/
public class AtMostQuantifierExpression
       extends QuantifiedConstraintExpression
{
  /**
    * (&not;&oslash;) This expression evaluates to an integer representing the
    * maximum number of objects that must satisfy the child constraint
    * expression in order for this quantified constraint expression to be
    * satisfied.
   **/
  public Expression upperBound;
  /**
    * Filled in by <code>SemanticAnalysis</code>, this flag is set if
    * <code>upperBound</code> contains any quantified variables.
   **/
  public boolean upperBoundIsQuantified;


  /**
    * Full constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param ub         The upper bound expression.
    * @param a          The quantification variable specification.
    * @param c          Evaluates to the collection of objects.
    * @param co         The quantified constraint.
   **/
  public AtMostQuantifierExpression(int line, int byteOffset,
                                    Expression ub, Argument a, Expression c,
                                    ConstraintExpression co) {
    super(line, byteOffset, a, c, co);
    upperBound = ub;
  }

  /**
    * Parser's constructor.  Line and byte offset information are taken from
    * the token.
    *
    * @param t  The token containing line and byte offset information.
    * @param ub The upper bound expression.
    * @param a  The quantification variable specification.
    * @param c  Evaluates to the collection of objects.
    * @param co The quantified constraint.
   **/
  public AtMostQuantifierExpression(TokenValue t, Expression ub, Argument a,
                                    Expression c, ConstraintExpression co) {
    this(t.line, t.byteOffset, ub, a, c, co);
  }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() {
    HashSet result = super.getVariableTypes();
    result.addAll(upperBound.getVariableTypes());
    return result;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(4);
    I.children[0] = upperBound;
    I.children[1] = argument;
    I.children[2] = collection;
    I.children[3] = constraint;
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
      new AtMostQuantifierExpression(
          -1, -1, (Expression) upperBound.clone(),
          (Argument) argument.clone(),
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
    buffer.append("atmost ");
    upperBound.write(buffer);
    buffer.append(" of ");
    argument.write(buffer);
    buffer.append(" in (");
    collection.write(buffer);
    buffer.append(") ");
    constraint.write(buffer);
  }
}

