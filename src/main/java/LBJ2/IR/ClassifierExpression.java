package LBJ2.IR;


/**
  * Abstract classifier expression class.  The member variables of this class
  * are filled in either during parsing or during semantic analysis, but in
  * any case, they do not represent AST children that exist in the source.
  *
  * @author Nick Rizzolo
 **/
public abstract class ClassifierExpression extends ASTNode
                                           implements LBJ2.CodeGenerator
{
  /**
    * (&oslash;) The text of a Javadoc comment that may be associated with
    * this classifier.
   **/
  public String comment;
  /** Expression describing what is being declared. */
  public Name name;
  /** The return type of the declared classifier. */
  public ClassifierReturnType returnType;
  /** Specification of the classifier's input. */
  public Argument argument;
  /** Indicates whether this expression was parenthesized in the source. */
  public boolean parenthesized = false;
  /**
    * (&oslash;) The expression representing the field to cache this
    * classifier's result in.
   **/
  public Name cacheIn;
  /**
    * Whether the classifier will have a single example feature vector cache.
   **/
  public boolean singleExampleCache;


  /**
    * Default constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  ClassifierExpression(int line, int byteOffset) { super(line, byteOffset); }


  /** Returns the name of the <code>ClassifierExpression</code>. */
  public String getName() { return name.toString(); }


  /**
    * Returns the line number on which this AST node is found in the source
    * (starting from line 0).  This method exists to fulfull the
    * implementation of <code>CodeGenerator</code>.
    * @see LBJ2.CodeGenerator
   **/
  public int getLine() { return line; }


  /**
    * Sets the <code>cacheIn</code> member variable to the argument.
    *
    * @param c  The new expression for the <code>cacheIn</code> member
    *           variable.
   **/
  public void setCacheIn(Name c) { cacheIn = c; }


  /**
    * Creates a <code>StringBuffer</code> containing a shallow representation
    * of this <code>ClassifierExpression</code>.
    *
    * @return A <code>StringBuffer</code> containing a shallow text
    *         representation of the given node.
   **/
  abstract public StringBuffer shallow();
}

