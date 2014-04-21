package LBJ2.IR;

import java.util.HashSet;
import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * This class represents a scalar variable.
  *
  * @author Nick Rizzolo
 **/
public class Name extends Expression
{
  /**
    * (&not;&oslash;) These strings appeared with dots between them to form
    * the name in the source.
   **/
  public String[] name;
  /**
    * The number of matched brackets appearing after a single identifier;
    * supports variable declarations.
   **/
  public int dimensions;


  /**
    * Takes a fully specified name (eg java.lang.String) as input.
    *
    * @param n  A fully specified name.
   **/
  public Name(String n) { this(n, -1, -1); }

  /**
    * Takes a fully specified name (eg java.lang.String) as input.
    *
    * @param n  A fully specified name.
   **/
  public Name(String n, int line, int byteOffset) {
    super(line, byteOffset);
    name = n.split("\\.");
    dimensions = 0;
  }

  /**
    * Should only be called by the <code>clone()</code> method.
    *
    * @param n  The value of the <code>name</code> variable.
   **/
  protected Name(String[] n) { this(n, -1, -1); }

  /**
    * Should only be called by the <code>clone()</code> method.
    *
    * @param n  The value of the <code>name</code> variable.
   **/
  protected Name(String[] n, int line, int byteOffset) {
    super(line, byteOffset);
    name = (String[]) n.clone();
    dimensions = 0;
  }

  /**
    * Parser's constructor.
    *
    * @param token  The parser's token for the identifier.
   **/
  public Name(TokenValue token) {
    super(token.line, token.byteOffset);
    name = new String[1];
    name[0] = token.toString();
    dimensions = 0;
  }

  /**
    * Parser's constructor.
    *
    * @param n      A name that needs another identifier added to it.
    * @param token  The parser's token for the identifier.
   **/
  public Name(Name n, TokenValue token) {
    super(n.line, n.byteOffset);
    name = new String[n.name.length + 1];
    for (int i = 0; i < n.name.length; ++i) name[i] = n.name[i];
    name[n.name.length] = token.toString();
  }


  /**
    * Returns the length of the <code>name</code> array.
    *
    * @return The length of the <code>name</code> array.
   **/
  public int length() { return name.length; }


  /**
    * Returns a new <code>Name</code> object that is the same as this
    * <code>Name</code> object, except the last identifier has been removed.
   **/
  public Name cutLast() {
    String[] n = new String[name.length - 1];
    for (int i = 0; i < n.length; ++i) n[i] = name[i];
    return new Name(n);
  }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() {
    HashSet result = new HashSet();

    Type type = null;
    if (name.length == 1) type = typeCache;
    else type = symbolTable.get(name[0]);

    if (type != null
        && (type instanceof ArrayType || type instanceof ReferenceType
            || type instanceof PrimitiveType))
      result.add(new Argument(type, name[0]));

    return result;
  }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
    *
    * @param b  Flag set if this name is the name of an invoked method.
   **/
  public HashSet getVariableTypes(boolean b) {
    if (!b || name.length > 1) return getVariableTypes();
    return new HashSet();
  }


  /**
    * Determines if there are any quantified variables in this expression.
    * This method cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public boolean containsQuantifiedVariable() {
    HashSet types = getVariableTypes();
    if (types.size() == 0) return false;
    return
      ((Argument) types.iterator().next()).getType().quantifierArgumentType;
  }


  /**
    * Determines if there are any quantified variables in this expression.
    * This method cannot be run before <code>SemanticAnalysis</code> runs.
    *
    * @param b  Flag set if this name is the name of an invoked method.
   **/
  public boolean containsQuantifiedVariable(boolean b) {
    if (!b || name.length > 1) return containsQuantifiedVariable();
    return false;
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    int code = 0;
    for (int i = 0; i < name.length; ++i) code += name[i].hashCode();
    return code;
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof Name)) return false;
    Name n = (Name) o;
    if (n.name.length != name.length) return false;
    for (int i = 0; i < name.length; ++i)
      if (!name[i].equals(n.name[i])) return false;
    return true;
  }


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
  public Object clone() { return new Name(name, line, byteOffset); }


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

    if (name.length > 0) {
      buffer.append(name[0]);
      for (int i = 1; i < name.length; ++i) {
        buffer.append(".");
        buffer.append(name[i]);
      }
    }

    if (parenthesized) buffer.append(")");
  }
}

