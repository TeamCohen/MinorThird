package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents an array access.
  *
  * @author Nick Rizzolo
 **/
public class SubscriptVariable extends VariableInstance
{
  /** (&not;&oslash;) The expression describing the array to be accessed. */
  public Expression array;
  /**
    * (&not;&oslash;) The expression whose evaluation will be used as the
    * subscript.
   **/
  public Expression subscript;


  /**
    * Initializing constructor.  Line and byte offset information is taken
    * from the first expression.
    *
    * @param ar   The expression describing the array to be accessed.
    * @param sub  The subscript expression.
   **/
  public SubscriptVariable(Expression ar, Expression sub) {
    super(ar.line, ar.byteOffset);
    array = ar;
    subscript = sub;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() { return array.hashCode() + subscript.hashCode(); }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof SubscriptVariable)) return false;
    SubscriptVariable s = (SubscriptVariable) o;
    return array.equals(s.array) && subscript.equals(s.subscript);
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
    I.children[0] = array;
    I.children[1] = subscript;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new SubscriptVariable((Expression) array.clone(),
                                 (Expression) subscript.clone());
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
    array.write(buffer);
    buffer.append("[");
    subscript.write(buffer);
    buffer.append("]");
    if (parenthesized) buffer.append(")");
  }
}

