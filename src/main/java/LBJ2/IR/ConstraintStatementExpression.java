package LBJ2.IR;

import java.util.HashSet;
import LBJ2.Pass;


/**
  * This class is simply a wrapper for a <code>ConstraintExpression</code> so
  * that it can be used in an <code>ExpressionStatement</code>.
  *
  * @author Nick Rizzolo
 **/
public class ConstraintStatementExpression extends StatementExpression
{
  /** (&not;&oslash;) The expression representing the constraint. */
  public ConstraintExpression constraint;


  /**
    * Full constructor.  Line and byte offset information is taken from the
    * lone argument.
    *
    * @param c  The expression representing a constraint.
   **/
  public ConstraintStatementExpression(ConstraintExpression c) {
    super(c.line, c.byteOffset);
    constraint = c;
  }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() { return constraint.getVariableTypes(); }


  /**
    * Determines if there are any quantified variables in this expression.
    * This method cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public boolean containsQuantifiedVariable() {
    return constraint.containsQuantifiedVariable();
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
      new ConstraintStatementExpression(
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
  public void write(StringBuffer buffer) { constraint.write(buffer); }
}

