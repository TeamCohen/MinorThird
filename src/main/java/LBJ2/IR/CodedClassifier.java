package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a hard-coded classifier definition.
  *
  * @author Nick Rizzolo
 **/
public class CodedClassifier extends ClassifierExpression
{
  /**
    * (&not;&oslash;) Statements making up the body of the hard-coded
    * classifier.
   **/
  public Block body;


  /**
    * Full constructor.
    *
    * @param b  The body of the classifier.
   **/
  public CodedClassifier(Block b) {
    super(b.line, b.byteOffset);
    body = b;
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
    I.children[0] = body;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new CodedClassifier((Block) body.clone());
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return 31 * argument.hashCode() + 17 * body.hashCode();
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof CodedClassifier)) return false;
    CodedClassifier c = (CodedClassifier) o;
    return argument.equals(c.argument) && body.equals(c.body);
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
    if (parenthesized) buffer.append("(");
    body.write(buffer);
    if (parenthesized) buffer.append(")");
  }


  /**
    * Creates a <code>StringBuffer</code> containing a shallow representation
    * of this <code>ASTNode</code>.
    *
    * @return A <code>StringBuffer</code> containing a shallow text
    *         representation of the given node.
   **/
  public StringBuffer shallow() {
    StringBuffer buffer = new StringBuffer();
    returnType.write(buffer);
    buffer.append(" ");
    name.write(buffer);
    buffer.append("(");
    argument.write(buffer);
    buffer.append(") ");

    if (singleExampleCache) buffer.append("cached ");

    if (cacheIn != null) {
      buffer.append("cachedin");

      if (cacheIn.toString().equals(ClassifierAssignment.mapCache))
        buffer.append("map");
      else {
        buffer.append(" ");
        cacheIn.write(buffer);
      }

      buffer.append(' ');
    }

    buffer.append("<- ");
    body.write(buffer);
    return buffer;
  }
}

