package LBJ2.IR;

import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * Represents constant values.
  *
  * @author Nick Rizzolo
 **/
public class Constant extends Expression
{
  /** (&not;&oslash;) The text representing the constant. */
  public String value;


  /**
    * Parser's constructor.
    *
    * @param token  The parser's token for the constant.
   **/
  public Constant(TokenValue token) {
    this(token.line, token.byteOffset, token.text);
  }

  /**
    * Initializing constructor.  The line and byte offset, having not been
    * specified, are both set to -1.
    *
    * @param value  The text representation of the constant.
   **/
  public Constant(String value) { this(-1, -1, value); }

  /**
    * Full constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param value      The text representation of the constant.
   **/
  public Constant(int line, int byteOffset, String value) {
    super(line, byteOffset);
    this.value = value;
  }


  /**
    * Returns the contents of <code>value</code> removing unescaped double
    * quotes.
    *
    * @return The contents of <code>value</code> with unescaped double quotes
    *         removed.
   **/
  public String noQuotes() {
    StringBuffer result = new StringBuffer(value);
    for (int i = 0; i < result.length(); ) {
      if (result.charAt(i) == '\\') i += 2;
      else if (result.charAt(i) == '"') result.deleteCharAt(i);
      else ++i;
    }

    return result.toString();
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() { return new Boolean(value).hashCode(); }


  /**
    * Two constants are equal when their <code>noQuotes()</code> methods
    * return the same thing.
    *
    * @see    Constant#noQuotes()
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    return o instanceof Constant
           && ((Constant) o).noQuotes().equals(noQuotes());
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
  public Object clone() { return new Constant(value); }


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
    buffer.append(value);
    if (parenthesized) buffer.append(")");
  }
}

