package LBJ2.IR;

import java.util.HashSet;
import LBJ2.Pass;


/**
  * Represents the return type of a hard-coded classifier.
  *
  * @author Nick Rizzolo
 **/
public class ClassifierReturnType extends Type
{
  /** Value of the <code>type</code> variable. */
  public static final int DISCRETE = 0;
  /** Value of the <code>type</code> variable. */
  public static final int REAL = 1;
  /** Value of the <code>type</code> variable. */
  public static final int MIXED = 2;
  /** Value of the <code>type</code> variable. */
  public static final int DISCRETE_ARRAY = 3;
  /** Value of the <code>type</code> variable. */
  public static final int REAL_ARRAY = 4;
  /** Value of the <code>type</code> variable. */
  public static final int MIXED_ARRAY = 5;
  /** Value of the <code>type</code> variable. */
  public static final int DISCRETE_GENERATOR = 6;
  /** Value of the <code>type</code> variable. */
  public static final int REAL_GENERATOR = 7;
  /** Value of the <code>type</code> variable. */
  public static final int MIXED_GENERATOR = 8;

  /**
    * <code>
    * = { "discrete", "real", "mixed", "discrete[]", "real[]", "mixed[]",
    * "discrete%", "real%", "mixed%" }
    * </code>
   **/
  private static final String[] typeNames =
    {
      "discrete", "real", "mixed", "discrete[]", "real[]", "mixed[]",
      "discrete%", "real%", "mixed%"
    };

  /**
    * Produces the name of the primitive type given its index.
    *
    * @param t  The index of the type.  (See the static member variables.)
    * @return A String holding the name of the type.
   **/
  public static String typeName(int t) { return typeNames[t]; }

  /** <code>= { String.class, Double.TYPE }</code> */
  private static final Class[] classes = { String.class, Double.TYPE };


  /**
    * The index of the type represented by this
    * <code>ClassifierReturnType</code>.
   **/
  public int type;
  /**
    * (&not;&oslash;) If the type is DISCRETE, this variable represents a list
    * of legal values.
   **/
  public ConstantList values;


  /**
    * This constructor parses the name of a classifier return type as it would
    * appear in the source, assuming value lists have been omitted.
    *
    * @param s  String representing the type's name.
   **/
  public ClassifierReturnType(String s) { this(s, new ConstantList()); }

  /**
    * This constructor parses the name of a classifier return type as it would
    * appear in the source, assuming value lists have been omitted.
    *
    * @param s  String representing the type's name.
    * @param l  The list of legal values.
   **/
  public ClassifierReturnType(String s, ConstantList l) {
    super(-1, -1);
    values = l;
    type = 0;
    while (type < typeNames.length && !s.equals(typeNames[type])) ++type;
    assert type < typeNames.length : "Couldn't find type name: " + s;
  }

  /**
    * Default constructor.  Line and byte offset information, having not been
    * supplied, are set to -1.
    *
    * @param t  The index of the primitive type.
   **/
  public ClassifierReturnType(int t) { this(t, new ConstantList()); }

  /**
    * Default constructor.  Line and byte offset information, having not been
    * supplied, are set to -1.
    *
    * @param t  The index of the primitive type.
    * @param l  The list of legal values.
   **/
  public ClassifierReturnType(int t, ConstantList l) { this(t, l, -1, -1); }

  /**
    * Initializing constructor.
    *
    * @param t          The index of the primitive type.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ClassifierReturnType(int t, int line, int byteOffset) {
    this(t, new ConstantList(), line, byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param t          The index of the primitive type.
    * @param l          The list of legal values.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ClassifierReturnType(int t, ConstantList l, int line, int byteOffset)
  {
    super(line, byteOffset);
    type = t;
    values = l;
  }


  /**
    * Retrieves the name of the base type represented by this object.
    *
    * @return The name of the base type represented by this object.
   **/
  public String getTypeName() { return typeName(type); }


  /**
    * Determines whether the feature(s) returned by a classifier of this type
    * can become part or all of the features returned by a classifier of the
    * specified type.
    *
    * @param crt  The type of the classifier to which features are
    *             hypothetically being added.
    * @return <code>true</code> iff this type is more specific than the
    *         specified type.
   **/
  public boolean isContainableIn(ClassifierReturnType crt) {
    if (crt.values.size() > 0) {
      if (values.size() == 0 || values.size() > crt.values.size())
        return false;

      HashSet constants = new HashSet();
      for (ASTNodeIterator I = crt.values.iterator(); I.hasNext(); )
        constants.add(I.next());

      if (!constants.contains(new Constant("*"))) {
        for (ConstantList.ConstantListIterator I = values.listIterator();
            I.hasNext(); ) {
          Constant c = I.nextItem();
          if (!c.value.equals("*") && !constants.contains(c)) return false;
        }
      }
    }

    return type == crt.type || crt.type == MIXED_GENERATOR
      || type == DISCRETE
         && (crt.type == DISCRETE_ARRAY || crt.type == DISCRETE_GENERATOR)
      || type == DISCRETE_ARRAY && crt.type == DISCRETE_GENERATOR
      || type == REAL
         && (crt.type == REAL_ARRAY || crt.type == REAL_GENERATOR)
      || type == REAL_ARRAY && crt.type == REAL_GENERATOR;
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
    * Determines whether the argument is equal to this object.
    *
    * @param o  The <code>Object</code> whose equality with this object needs
    *           to be tested.
    * @return <code>true</code> if the two <code>Object</code>s are equal, and
    *         <code>false</code> otherwise.
   **/
  public boolean equals(Object o) {
    return o instanceof ClassifierReturnType
           && type == ((ClassifierReturnType) o).type;
  }


  /** A hash code based on {@link #type}. */
  public int hashCode() {
    return 31 * type + 17;
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
    * Creates a new object with the same primitive data.
    *
    * @return The clone node.
   **/
  public Object clone() { return new ClassifierReturnType(type, values); }


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
    String t = getTypeName();

    if (values.size() > 0) {
      assert t.startsWith("discrete") : "Non-discrete type with value list.";

      buffer.append("discrete{");
      ASTNodeIterator I = values.iterator();
      I.next().write(buffer);

      while (I.hasNext()) {
        buffer.append(", ");
        I.next().write(buffer);
      }

      buffer.append("}");

      int lastE = t.lastIndexOf('e');
      buffer.append(t.substring(lastE + 1));
    }
    else buffer.append(t);
  }
}

