package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents a classifier conjunction.
  *
  * @author Nick Rizzolo
 **/
public class Conjunction extends ClassifierExpression
{
  /** (&not;&oslash;) The left hand side of the conjunction. */
  public ClassifierExpression left;
  /** (&not;&oslash;) The right hand side of the conjunction. */
  public ClassifierExpression right;


  /**
    * Initializing constructor.
    *
    * @param l          Reference to the left hand side's representation.
    * @param r          Reference to the right hand side's representation.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public Conjunction(ClassifierExpression l, ClassifierExpression r, int line,
                     int byteOffset) {
    super(line, byteOffset);
    left = l;
    right = r;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    return 31 * left.hashCode() + 17 * right.hashCode();
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof Conjunction)) return false;
    Conjunction c = (Conjunction) o;
    return left.equals(c.left) && right.equals(c.right);
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
    I.children[0] = left;
    I.children[1] = right;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new Conjunction((ClassifierExpression) left.clone(),
                           (ClassifierExpression) right.clone(), -1, -1);
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
    left.write(buffer);
    buffer.append(" && ");
    right.write(buffer);
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
    left.name.write(buffer);
    buffer.append(" && ");
    right.name.write(buffer);
    return buffer;
  }
}

