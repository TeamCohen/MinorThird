package LBJ2.IR;

import LBJ2.Pass;


/**
  * Abstract node class that every AST node extends. <br><br>
  *
  * Every AST node must
  * <ul>
  *   <li>
  *     have a unique ID number and store the line and byte offset where it
  *     was found in the source file.
  *   <li>
  *     include a special symbol at the beginning of the Javadoc comment of
  *     each of its AST node child member variables indicating whether that
  *     member variable is allowed to be set to <code>null</code>.  The symbol
  *     (&oslash;) is used to indicate that the variable is allowed to be
  *     <code>null</code>, and the symbol (&not;&oslash;) indicates that the
  *     variable will never be <code>null</code>.
  *   <li>
  *     return an <code>ASTNodeIterator</code> through the abstract
  *     <code>iterator()</code> method that traverses its children in the
  *     order in which the code would be executed.  If the node is composed of
  *     declarative constructs rather than executable code, the iterator will
  *     traverse those constructs in the order in which they appear in the
  *     source.
  *   <li>
  *     override the <code>clone()</code> method such that it returns a deep
  *     copy of itself, except that the line and byte offset of the copy are
  *     both set to <code>-1</code>.
  *   <li>
  *     override the <code>runPass(Pass)</code> method so that it simply calls
  *     the <code>Pass</code>'s <code>run(.)</code> method whose argument type
  *     is the type of the node.
  *   <li>
  *     return a <code>String</code> representation of itself through the
  *     <code>toString()</code> and <code>write(StringBuffer)</code> methods.
  *     Only the <code>write(StringBuffer)</code> method need be overridden by
  *     each node, as <code>toString()</code> will simply invoke
  *     <code>write(StringBuffer)</code> to produce its result.  (This is much
  *     more efficient than having <code>toString()</code> call its childrens'
  *     <code>toString()</code> methods recursively through the AST and
  *     concatenating them all together.)  The <code>String</code> produced
  *     will not be very readable (e.g., it won't contain any new lines), but
  *     it will be compilable by the LBJ compiler.
  * </ul>
  *
  * Most <code>ASTNode</code>s will also contain more than one constructor.
  * There will be one constructor that includes all references to its children
  * as well as line and byte offset information, etc.  This constructor is
  * commonly used by the node's <code>clone()</code> method.  In addition,
  * there may be at least one constructor designed to be more useful for the
  * JavaCUP parser, taking <code>TokenValue</code>s as input.
  *
  * @author Nick Rizzolo
 **/
public abstract class ASTNode
{
  /** Keeps track of how many nodes have been created. */
  private static int nextID = 0;
  /** Stores the ID of this node as provided by <code>nextID</code>. */
  public int nodeID;

  /** The line on which the source code represented by this node is found. */
  public int line;
  /**
    * The byte offset from the beginning of the source file at which the
    * source code represented by this node is found.
   **/
  public int byteOffset;
  /** The table of variable types representing this node's scope. */
  public SymbolTable symbolTable;


  /** Default constructor. */
  public ASTNode() { this(-1, -1); }


  /**
    * Initializing constructor.  This constructor is called via the
    * <code>super</code> operator from every other node.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ASTNode(int line, int byteOffset) {
    nodeID = nextID++;
    this.line = line;
    this.byteOffset = byteOffset;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  abstract public ASTNodeIterator iterator();


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    System.err.println("WARNING: clone() not defined for class '"
                       + this.getClass().getName() + "'");
    return null;
  }


  /**
    * Ensures that the correct <code>run()</code> method is called for this
    * type of node.
    *
    * @param pass The pass whose <code>run()</code> method should be called.
   **/
  abstract public void runPass(Pass pass);


  /**
    * Calls the <code>write(StringBuffer)</code> method to produce a string
    * representation of this node.
    *
    * @return A textual representation of this node.
   **/
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    write(buffer);
    return buffer.toString();
  }


  /**
    * Writes a string representation of this <code>ASTNode</code> to the
    * specified buffer.  The representation written is parsable by the LBJ2
    * compiler, but not very readable.
    *
    * @param buffer The buffer to write to.
   **/
  abstract public void write(StringBuffer buffer);
}

