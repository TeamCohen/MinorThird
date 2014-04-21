package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents an expression creating an array.
  *
  * @author Nick Rizzolo
 **/
public class ArrayCreationExpression extends Expression
{
  /**
    * (&not;&oslash;) The most basic type of elements in the array (i.e., it
    * will not be an <code>ArrayType</code>).
   **/
  public Type elementType;
  /**
    * (&not;&oslash;) Describes the size of each dimension in the new array.
   **/
  public ExpressionList sizes;
  /**
    * The total number of dimensions, including those for
    * which no size is given.
   **/
  public int dimensions;
  /** (&oslash;) Initial values for the new array. */
  public ArrayInitializer initializer;


  /**
    * Initializing constructor.
    *
    * @param t          The element type.
    * @param l          The list of dimension size expressions.
    * @param d          The total number of dimensions in the array.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ArrayCreationExpression(Type t, ExpressionList l, int d, int line,
                                 int byteOffset) {
    super(line, byteOffset);
    elementType = t;
    sizes = l;
    dimensions = d;
    initializer = null;
  }

  /**
    * Initializing constructor.
    *
    * @param t          The element type.
    * @param d          The total number of dimensions in the array.
    * @param a          An initializing expression for the array.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ArrayCreationExpression(Type t, int d, ArrayInitializer a, int line,
                                 int byteOffset) {
    this(t, new ExpressionList(), d, line, byteOffset);
    initializer = a;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    return elementType.hashCode() + sizes.hashCode()
           + (initializer == null ? 0 : initializer.hashCode());
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof ArrayCreationExpression)) return false;
    ArrayCreationExpression a = (ArrayCreationExpression) o;
    return elementType.equals(a.elementType) && sizes.equals(a.sizes)
           && (initializer == null && a.initializer == null
               || initializer != null && initializer.equals(a.initializer));
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(initializer == null ? 2 : 3);
    I.children[0] = elementType;
    I.children[1] = sizes;
    if (initializer != null) I.children[2] = initializer;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return initializer == null
      ? new ArrayCreationExpression((Type) elementType.clone(),
                                    (ExpressionList) sizes.clone(),
                                    dimensions, -1, -1)
      : new ArrayCreationExpression((Type) elementType.clone(),
                                    dimensions,
                                    (ArrayInitializer) initializer.clone(),
                                    -1, -1);
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
    buffer.append("new ");
    elementType.write(buffer);

    int i = 0;
    for (ASTNodeIterator I = sizes.iterator(); I.hasNext(); ++i) {
      buffer.append("[");
      I.next().write(buffer);
      buffer.append("]");
    }

    for (; i < dimensions; ++i) buffer.append("[]");
    if (initializer != null) initializer.write(buffer);
    if (parenthesized) buffer.append(")");
  }
}

