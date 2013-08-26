package LBJ2.IR;

import java.util.HashSet;
import LBJ2.Pass;


/**
  * This class represents the atom of the LBJ constraint expression: the
  * (in)equality comparison.  The application of a learning classifier to an
  * example object is here compared to either another learning classifier
  * application or to an arbitrary Java expression evaluating to a
  * <code>String</code>.
  *
  * @author Nick Rizzolo
 **/
public class ConstraintEqualityExpression extends ConstraintExpression
{
  /**
    * (&not;&oslash;) Represents either an equality or an inequality
    * comparison.
   **/
  public Operator operation;
  /** (&not;&oslash;) The expression on the left hand side of the operator. */
  public Expression left;
  /**
    * Filled in by <code>SemanticAnalysis</code>, this flag is set if
    * <code>left</code> represents the invocation of a discrete learner.
   **/
  public boolean leftIsDiscreteLearner;
  /**
    * Filled in by <code>SemanticAnalysis</code>, this flag is set if
    * <code>left</code> contains any quantified variables.
   **/
  public boolean leftIsQuantified;
  /**
    * (&not;&oslash;) The expression on the right hand side of the operator.
   **/
  public Expression right;
  /**
    * Filled in by <code>SemanticAnalysis</code>, this flag is set if
    * <code>right</code> represents the invocation of a discrete learner.
   **/
  public boolean rightIsDiscreteLearner;
  /**
    * Filled in by <code>SemanticAnalysis</code>, this flag is set if
    * <code>right</code> contains any quantified variables.
   **/
  public boolean rightIsQuantified;


  /**
    * Full constructor.  Line and byte offset information are taken from the
    * operator.
    *
    * @param o  The equality comparison operator.
    * @param l  The expression on the left of the operator.
    * @param r  The expression on the right of the operator.
   **/
  public ConstraintEqualityExpression(Operator o, Expression l, Expression r)
  {
    super(o.line, o.byteOffset);
    operation = o;
    left = l;
    right = r;
  }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() {
    HashSet result = left.getVariableTypes();
    result.addAll(right.getVariableTypes());
    return result;
  }


  /**
    * Determines if there are any quantified variables in this expression.
    * This method cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public boolean containsQuantifiedVariable() {
    return left.containsQuantifiedVariable()
           || right.containsQuantifiedVariable();
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
    I.children[0] = left;
    I.children[1] = operation;
    I.children[2] = right;
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
      new ConstraintEqualityExpression((Operator) operation.clone(),
                                       (Expression) left.clone(),
                                       (Expression) right.clone());
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
    left.write(buffer);
    buffer.append(" ");
    operation.write(buffer);
    buffer.append(" ");
    right.write(buffer);
  }
}

