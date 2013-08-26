package LBJ2.IR;

import LBJ2.Pass;


/**
  * A block is just a list of statements in between curly braces.
  *
  * @author Nick Rizzolo
 **/
public class Block extends Statement
{
  /** (&not;&oslash;) The list of statements. */
  private StatementList statements;


  /**
    * Initializing constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public Block(int line, int byteOffset) {
    super(line, byteOffset);
    statements = new StatementList();
  }

  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the statement list's representation.
    *
    * @param list The statement list.
   **/
  public Block(StatementList list) {
    super(list.line, list.byteOffset);
    statements = list;
  }


  /**
    * Returns the statement list.
    *
    * @return This block's statement list.
   **/
  public StatementList statementList() { return statements; }


  /**
    * Transforms the list into an array of statements.
    *
    * @return An array of statements containing references to every statement
    *         in the list.
   **/
  public Statement[] toArray() { return statements.toArray(); }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(1);
    I.children[0] = statements;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new Block((StatementList) statements.clone());
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() { return statements.hashCode(); }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof Block)) return false;
    Block b = (Block) o;
    return statements.equals(b.statements);
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
    buffer.append("{ ");
    statements.write(buffer);
    buffer.append(" }");
  }
}

