package LBJ2.IR;

import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * Represents any statement with an identifier label.
  *
  * @author Nick Rizzolo
 **/
public class LabeledStatement extends Statement
{
  /** (&not;&oslash;) The label for the statement. */
  public String label;
  /** (&not;&oslash;) The statement. */
  public Statement statement;


  /**
    * Parser's constructor.  Line and byte offset information are taken from
    * the label's representation.
    *
    * @param l  The token representing the label.
    * @param s  The statement.
   **/
  public LabeledStatement(TokenValue l, Statement s) {
    this(l.toString(), s, l.line, l.byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param l          The label.
    * @param s          The statement.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public LabeledStatement(String l, Statement s, int line, int byteOffset) {
    super(line, byteOffset);
    label = l;
    statement = s;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(1);
    I.children[0] = statement;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new LabeledStatement(label, (Statement) statement.clone(), -1, -1);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return 31 * label.hashCode() + 7 * statement.hashCode();
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof LabeledStatement)) return false;
    LabeledStatement l = (LabeledStatement) o;
    return label.equals(l.label) && statement.equals(l.statement);
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
    buffer.append(label + ": ");
    statement.write(buffer);
  }
}

