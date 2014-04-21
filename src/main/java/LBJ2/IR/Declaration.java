package LBJ2.IR;


/**
  * Abstract representation of declarations such as <code>import</code> and
  * <code>package</code>.
  *
  * @author Nick Rizzolo
 **/
abstract public class Declaration extends ASTNode
{
  /**
    * (&oslash;) The text of a Javadoc comment that may appear before the
    * declaration.
   **/
  public String comment;
  /** (&not;&oslash;) Identifies what is being declared. */
  public Name name;


  /**
    * Initializing constructor.
    *
    * @param n          Reference to the name describing what is being
    *                   declared.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public Declaration(Name n, int line, int byteOffset) {
    this(null, n, line, byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param c          The text of a Javadoc comment.
    * @param n          Reference to the name describing what is being
    *                   declared.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public Declaration(String c, Name n, int line, int byteOffset) {
    super(line, byteOffset);
    comment = c;
    name = n;
  }


  /**
    * Returns the type of the declaration.
    *
    * @return The type of the declaration.
   **/
  abstract public Type getType();


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(1);
    I.children[0] = name;
    return I;
  }
}

