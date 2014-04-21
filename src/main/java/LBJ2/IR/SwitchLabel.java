package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a case or default label inside a switch block.
  *
  * @author Nick Rizzolo
 **/
public class SwitchLabel extends ASTNode
{
  /** (&oslash;) The expression representing the value to match, if any. */
  public Expression value;


  /**
    * Full constructor.
    *
    * @param v          The expression representing the value to match, if
    *                   any.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public SwitchLabel(Expression v, int line, int byteOffset) {
    super(line, byteOffset);
    value = v;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(value == null ? 0 : 1);
    if (value != null) I.children[0] = value;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new SwitchLabel(value == null ? null : (Expression) value.clone(),
                           -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    int result = 17;
    if (value != null) result += 7 * value.hashCode();
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
    if (!(o instanceof SwitchLabel)) return false;
    SwitchLabel s = (SwitchLabel) o;
    return value == null ? s.value == null : value.equals(s.value);
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
    if (value == null) buffer.append("default:");
    else {
      buffer.append("case ");
      value.write(buffer);
      buffer.append(":");
    }
  }
}

