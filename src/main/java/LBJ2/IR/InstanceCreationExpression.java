package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents an expression creating a class instance.
  *
  * @author Nick Rizzolo
 **/
public class InstanceCreationExpression extends MethodInvocation
{
  /**
    * Initializing constructor.
    *
    * @param n          The name of the class or inner class to instantiate.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public InstanceCreationExpression(Name n, int line, int byteOffset) {
    this(null, n, new ExpressionList(), line, byteOffset);
  }

  /**
    * Initializing constructor.
    *
    * @param n          The name of the class or inner class to instantiate.
    * @param a          The argument expressions passed to the constructor.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public InstanceCreationExpression(Name n, ExpressionList a, int line,
                                    int byteOffset) {
    this(null, n, a, line, byteOffset);
  }

  /**
    * Initializing constructor.
    *
    * @param p          Represents the parent object containing an inner
    *                   class.
    * @param n          The name of the class or inner class to instantiate.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public InstanceCreationExpression(Expression p, Name n, int line,
                                    int byteOffset) {
    this(p, n, new ExpressionList(), line, byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param p          Represents the parent object containing an inner
    *                   class.
    * @param n          The name of the class or inner class to instantiate.
    * @param a          The argument expressions passed to the constructor.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public InstanceCreationExpression(Expression p, Name n, ExpressionList a,
                                    int line, int byteOffset) {
    super(p, n, a, line, byteOffset);
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof InstanceCreationExpression)) return false;
    return super.equals(o);
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return
      new InstanceCreationExpression(
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

    buffer.append("new ");
    name.write(buffer);
    buffer.append("(");
    arguments.write(buffer);
    buffer.append(")");

    if (parenthesized) buffer.append(")");
  }
}

