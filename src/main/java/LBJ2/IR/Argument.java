package LBJ2.IR;

import LBJ2.Pass;


/**
  * An "argument" is the specification of a classifier's input parameter.
  *
  * @author Nick Rizzolo
 **/
public class Argument extends ASTNode
{
  /** Whether or not the argument was modified as final. */
  private boolean isFinal;
  /** (&not;&oslash;) The type of the argument. */
  private Type type;
  /** (&not;&oslash;) The name of the argument. */
  private String name;


  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the type's representation.
    *
    * @param t  Reference to the object representing the argument's type.
    * @param n  Reference to the object representing the argument's name.
   **/
  public Argument(Type t, String n) { this(t, n, false); }

  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the type's representation.
    *
    * @param t  Reference to the object representing the argument's type.
    * @param n  Reference to the object representing the argument's name.
    * @param f  Whether or not the argument was modified as final.
   **/
  public Argument(Type t, String n, boolean f) {
    super(t.line, t.byteOffset);
    type = t;
    name = n;
    isFinal = f;
  }


  /**
    * Retrieves the value of the <code>isFinal</code> member variable.
    *
    * @return The value stored in <code>isFinal</code>.
   **/
  public boolean getFinal() { return isFinal; }


  /**
    * Retrieves the type portion of the argument.
    *
    * @return The value stored in <code>type</code>.
   **/
  public Type getType() { return type; }


  /**
    * Retrieves the name portion of the argument.
    *
    * @return The value stored in <code>name</code>.
   **/
  public String getName() { return name; }


  /**
    * The hash code of an <code>Argument</code> is simply the hash code of its
    * name.
   **/
  public int hashCode() { return name.hashCode(); }


  /**
    * Two <code>Argument</code>s are equivalent when their names and types are
    * equivalent.
    *
    * @param o  The object with which this object is to be compared.
    * @return <code>true</code> iff the two objects are equivalent.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof Argument)) return false;
    Argument a = (Argument) o;
    return a.name.equals(name) && a.type.equals(type);
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
    I.children[0] = type;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new Argument((Type) type.clone(), name, isFinal);
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
    if (isFinal) buffer.append("final ");
    type.write(buffer);
    buffer.append(" " + name);
  }
}

