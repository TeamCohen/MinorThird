package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents the body of a switch statement.
  *
  * @author Nick Rizzolo
 **/
public class SwitchBlock extends ASTNode
{
  /** (&not;&oslash;) The list of labeled blocks of statements, if any. */
  public SwitchGroupList groups;
  /** (&not;&oslash;) The trailing list of labels, if any. */
  public SwitchLabelList labels;


  /**
    * Initializing constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public SwitchBlock(int line, int byteOffset) {
    this(new SwitchGroupList(), new SwitchLabelList(), line, byteOffset);
  }

  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the labels.
    *
    * @param l  The list of labels.
   **/
  public SwitchBlock(SwitchLabelList l) {
    this(new SwitchGroupList(), l, l.line, l.byteOffset);
  }

  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the groups.
    *
    * @param g  The list of groups.
   **/
  public SwitchBlock(SwitchGroupList g) { this(g, new SwitchLabelList()); }

  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the groups.
    *
    * @param g  The list of groups.
    * @param l  The list of labels.
   **/
  public SwitchBlock(SwitchGroupList g, SwitchLabelList l) {
    this(g, l, g.line, g.byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param g          The list of groups.
    * @param l          The list of labels.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public SwitchBlock(SwitchGroupList g, SwitchLabelList l, int line,
                     int byteOffset) {
    super(line, byteOffset);
    groups = g;
    labels = l;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(2);
    I.children[0] = groups;
    I.children[1] = labels;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new SwitchBlock((SwitchGroupList) groups.clone(),
                           (SwitchLabelList) labels.clone());
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return 31 * groups.hashCode() + 7 * labels.hashCode();
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof SwitchBlock)) return false;
    SwitchBlock s = (SwitchBlock) o;
    return groups.equals(s.groups) && labels.equals(s.labels);
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
    groups.write(buffer);
    buffer.append(" ");
    labels.write(buffer);
  }
}

