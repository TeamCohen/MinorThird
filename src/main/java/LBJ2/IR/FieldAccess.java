package LBJ2.IR;

import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * This class represents a field access.
  *
  * @author Nick Rizzolo
 **/
public class FieldAccess extends VariableInstance
{
  /** (&not;&oslash;) The expression describing the object to be accessed. */
  public Expression object;
  /** (&not;&oslash;) The name of the field to be accessed. */
  public String name;


  /**
    * Parser's constructor.  Line and byte offset information is taken
    * from the name token.
    *
    * @param o  The expression describing the object to be accessed.
    * @param n  Token representing the name of the field to be accessed.
   **/
  public FieldAccess(Expression o, TokenValue n) {
    this(o, n.toString(), n.line, n.byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param o          The expression describing the object to be accessed.
    * @param n          The name of the field to be accessed.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public FieldAccess(Expression o, String n, int line, int byteOffset) {
    super(line, byteOffset);
    object = o;
    name = n;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() { return object.hashCode() + name.hashCode(); }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FieldAccess)) return false;
    FieldAccess f = (FieldAccess) o;
    return object.equals(f.object) && name.equals(f.name);
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
    I.children[0] = object;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new FieldAccess((Expression) object.clone(), name, -1, -1);
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
    object.write(buffer);
    buffer.append("." + name);
    if (parenthesized) buffer.append(")");
  }
}

