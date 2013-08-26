package LBJ2.IR;

import LBJ2.Pass;


/**
  * A normalizer type is simply a place holder indicating that the name it is
  * associated with in the symbol table is a normalizer function.
  *
  * @author Nick Rizzolo
 **/
public class NormalizerType extends Type
{
  /** Default constructor. */
  public NormalizerType() {
    super(-1, -1);

    try { myClass = Class.forName("LBJ2.learn.Normalizer"); }
    catch (Exception e) {
      System.err.println("Class 'LBJ2.learn.Normalizer' not found.  "
                         + "Aborting.");
      System.exit(1);
    }
  }


  /**
    * Any two <code>NormalizerType</code>s are equivalent.
    *
    * @param o  The object whose equality with this object needs to be tested.
    * @return <code>true</code> if the two objects are equal, and
    *         <code>false</code> otherwise.
   **/
  public boolean equals(Object o) { return o instanceof NormalizerType; }


  /**
    * Returns a constant, since all objects of this type are equal according
    * to {@link #equals(Object)}.
   **/
  public int hashCode() { return 17; }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() { return new ASTNodeIterator(0); }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() { return new NormalizerType(); }


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
  public void write(StringBuffer buffer) { buffer.append("Normalizer { }"); }
}

