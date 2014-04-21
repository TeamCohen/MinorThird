package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a local variable declaration.
  *
  * @author Nick Rizzolo
 **/
public class VariableDeclaration extends Statement
{
  /** Whether or not the argument was modified as final. */
  public boolean isFinal;
  /** (&not;&oslash;) The type of the declared variable. */
  public Type type;
  /** (&not;&oslash;) The names of variables declared in this statement. */
  public NameList names;
  /**
    * (&not;&oslash;) The initializing expressions for the declared variables,
    * <code>null</code> being an allowable value.
   **/
  public ExpressionList initializers;


  /**
    * Parser's constructor, leaving the type to be filled in later.
    *
    * @param n  The name of the declared variable.
   **/
  public VariableDeclaration(Name n) { this(n, null); }

  /**
    * Parser's constructor, leaving the type to be filled in later.
    *
    * @param n  The name of the declared variable.
    * @param i  The initializing expression for the declared variable.
   **/
  public VariableDeclaration(Name n, Expression i) {
    super(-1, -1);  // Line and byte offset information will be filled in
                    // later.
    names = new NameList(n);
    initializers = new ExpressionList(i);
    type = null;
    isFinal = false;
  }

  /**
    * Full constructor.
    *
    * @param t  The type of the declared variables.
    * @param n  The names of the declared variables.
    * @param i  The initializing expressions for the declared variables.
    * @param f  Whether or not the variables were declared as final.
   **/
  public VariableDeclaration(Type t, NameList n, ExpressionList i, boolean f)
  {
    super(t.line, t.byteOffset);
    type = t;
    names = n;
    initializers = i;
    isFinal = f;
  }


  /**
    * Adds the declarations in the specified declaration statement to the
    * declarations in this statement.
    *
    * @param v  The variables to be added.
   **/
  public void addVariables(VariableDeclaration v) {
    names.addAll(v.names);
    initializers.addAll(v.initializers);
  }


  /**
    * Setting this declaration statement's type also sets its line and byte
    * offset information.
    *
    * @param t  The new type for this variable declaration statement.
   **/
  public void setType(Type t) {
    type = t;
    line = type.line;
    byteOffset = type.byteOffset;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(3);
    I.children[0] = type;
    I.children[1] = names;
    I.children[2] = initializers;
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
      new VariableDeclaration((Type) type.clone(), (NameList) names.clone(),
                              (ExpressionList) initializers.clone(), isFinal);
  }


  /** Returns a hash code for this {@link ASTNode}. */
  public int hashCode() {
    return
      (isFinal ? 1 : 3) + 7 * type.hashCode() + 17 * names.hashCode()
      + 31 * initializers.hashCode();
  }


  /**
    * Distinguishes this {@link ASTNode} from other objects according to its
    * contents recursively.
    *
    * @param o  Another object.
    * @return <code>true</code> iff this node is equal to <code>o</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof VariableDeclaration)) return false;
    VariableDeclaration v = (VariableDeclaration) o;
    return
      isFinal == v.isFinal && type.equals(v.type) && names.equals(v.names)
      && initializers.equals(v.initializers);
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
    if (isFinal) buffer.append("final ");
    type.write(buffer);

    ASTNodeIterator N = names.iterator();
    ExpressionList.ExpressionListIterator I = initializers.listIterator();
    buffer.append(" ");
    N.next().write(buffer);

    Expression i = I.nextItem();
    if (i != null) {
      buffer.append(" = ");
      i.write(buffer);
    }

    while (N.hasNext()) {
      buffer.append(", ");
      N.next().write(buffer);
      i = I.nextItem();
      if (i != null) {
        buffer.append(" = ");
        i.write(buffer);
      }
    }

    buffer.append(";");
  }
}

