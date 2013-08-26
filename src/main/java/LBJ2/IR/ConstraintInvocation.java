package LBJ2.IR;

import java.util.HashSet;
import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * A constraint may be invoked from within another constraint using the
  * <code>&#64;</code> operator.  This class is essentially a
  * <code>ConstraintExpression</code> wrapper for a
  * <code>MethodInvocation</code>.
  *
  * @author Nick Rizzolo
 **/
public class ConstraintInvocation extends ConstraintExpression
{
  /** (&not;&oslash;) The invocation. */
  public MethodInvocation invocation;
  /**
    * Filled in by <code>SemanticAnalysis</code>, this flag is set if
    * <code>invocation</code> contains any quantified variables.
   **/
  public boolean invocationIsQuantified;


  /**
    * Full constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param m          The invocation.
   **/
  public ConstraintInvocation(int line, int byteOffset, MethodInvocation m) {
    super(line, byteOffset);
    invocation = m;
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the token.
    *
    * @param t  The token providing line and byte offset information.
    * @param m  The invocation.
   **/
  public ConstraintInvocation(TokenValue t, MethodInvocation m) {
    this(t.line, t.byteOffset, m);
  }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() { return invocation.getVariableTypes(); }


  /**
    * Determines if there are any quantified variables in this expression.
    * This method cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public boolean containsQuantifiedVariable() {
    return invocation.containsQuantifiedVariable();
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
    I.children[0] = invocation;
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
      new ConstraintInvocation(-1, -1, (MethodInvocation) invocation.clone());
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
    buffer.append("@");
    invocation.write(buffer);
  }
}

