package LBJ2.IR;

import java.util.HashSet;
import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * This class represents a method call.
  *
  * @author Nick Rizzolo
 **/
public class MethodInvocation extends StatementExpression
{
  /**
    * (&oslash;) This expression evaluates to the object whose method will be
    * called.
   **/
  public Expression parentObject;
  /** (&not;&oslash;) The name of the method to be invoked. */
  public Name name;
  /** (&not;&oslash;) The argument expressions passed to the method. */
  public ExpressionList arguments;
  /**
    * Filled in by the <code>SemanticAnalysis</code> pass, this variable is
    * set to <code>true</code> iff this invocation represents a classifier
    * invocation.
    *
    * @see LBJ2.SemanticAnalysis
   **/
  public boolean isClassifierInvocation;
  /**
    * Filled in by the <code>SemanticAnalysis</code> pass, this variable is
    * set to <code>true</code> iff this invocation is the argument of a
    * learning classifier expression's evaluate clause.
    *
    * @see LBJ2.SemanticAnalysis
   **/
  public boolean isEvaluateArgument;
  /**
    * The <code>SemanticAnalysis</code> pass will let this
    * <code>MethodInvocation</code> know if it is the immediate
    * <code>value</code> child of a <code>SenseStatement</code> by setting
    * this flag.
    *
    * @see LBJ2.SemanticAnalysis
   **/
  public boolean isSensedValue;


  /**
    * Initializing constructor.  Line and byte offset information is taken
    * from the representation of the name.
    *
    * @param n  The name of the method being invoked.
   **/
  public MethodInvocation(Name n) { this(n, new ExpressionList()); }

  /**
    * Initializing constructor.  Line and byte offset information is taken
    * from the representation of the name.
    *
    * @param n  The name of the method being invoked.
    * @param a  The argument expressions passed to the method.
   **/
  public MethodInvocation(Name n, ExpressionList a) {
    this(null, n, a, n.line, n.byteOffset);
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the representation of the name.
    *
    * @param p  Represents the object whose method is being invoked.
    * @param n  Token representing the name of the method being invoked.
   **/
  public MethodInvocation(Expression p, TokenValue n) {
    this(p, n, new ExpressionList());
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the representation of the name.
    *
    * @param p  Represents the object whose method is being invoked.
    * @param n  Token representing the name of the method being invoked.
    * @param a  The argument expressions passed to the method.
   **/
  public MethodInvocation(Expression p, TokenValue n, ExpressionList a) {
    this(p, new Name(n), a, n.line, n.byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param p          Represents the object whose method is being invoked.
    * @param n          The name of the method being invoked.
    * @param a          The argument expressions passed to the method.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public MethodInvocation(Expression p, Name n, ExpressionList a, int line,
                          int byteOffset) {
    super(line, byteOffset);
    parentObject = p;
    name = n;
    arguments = a;
    isClassifierInvocation = isEvaluateArgument = isSensedValue = false;
  }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() {
    HashSet result = new HashSet();
    if (parentObject != null) result.addAll(parentObject.getVariableTypes());
    result.addAll(name.getVariableTypes(true));
    result.addAll(arguments.getVariableTypes());
    return result;
  }


  /**
    * Determines if there are any quantified variables in this expression.
    * This method cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public boolean containsQuantifiedVariable() {
    return parentObject != null && parentObject.containsQuantifiedVariable()
           || name.containsQuantifiedVariable(true)
           || arguments.containsQuantifiedVariable();
  }


  /** Sets the <code>isSensedValue</code> flag. */
  public void senseValueChild() { isSensedValue = true; }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    int result = 31 * name.hashCode() + 17 * arguments.hashCode();
    if (parentObject != null) result += 7 * parentObject.hashCode();
    return result;
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof MethodInvocation)) return false;
    MethodInvocation i = (MethodInvocation) o;
    return (parentObject == null ? i.parentObject == null
                                 : parentObject.equals(i.parentObject))
           && name.equals(i.name) && arguments.equals(i.arguments);
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(parentObject == null ? 2 : 3);
    if (parentObject != null) I.children[0] = parentObject;
    I.children[I.children.length - 2] = name;
    I.children[I.children.length - 1] = arguments;
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
      new MethodInvocation(
          (parentObject == null) ? null : (Expression) parentObject.clone(),
          (Name) name.clone(), (ExpressionList) arguments.clone(), -1, -1);
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

    if (parentObject != null) {
      parentObject.write(buffer);
      buffer.append(".");
    }

    name.write(buffer);
    buffer.append("(");
    arguments.write(buffer);
    buffer.append(")");

    if (parenthesized) buffer.append(")");
  }
}

