package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a continue statement.
  *
  * @author Nick Rizzolo
 **/
public class ContinueStatement extends Statement
{
  /** (&oslash;) The label identifying the loop to continue, if any. */
  public String label;


  /**
    * Full constructor.
    *
    * @param l          The label of the loop to continue.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public ContinueStatement(String l, int line, int byteOffset) {
    super(line, byteOffset);
    label = l;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() { return new ASTNodeIterator(0); }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() { return new ContinueStatement(label, -1, -1); }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return label == null ? 7 : label.hashCode();
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof ContinueStatement)) return false;
    ContinueStatement c = (ContinueStatement) o;
    return label == null ? c.label == null : label.equals(c.label);
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
    buffer.append("continue");
    if (label != null) buffer.append(" " + label);
    buffer.append(";");
  }
}

