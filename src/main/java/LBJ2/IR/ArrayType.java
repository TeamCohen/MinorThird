package LBJ2.IR;

import LBJ2.Pass;
import java.lang.reflect.Array;


/**
  * Class for representing array types.
  *
  * @author Nick Rizzolo
 **/
public class ArrayType extends Type
{
  /** (&not;&oslash;) Represents the type of each element in the array. */
  public Type type;


  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the argument.
    *
    * @param t  The type of each element in the array.
   */
  public ArrayType(Type t) {
    super(t.line, t.byteOffset);
    type = t;
  }


  /**
    * Returns an object representing the <code>class</code> that this type
    * represents.
    *
    * @return An object representing the <code>class</code> that this type
    *         represents.
   **/
  public Class typeClass() {
    if (myClass == null) {
      myClass = type.typeClass();
      if (myClass != null) {
        try { myClass = Array.newInstance(myClass, 0).getClass(); }
        catch (Exception e) {
          System.err.println("Could not get the class for an array with "
                             + "element type '" + type + "'.  Aborting...");
          System.exit(1);
        }
      }
    }

    return myClass;
  }


  /**
    * Two <code>ArrayType</code>s are equivalent if their child types are
    * equivalent.
    *
    * @param t  The <code>Type</code> whose equality with this object needs to
    *           be tested.
    * @return <code>true</code> if the two <code>Type</code>s are equal, and
    *         <code>false</code> otherwise.
   **/
  public boolean equals(Object t) {
    return t instanceof ArrayType && type.equals(((ArrayType) t).type);
  }


  /** A hash code based on the hash code of {@link #type}. */
  public int hashCode() {
    return 31 * type.hashCode() + 17;
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
  public Object clone() { return new ArrayType((Type) type.clone()); }


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
    type.write(buffer);
    buffer.append("[]");
  }
}

