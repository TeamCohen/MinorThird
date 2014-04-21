package LBJ2.IR;

import java.util.HashSet;


/**
  * Resembling first order logic, a constraint expression consists of equality
  * (or inequality) tests and logical operators and evaluates to a Boolean
  * value.
  *
  * @author Nick Rizzolo
 **/
public abstract class ConstraintExpression extends ASTNode
{
  /** Indicates whether this expression was parenthesized in the source. */
  public boolean parenthesized = false;


  /**
    * Default constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  ConstraintExpression(int line, int byteOffset) { super(line, byteOffset); }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() {
    HashSet result = new HashSet();
    for (ASTNodeIterator I = iterator(); I.hasNext(); ) {
      ASTNode node = I.next();
      if (node instanceof ConstraintExpression)
        result.addAll(((ConstraintExpression) node).getVariableTypes());
    }

    return result;
  }


  /**
    * Determines if there are any quantified variables in this expression.
    * This method cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public boolean containsQuantifiedVariable() {
    for (ASTNodeIterator I = iterator(); I.hasNext(); ) {
      ASTNode node = I.next();
      if (node instanceof ConstraintExpression
          && ((ConstraintExpression) node).containsQuantifiedVariable())
        return true;
    }

    return false;
  }
}

