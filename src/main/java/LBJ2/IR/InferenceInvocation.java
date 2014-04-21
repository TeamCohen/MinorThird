package LBJ2.IR;

import LBJ2.Pass;


/**
  * An inference can be invoked as a method with the name of a learning
  * classifier involved in that inference as its lone argument.
  *
  * @author Nick Rizzolo
 **/
public class InferenceInvocation extends ClassifierExpression
{
  /** (&not;&oslash;) The name of the inference to invoke. */
  public Name inference;
  /** (&not;&oslash;) The name of the argument learning classifier. */
  public Name classifier;


  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the name of the inference.
    *
    * @param i  The name of the inference.
    * @param c  The name of the classifier.
   **/
  public InferenceInvocation(Name i, Name c) {
    super(i.line, i.byteOffset);
    inference = i;
    classifier = c;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    return inference.hashCode() + classifier.hashCode();
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof InferenceInvocation)) return false;
    InferenceInvocation i = (InferenceInvocation) o;
    return inference.equals(i.inference) && classifier.equals(i.classifier);
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
    I.children[0] = inference;
    I.children[1] = classifier;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return
      new InferenceInvocation((Name) inference.clone(),
                              (Name) classifier.clone());
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
    inference.write(buffer);
    buffer.append("(");
    classifier.write(buffer);
    buffer.append(")");
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
    inference.write(buffer);
    buffer.append("(");
    classifier.write(buffer);
    buffer.append(")");
    return buffer;
  }
}

