package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a type defined by a <code>class</code>.  Note that in LBJ2's
  * parser, the nonterminal <code>referenceType</code> refers to both types
  * defined by <code>class</code>es and array types, but this class represents
  * only the former.
  *
  * @author Nick Rizzolo
 **/
public class ReferenceType extends Type
{
  /**
    * (&not;&oslash;) The expression representing the name of the class that
    * defines this type.
   **/
  private Name name;


  /**
    * Initializing constructor.  Line and byte offset information is taken
    * from the expression.
    *
    * @param name The expression representing the name of the class that
    *             defines this type.
   **/
  public ReferenceType(Name name) {
    super(name.line, name.byteOffset);
    this.name = name;
  }


  /**
    * Returns the name of the class that defines this type.
    *
    * @return The contents of <code>name</code>.
   **/
  public Name getName() { return name; }


  /**
    * Returns an object representing the <code>class</code> that this type
    * represents.
    *
    * @return An object representing the <code>class</code> that this type
    *         represents.
   **/
  public Class typeClass() {
    if (myClass == null) myClass = AST.globalSymbolTable.classForName(name);
    return myClass;
  }


  /**
    * Two <code>ReferenceType</code>s are equivalent when their associated
    * Java <code>class</code>es, as computed by <code>typeClass()</code> are
    * equivalent.
    *
    * @param t  The <code>Type</code> whose equality with this object needs to
    *           be tested.
    * @return <code>true</code> if the two <code>Type</code>s are equal, and
    *         <code>false</code> otherwise.
   **/
  public boolean equals(Object t) {
    if (!(t instanceof ReferenceType)) return false;
    ReferenceType r = (ReferenceType) t;
    if (typeClass() != null) return typeClass().equals(r.typeClass());
    return name.equals(r.name);
  }


  /** A hash code based on the hash code of {@link #name}. */
  public int hashCode() { return name.hashCode(); }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(1);
    I.children[0] = name;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() { return new ReferenceType((Name) name.clone()); }


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
  public void write(StringBuffer buffer) { name.write(buffer); }
}

