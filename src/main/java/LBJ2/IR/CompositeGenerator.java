package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a generator composed from several other classifiers.
  *
  * @author Nick Rizzolo
 **/
public class CompositeGenerator extends ClassifierExpression
{
  /** (&not;&oslash;) The list of classifiers composing this classifier. */
  public ClassifierExpressionList components;


  /**
    * Full constructor.  The line and byte offset are each set to -1.
    *
    * @param c  The list of components.
   **/
  public CompositeGenerator(ClassifierExpressionList c) {
    super(-1, -1);
    components = c;
  }

  /**
    * Parser's constructor.
    *
    * @param e1 One <code>ClassifierExpression</code> to add.
    * @param e2 Another <code>ClassifierExpression</code> to add.
   **/
  public CompositeGenerator(ClassifierExpression e1, ClassifierExpression e2)
  {
    super(e1.line, e2.byteOffset);

    if (e1 instanceof CompositeGenerator)
      components = ((CompositeGenerator) e1).components;
    else components = new ClassifierExpressionList(e1);

    if (e2 instanceof CompositeGenerator)
      components.addAll(((CompositeGenerator) e2).components);
    else components.add(e2);
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
    I.children[0] = components;
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
      new CompositeGenerator((ClassifierExpressionList) components.clone());
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return 17 * components.hashCode();
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof CompositeGenerator)) return false;
    CompositeGenerator c = (CompositeGenerator) o;
    return components.equals(c.components);
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
    components.write(buffer);
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
    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           components.listIterator();
         I.hasNext(); )
      buffer.append(I.nextItem().name + ", ");
    return buffer;
  }
}

