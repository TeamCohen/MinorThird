package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a break statement.
  *
  * @author Nick Rizzolo
 **/
public class BreakStatement extends Statement
{
  /** (&oslash;) The label identifying the loop to break out of, if any. */
  public String label;


  /**
    * Full constructor.
    *
    * @param l          The label of the loop to break out of.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public BreakStatement(String l, int line, int byteOffset) {
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
  public Object clone() { return new BreakStatement(label, -1, -1); }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return label != null ? label.hashCode() : 7;
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof BreakStatement)) return false;
    BreakStatement b = (BreakStatement) o;
    return label == null ? b.label == null : label.equals(b.label);
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
    buffer.append("break");
    if (label != null) buffer.append(" " + label);
    buffer.append(";");
  }
}

