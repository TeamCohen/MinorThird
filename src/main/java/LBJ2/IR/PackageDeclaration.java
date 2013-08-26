package LBJ2.IR;

import LBJ2.Pass;


/**
  * Representation of an <code>package</code> declaration.
  *
  * @author Nick Rizzolo
 **/
public class PackageDeclaration extends Declaration
{
  /**
    * Full constructor.
    *
    * @param n          Reference to the object representing the package name.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public PackageDeclaration(Name n, int line, int byteOffset) {
    super(n, line, byteOffset);
  }


  /**
    * Returns <code>null</code>, since this method should never be called on
    * an object of this class.
    *
    * @return <code>null</code>
   **/
  public Type getType() { return null; }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return new PackageDeclaration((Name) name.clone(), -1, -1);
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
    buffer.append("package ");
    name.write(buffer);
    buffer.append(";");
  }
}

