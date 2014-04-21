package LBJ2.IR;

import LBJ2.Pass;


/**
  * A classifier's type is defined by what it takes as input and what it
  * returns as output, but it is distinguished only by what it takes as input.
  * This class does not represent a syntax that appears in the source - it is
  * constructed only during semantic analysis.
  *
  * @author Nick Rizzolo
 **/
public class ClassifierType extends Type
{
  /** The type of the classifier's input. */
  protected Type input;
  /** The type of the classifier's output. */
  protected ClassifierReturnType output;
  /** Whether or not the classifier is derived from a learning algorithm. */
  protected boolean learner;


  /**
    * Initializing constructor.
    *
    * @param i  The classifier's input type.
    * @param o  The classifier's output type.
    * @param l  Whether or not the classifier is a learner.
   **/
  public ClassifierType(Type i, ClassifierReturnType o, boolean l) {
    super(-1, -1);
    input = i;
    output = o;
    learner = l;

    try { myClass = Class.forName("LBJ2.classify.Classifier"); }
    catch (Exception e) {
      System.err.println("Class 'LBJ2.classify.Classifier' not found.  "
                         + "Aborting.");
      System.exit(1);
    }
  }


  /** Retrieves the value of the <code>input</code> variable. */
  public Type getInput() { return input; }


  /** Retrieves the value of the <code>input</code> variable. */
  public ClassifierReturnType getOutput() { return output; }


  /** Retrieves the value of the <code>learner</code> variable. */
  public boolean isLearner() { return learner; }


  /**
    * Two <code>ClassifierType</code>s are equivalent when their input types
    * match.
    *
    * @param o  The object whose equality with this object needs to be tested.
    * @return <code>true</code> if the two objects are equal, and
    *         <code>false</code> otherwise.
   **/
  public boolean equals(Object o) {
    return o instanceof ClassifierType
           && input.equals(((ClassifierType) o).input);
  }


  /** A hash code based on the hash code of {@link #input}. */
  public int hashCode() {
    return 31 * input.hashCode() + 17;
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
    I.children[0] = input;
    I.children[1] = output;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new ClassifierType((Type) input.clone(),
                              (ClassifierReturnType) output.clone(),
                              learner);
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
    buffer.append("Classifier { ");
    output.write(buffer);
    buffer.append(" : ");
    input.write(buffer);
    buffer.append(" : ");
    buffer.append(learner);
    buffer.append(" }");
  }
}

