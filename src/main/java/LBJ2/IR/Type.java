package LBJ2.IR;


/**
  * Abstract class representing the type of a variable or the return type of a
  * method.
  *
  * @author Nick Rizzolo
 **/
public abstract class Type extends ASTNode
{
  /**
    * This method takes a Java <code>Class</code> object and generates an
    * LBJ2 <code>Type</code> that represents the same type.
    *
    * @param c  The <code>Class</code> to "translate".
    * @return The <code>Type</code> object that represents the same type as
    *         the argument.
   **/
  public static Type parseType(Class c) { return parseType(c.getName()); }


  /**
    * This method takes a Java type encoding and generates an LBJ2
    * <code>Type</code> that represents the same type.
    *
    * @param encoding The encoding to decode.
    * @return The <code>Type</code> object that represents the same type as
    *         the argument.
   **/
  public static Type parseType(String encoding) {
    int dimensions = 0;
    while (encoding.charAt(dimensions) == '[') ++dimensions;

    if (dimensions == 0) {
      if (encoding.equals("boolean"))
        return new PrimitiveType(PrimitiveType.BOOLEAN);
      else if (encoding.equals("byte"))
        return new PrimitiveType(PrimitiveType.BYTE);
      else if (encoding.equals("char"))
        return new PrimitiveType(PrimitiveType.CHAR);
      else if (encoding.equals("double"))
        return new PrimitiveType(PrimitiveType.DOUBLE);
      else if (encoding.equals("float"))
        return new PrimitiveType(PrimitiveType.FLOAT);
      else if (encoding.equals("int"))
        return new PrimitiveType(PrimitiveType.INT);
      else if (encoding.equals("long"))
        return new PrimitiveType(PrimitiveType.LONG);
      else if (encoding.equals("short"))
        return new PrimitiveType(PrimitiveType.SHORT);
    }

    Type result = null;
    if (dimensions > 0) {
      switch (encoding.charAt(dimensions)) {
        case 'Z': result = new PrimitiveType(PrimitiveType.BOOLEAN); break;
        case 'B': result = new PrimitiveType(PrimitiveType.BYTE);    break;
        case 'C': result = new PrimitiveType(PrimitiveType.CHAR);    break;
        case 'D': result = new PrimitiveType(PrimitiveType.DOUBLE);  break;
        case 'F': result = new PrimitiveType(PrimitiveType.FLOAT);   break;
        case 'I': result = new PrimitiveType(PrimitiveType.INT);     break;
        case 'J': result = new PrimitiveType(PrimitiveType.LONG);    break;
        case 'S': result = new PrimitiveType(PrimitiveType.SHORT);   break;
        case 'L': break;
        default:
          System.err.println("ERROR: Can't parse type '" + encoding + "'");
          new Exception().printStackTrace();
          System.exit(1);
      }
    }

    if (result == null) {
      String referenceString;
      if (dimensions == 0) referenceString = encoding;
      else
        referenceString =
          encoding.substring(dimensions + 1, encoding.length() - 1);

      for (int i = referenceString.indexOf('$'); i != -1;
           i = referenceString.indexOf('$'))
        referenceString = referenceString.substring(0, i) + "."
                          + referenceString.substring(i + 1);

      result = new ReferenceType(new Name(referenceString));
    }

    for (int i = 0; i < dimensions; ++i) result = new ArrayType(result);
    return result;
  }


  /**
    * Set <code>true</code> by <code>SemanticAnalysis</code> iff this type
    * will be used to represent the argument of a
    * <code>QuantifiedConstraintExpression</code>.
   **/
  public boolean quantifierArgumentType;
  /**
    * Java's <code>Class</code> object defining the class that this
    * <code>Type</code> represents.
   **/
  protected Class myClass = null;


  /**
    * Default constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  protected Type(int line, int byteOffset) {
    super(line, byteOffset);
    quantifierArgumentType = false;
  }


  /**
    * Returns an object representing the <code>class</code> that this type
    * represents.
    *
    * @return An object representing the <code>class</code> that this type
    *         represents.
   **/
  public Class typeClass() { return myClass; }
}

