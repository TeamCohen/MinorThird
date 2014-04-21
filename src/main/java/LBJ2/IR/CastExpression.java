package LBJ2.IR;

import LBJ2.Pass;


/**
  * Representation of an expression that casts a value to another type.
  *
  * @author Nick Rizzolo
 **/
public class CastExpression extends Expression
{
  /** (&not;&oslash;) The type to cast to. */
  public Type type;
  /** (&not;&oslash;) The expression whose value should be casted. */
  public Expression expression;


  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the type's representation.
    *
    * @param t  Reference to the object representing the cast's type.
    * @param e  Reference to the object representing the expression to cast.
   **/
  public CastExpression(Type t, Expression e) {
    super(t.line, t.byteOffset);
    type = t;
    expression = e;
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
    I.children[0] = type;
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
    return
      new CastExpression((Type) type.clone(),
                         (Expression) expression.clone());
  }


  /** Determines if this object is equivalent to another object. */
  public boolean equals(Object o) {
    if (!(o instanceof CastExpression)) return false;
    CastExpression c = (CastExpression) o;
    return type.equals(c.type) && expression.equals(c.expression);
  }


  /**
    * A hash code based on the hash codes of {@link #type} and
    * {@link #expression}.
   **/
  public int hashCode() {
    return 31 * type.hashCode() + 17 * expression.hashCode();
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
    type.write(buffer);
    buffer.append(") ");
    expression.write(buffer);
    if (parenthesized) buffer.append(")");
  }
}

