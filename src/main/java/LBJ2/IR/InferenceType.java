package LBJ2.IR;

import LBJ2.Pass;


/**
  * An inference's type is defined by the type of the head object as well as
  * the types of objects from which the head can be found.  This class does
  * not represent a syntax that appears in the source - it is constructed only
  * during semantic analysis.
  *
  * @author Nick Rizzolo
 **/
public class InferenceType extends Type
{
  /** The type of the head object. */
  protected Type headType;
  /** The types of the head finder objects. */
  protected Type[] headFinderTypes;


  /**
    * Initializing constructor.
    *
    * @param h  The head object's type.
    * @param f  The array of head finder types.
   **/
  public InferenceType(Type h, Type[] f) {
    super(-1, -1);
    headType = h;
    headFinderTypes = f;

    try { myClass = Class.forName("LBJ2.infer.Inference"); }
    catch (Exception e) {
      System.err.println("Class 'LBJ2.infer.Inference' not found.  "
                         + "Aborting.");
      System.exit(1);
    }
  }

  /**
    * Initializing constructor.
    *
    * @param h  The head object's type.
    * @param f  The array of head finder arguments from the inference
    *           declaration.
   **/
  public InferenceType(Type h, InferenceDeclaration.HeadFinder[] f) {
    super(-1, -1);
    headType = h;

    if (f == null) headFinderTypes = new Type[0];
    else {
      headFinderTypes = new Type[f.length];
      for (int i = 0; i < f.length; ++i)
        headFinderTypes[i] = f[i].argument.getType();
    }

    try { myClass = Class.forName("LBJ2.infer.Inference"); }
    catch (Exception e) {
      System.err.println("Class 'LBJ2.infer.Inference' not found.  "
                         + "Aborting.");
      System.exit(1);
    }
  }


  /** Retrieves the value of the <code>headType</code> variable. */
  public Type getHeadType() { return headType; }


  /** Retrieves the number of head finder types. */
  public int getFindersLength() { return headFinderTypes.length; }


  /**
    * Retrieves the type of the <i>i<sup>th</sup></i> head finder object.
    *
    * @param i  The index of the head finder type requested.
    * @return The type at location <i>i</i> in the
    *         <code>headFinderTypes</code> array member variable, or
    *         <code>null</code> if <i>i</i> is outside the bounds of that
    *         array.
   **/
  public Type getFinderType(int i) {
    if (i < 0 || i >= headFinderTypes.length) return null;
    return headFinderTypes[i];
  }


  /**
    * Two <code>InferenceType</code>s are equivalent when their head types
    * match.
    *
    * @param o  The object whose equality with this object needs to be tested.
    * @return <code>true</code> if the two objects are equal, and
    *         <code>false</code> otherwise.
   **/
  public boolean equals(Object o) {
    return o instanceof InferenceType
           && headType.equals(((InferenceType) o).headType);
  }


  /** A hash code based on the hash code of {@link #headType}. */
  public int hashCode() {
    return 31 * headType.hashCode() + 17;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(headFinderTypes.length + 1);
    I.children[0] = headType;
    for (int i = 0; i < headFinderTypes.length; ++i)
      I.children[i + 1] = headFinderTypes[i];
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new InferenceType((Type) headType.clone(),
                             (Type[]) headFinderTypes.clone());
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
    buffer.append("Inference { ");
    headType.write(buffer);
    buffer.append(" : ");

    if (headFinderTypes.length > 0) {
      headFinderTypes[0].write(buffer);
      for (int i = 1; i < headFinderTypes.length; ++i) {
        buffer.append(", ");
        headFinderTypes[i].write(buffer);
      }
    }

    buffer.append(" }");
  }
}

