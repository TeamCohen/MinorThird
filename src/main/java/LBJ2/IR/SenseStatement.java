package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a feature sensing statement.
  *
  * @author Nick Rizzolo
 **/
public class SenseStatement extends Statement
{
  /**
    * (&oslash;) Represents the name of the feature being sensed (only used in
    * generators).
   **/
  public Expression name;
  /** (&not;&oslash;) Represents the value of the feature being sensed. */
  public Expression value;
  /** <code>true</code> iff this is a <code>senseall</code> statement. */
  public boolean senseall;


  /**
    * Initializing constructor.
    *
    * @param v          The value of the feature being sensed.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public SenseStatement(Expression v, int line, int byteOffset) {
    this(null, v, false, line, byteOffset);
  }

  /**
    * Initializing constructor.
    *
    * @param v          The value of the feature being sensed.
    * @param all        <code>true</code> iff this is a <code>senseall</code>
    *                   statement.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public SenseStatement(Expression v, boolean all, int line, int byteOffset) {
    this(null, v, all, line, byteOffset);
  }

  /**
    * Initializing constructor.
    *
    * @param n          The name of the feature being sensed.
    * @param v          The value of the feature being sensed.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public SenseStatement(Expression n, Expression v, int line, int byteOffset)
  {
    this(n, v, false, line, byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param n          The name of the feature being sensed.
    * @param v          The value of the feature being sensed.
    * @param all        <code>true</code> iff this is a <code>senseall</code>
    *                   statement.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public SenseStatement(Expression n, Expression v, boolean all, int line,
                        int byteOffset) {
    super(line, byteOffset);
    name = n;
    value = v;
    senseall = all;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(name == null ? 1 : 2);
    if (name != null) I.children[0] = name;
    I.children[I.children.length - 1] = value;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new SenseStatement(
        (name == null ? null : (Expression) name.clone()),
        (Expression) value.clone(), senseall, -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    int result = (senseall ? 1 : 3) + 31 * value.hashCode();
    if (name != null) result += name.hashCode();
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
    if (!(o instanceof SenseStatement)) return false;
    SenseStatement s = (SenseStatement) o;
    return
      senseall == s.senseall && value.equals(s.value)
      && (name == null ? s.name == null : name.equals(s.name));
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
    buffer.append("sense");
    if (senseall) buffer.append("all");
    buffer.append(" ");

    if (name != null) {
      name.write(buffer);
      buffer.append(" : ");
    }

    value.write(buffer);
    buffer.append(";");
  }
}

