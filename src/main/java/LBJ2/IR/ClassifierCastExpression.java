package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents a classifier cast expression.
  *
  * @author Nick Rizzolo
 **/
public class ClassifierCastExpression extends ClassifierExpression
{
  /** (&not;&oslash;) The return type used to cast. */
  public ClassifierReturnType castType;
  /** (&not;&oslash;) The expression being casted. */
  public ClassifierExpression expression;


  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the type.
    *
    * @param t  The return type used to cast.
    * @param e  The expression being casted.
   **/
  public ClassifierCastExpression(ClassifierReturnType t,
                                  ClassifierExpression e) {
    super(t.line, t.byteOffset);
    castType = t;
    expression = e;
  }


  /**
    * Sets the <code>cacheIn</code> member variable to the argument.
    *
    * @param c  The new expression for the <code>cacheIn</code> member
    *           variable.
   **/
  public void setCacheIn(Name c) { expression.setCacheIn(c); }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    return castType.hashCode() + expression.hashCode();
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof ClassifierCastExpression)) return false;
    ClassifierCastExpression c = (ClassifierCastExpression) o;
    return castType.equals(c.castType) && expression.equals(c.expression);
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(2);
    I.children[0] = castType;
    I.children[1] = expression;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new ClassifierCastExpression(
        (ClassifierReturnType) castType.clone(),
        (ClassifierExpression) expression.clone());
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
    buffer.append("(");
    castType.write(buffer);
    buffer.append(") ");
    expression.write(buffer);
    if (parenthesized) buffer.append(")");
  }


  /**
    * Creates a <code>StringBuffer</code> containing a shallow representation
    * of this <code>ASTNode</code>.
    *
    * @return A <code>StringBuffer</code> containing a shallow text
    *         representation of the given node.
   **/
  public StringBuffer shallow() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("(");
    castType.write(buffer);
    buffer.append(") " + expression.name);
    return buffer;
  }
}

