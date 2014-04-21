package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents those expressions that can be used to set all the values in an
  * array.
  *
  * @author Nick Rizzolo
 **/
public class ArrayInitializer extends Expression
{
  /**
    * (&not;&oslash;) The list of expressions that represent the values in the
    * array.
   **/
  public ExpressionList values;


  /** Default constructor. */
  public ArrayInitializer() { this(new ExpressionList(), -1, -1); }

  /**
    * Initializing constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ArrayInitializer(int line, int byteOffset) {
    this(new ExpressionList(), line, byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param v          The expressions that represent the values in the
    *                   array.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ArrayInitializer(ExpressionList v, int line, int byteOffset) {
    super(line, byteOffset);
    values = v;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() { return values.hashCode(); }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof ArrayInitializer)) return false;
    ArrayInitializer a = (ArrayInitializer) o;
    return values.equals(a.values);
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
    I.children[0] = values;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new ArrayInitializer((ExpressionList) values.clone(), -1, -1);
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
    buffer.append("{");
    values.write(buffer);
    buffer.append("}");
    if (parenthesized) buffer.append(")");
  }
}

