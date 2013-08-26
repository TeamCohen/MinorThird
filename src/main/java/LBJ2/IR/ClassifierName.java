package LBJ2.IR;

import LBJ2.Pass;


/**
  * This class represents identifiers that name classifiers.  It is ostensibly
  * the same class as <code>Name</code>, but it extends
  * <code>ClassifierExpression</code>, helping keep the syntax of classifier
  * manipulation separate from Java's method definition syntax.
  *
  * @see    Name
  * @author Nick Rizzolo
 **/
public class ClassifierName extends ClassifierExpression
{
  /**
    * (&not;&oslash;) The name as it appears in the source code.  The member
    * variable <code>name</code> defined in <code>ClassifierExpression</code>
    * will be used by <code>SemanticAnalysis</code> for other purposes.
    *
    * @see LBJ2.SemanticAnalysis
    * @see ClassifierExpression#name
   **/
  public Name referent;


  /**
    * Full constructor.  Line and byte offset information is taken from the
    * name.
    *
    * @param n  A name.
   **/
  public ClassifierName(Name n) {
    super(n.line, n.byteOffset);
    referent = n;
  }

  /**
    * Takes a fully specified name (eg java.lang.String) as input.
    *
    * @param n  A fully specified name.
   **/
  public ClassifierName(String n) { this(n, -1, -1); }

  /**
    * Takes a fully specified name (eg java.lang.String) as input.
    *
    * @param n  A fully specified name.
   **/
  public ClassifierName(String n, int line, int byteOffset) {
    super(line, byteOffset);
    referent = new Name(n);
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() { return referent.hashCode(); }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof ClassifierName)) return false;
    ClassifierName n = (ClassifierName) o;
    return referent.equals(n.referent);
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
    I.children[0] = referent;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new ClassifierName((Name) referent.clone());
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
  public void write(StringBuffer buffer) { referent.write(buffer); }


  /**
    * Creates a <code>StringBuffer</code> containing a shallow representation
    * of this <code>ClassifierExpression</code>.
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
    referent.write(buffer);
    return buffer;
  }
}

