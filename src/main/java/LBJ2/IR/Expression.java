package LBJ2.IR;

import java.util.HashSet;


/**
  * Abstract expression class.
  *
  * @author Nick Rizzolo
 **/
public abstract class Expression extends ASTNode
{
  /**
    * The <code>SemanticAnalysis</code> pass will store the type of this
    * expression here.
   **/
  public Type typeCache = null;
  /**
    * Indicates whether the <code>typeCache</code> variable contains usable
    * information.
   **/
  public boolean typeCacheFilled = false;
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
  Expression(int line, int byteOffset) { super(line, byteOffset); }


  /**
    * Supports the <code>SemanticAnalysis</code> pass which needs to notify
    * <code>MethodInvocation</code>s that are the immediate <code>value</code>
    * child of a <code>SenseStatement</code> that it's allowable to invoke an
    * array or generator classifier.  Only <code>MethodInvocation</code> will
    * need to override this method which does nothing by default.
    *
    * @see LBJ2.SemanticAnalysis
   **/
  public void senseValueChild() { }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() {
    HashSet result = new HashSet();
    for (ASTNodeIterator I = iterator(); I.hasNext(); ) {
      ASTNode node = I.next();
      if (node instanceof Expression)
        result.addAll(((Expression) node).getVariableTypes());
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
      if (node instanceof Expression
          && ((Expression) node).containsQuantifiedVariable())
        return true;
    }

    return false;
  }
}

