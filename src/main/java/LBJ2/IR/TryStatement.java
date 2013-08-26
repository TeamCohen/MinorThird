package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a try statement.
  *
  * @author Nick Rizzolo
 **/
public class TryStatement extends Statement
{
  /** (&not;&oslash;) The code to look for exceptions in. */
  public Block block;
  /** (&not;&oslash;) A list of clauses for catching exceptions, if any. */
  public CatchList catchList;
  /** (&oslash;) The block of the "finally" clause, if any. */
  public Block finallyBlock;


  /**
    * Initializing constructor.
    *
    * @param b          The code to look for exceptions in.
    * @param l          The list of <code>CatchClause</code>s.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public TryStatement(Block b, CatchList l, int line, int byteOffset) {
    this(b, l, null, line, byteOffset);
  }

  /**
    * Initializing constructor.
    *
    * @param b          The code to look for exceptions in.
    * @param f          The finally block.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public TryStatement(Block b, Block f, int line, int byteOffset) {
    this(b, new CatchList(), f, line, byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param b          The code to look for exceptions in.
    * @param l          The list of <code>CatchClause</code>s.
    * @param f          The finally block.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public TryStatement(Block b, CatchList l, Block f, int line, int byteOffset)
  {
    super(line, byteOffset);
    block = b;
    catchList = l;
    finallyBlock = f;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(finallyBlock == null ? 2 : 3);
    I.children[0] = block;
    I.children[1] = catchList;
    if (finallyBlock != null) I.children[2] = finallyBlock;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new TryStatement(
        (Block) block.clone(),
        (CatchList) catchList.clone(),
        (finallyBlock == null ? null : (Block) finallyBlock.clone()), -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    int result = 31 * block.hashCode() + 17 * catchList.hashCode();
    if (finallyBlock != null) result += 7 * finallyBlock.hashCode();
    return result;
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof TryStatement)) return false;
    TryStatement t = (TryStatement) o;
    return
      block.equals(t.block) && catchList.equals(t.catchList)
      && (finallyBlock == null ? t.finallyBlock == null
                               : finallyBlock.equals(t.finallyBlock));
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
    buffer.append("try ");
    block.write(buffer);

    if (catchList.size() > 0) {
      buffer.append(" ");
      catchList.write(buffer);
    }

    if (finallyBlock != null) {
      buffer.append(" ");
      finallyBlock.write(buffer);
    }
  }
}

