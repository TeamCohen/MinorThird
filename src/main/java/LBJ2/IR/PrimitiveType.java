package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a primitive type, as in a declaration.  In LBJ2, the legal
  * primitive types are boolean, char, byte, short, int, long, float, and
  * double.
  *
  * @author Nick Rizzolo
 **/
public class PrimitiveType extends Type
{
  /** Value of the <code>type</code> variable. */
  public  static final int BOOLEAN = 0;
  /** Value of the <code>type</code> variable. */
  public  static final int CHAR = 1;
  /** Value of the <code>type</code> variable. */
  public  static final int BYTE = 2;
  /** Value of the <code>type</code> variable. */
  public  static final int SHORT = 3;
  /** Value of the <code>type</code> variable. */
  public  static final int INT = 4;
  /** Value of the <code>type</code> variable. */
  public  static final int LONG = 5;
  /** Value of the <code>type</code> variable. */
  public  static final int FLOAT = 6;
  /** Value of the <code>type</code> variable. */
  public  static final int DOUBLE = 7;

  /**
    * <code>=
    * {
    *   "boolean", "char", "byte", "short", "int", "long", "float", "double"
    * }
    * </code>
   **/
  private static final String[] typeNames =
    {
      "boolean", "char", "byte", "short", "int", "long", "float", "double"
    };

  /**
    * Produces the name of the primitive type given its index.
    *
    * @param t  The index of the type.  (See the static member variables.)
    * @return A String holding the name of the type.
   **/
  public static String typeName(int t) { return typeNames[t]; }

  /**
    * <code>=
    * {
    *   Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE,
    *   Long.TYPE, Float.TYPE, Double.TYPE
    * }
    * </code>
   **/
  private static final Class[] classes =
    {
      Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE,
      Long.TYPE, Float.TYPE, Double.TYPE
    };


  /**
    * (&not;&oslash;) The index of the type represented by this
    * <code>PrimitiveType</code>.
   **/
  public int type;


  /**
    * Default constructor.  Line and byte offset information, having not been
    * supplied, is set to -1.
    *
    * @param t  The index of the primitive type.
   **/
  public PrimitiveType(int t) { this(t, -1, -1); }

  /**
    * Full constructor.
    *
    * @param t          The index of the primitive type.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public PrimitiveType(int t, int line, int byteOffset) {
    super(line, byteOffset);
    type = t;
  }


  /**
    * Returns an object representing the <code>class</code> that this type
    * represents.
    *
    * @return An object representing the <code>class</code> that this type
    *         represents.
   **/
  public Class typeClass() { return classes[type]; }


  /**
    * Determines whether this type represents a numerical value (including
    * <code>char</code>), as opposed to a boolean or <code>null</code>.
    *
    * @return <code>true</code> iff this <code>PrimitiveType</code> represents
    *         a numerical type.
   **/
  public boolean isNumber() { return type >= CHAR; }


  /**
    * Determines whether this type represents a whole number value (including
    * <code>char</code>), as opposed to a floating point, a boolean, or
    * <code>null</code>.
    *
    * @return <code>true</code> iff this <code>PrimitiveType</code> represents
    *         a whole number type.
   **/
  public boolean isWholeNumber() { return type >= CHAR && type <= LONG; }


  /**
    * Two <code>PrimitiveType</code>s are equivalent when their
    * <code>type</code> member variables are the same.
    *
    * @param o  The <code>Object</code> whose equality with this object needs
    *           to be tested.
    * @return <code>true</code> if the two <code>Object</code>s are equal, and
    *         <code>false</code> otherwise.
   **/
  public boolean equals(Object o) {
    return o instanceof PrimitiveType
           && type == ((PrimitiveType) o).type;
  }


  /** A hash code based on {@link #type}. */
  public int hashCode() { return type; }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() { return new ASTNodeIterator(0); }


  /**
    * Creates a new object with the same primitive data.
    *
    * @return The clone node.
   **/
  public Object clone() { return new PrimitiveType(type); }


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
  public void write(StringBuffer buffer) { buffer.append(typeName(type)); }
}

