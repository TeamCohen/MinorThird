package LBJ2.IR;

import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * Represents the declaration of a constraint.  Constraints declared in an
  * LBJ source file are mainly used to constrain inferences, but they may also
  * be used exactly as if they were a classifier with return type
  * <code>discrete{false, true}</code>.
  *
  * @author Nick Rizzolo
 **/
public class ConstraintDeclaration extends Declaration
                                   implements LBJ2.CodeGenerator
{
  /** (&not;&oslash;) The input specification of the constraint. */
  public Argument argument;
  /** (&not;&oslash;) Statements making up the body of the constraint. */
  public Block body;


  /**
    * Full constructor.
    *
    * @param c          A Javadoc comment associated with the declaration.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param n          The constraint's name.
    * @param a          The input specification of the constraint.
    * @param b          The code block representing the constraint.
   **/
  public ConstraintDeclaration(String c, int line, int byteOffset, Name n,
                               Argument a, Block b) {
    super(c, n, line, byteOffset);
    argument = a;
    body = b;
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the first token.
    *
    * @param t  The first token indicates line and byte offset information.
    * @param i  The identifier token representing the constraint's name.
    * @param a  The input specification of the constraint.
    * @param b  The code block representing the constraint.
   **/
  public ConstraintDeclaration(TokenValue t, TokenValue i, Argument a,
                               Block b) {
    this(null, t.line, t.byteOffset, new Name(i), a, b);
  }


  /**
    * Returns the type of the declaration.
    *
    * @return The type of the declaration.
   **/
  public Type getType() { return new ConstraintType(argument.getType()); }


  /** Returns the name of the <code>ConstraintDeclaration</code>. */
  public String getName() { return name.toString(); }


  /**
    * Returns the line number on which this AST node is found in the source
    * (starting from line 0).  This method exists to fulfull the
    * implementation of <code>CodeGenerator</code>.
    * @see LBJ2.CodeGenerator
   **/
  public int getLine() { return line; }


  /** Returns a shallow textual representation of this AST node. */
  public StringBuffer shallow() {
    StringBuffer buffer = new StringBuffer();
    write(buffer);
    return buffer;
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
    I.children[0] = argument;
    I.children[1] = body;
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
      new ConstraintDeclaration(comment, -1, -1, (Name) name.clone(),
                               (Argument) argument.clone(),
                               (Block) body.clone());
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
    buffer.append("constraint ");
    name.write(buffer);
    buffer.append("(");
    argument.write(buffer);
    buffer.append(") ");
    body.write(buffer);
  }
}

