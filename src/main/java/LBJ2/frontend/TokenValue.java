package LBJ2.frontend;


/**
  * Objects of this class are returned by LBJ2's scanner to its parser.  It
  * simply holds some information about the token and provides easy access to
  * a few primitive parsing routines.
  *
  * @author Nick Rizzolo
 **/
public class TokenValue
{
  /** The line on which the token is found in the source file. */
  public int line;
  /**
    * The byte offset in the file at which the token is found in the source
    * file.
   **/
  public int byteOffset;
  /** The text in the source file that comprises the token. */
  public String text;
  /** The name of the source file. */
  public String filename;

  /** Default constructor.  Does nothing. */
  TokenValue() { }

  /**
    * Full constructor.
    *
    * @param text       The text in the source that comprises the token.
    * @param line       The line on which the token is found in the source.
    * @param byteOffset The byte offset in the file at which the token is
    *                   found in the source.
    * @param filename   The name of the source file.
   **/
  TokenValue(String text, int line, int byteOffset, String filename) {
    this.text = text;
    this.line = line;
    this.byteOffset = byteOffset;
    this.filename = filename;
  }

  /**
    * Return the token's text in a <code>String</code>.
    *
    * @return The token's text.
   **/
  public String toString() { return text; }

  /**
    * Attempts to parse the token's text as if it represented an integer.
    *
    * @return The integer that the token's text represents.
   **/
  public int toInt() { return Integer.parseInt(text); }

  /**
    * Attempts to parse the token's text as if it represented an integer.
    *
    * @return The integer that the token's text represents.
   **/
  public long toLong() { return Long.parseLong(text); }

  /**
    * Attempts to parse the token's text as if it represented a double
    * precision floating point value.
    *
    * @return The double precision floating point value that the token's text
    *         represents.
   **/
  public double toFloat() { return Float.parseFloat(text); }

  /**
    * Attempts to parse the token's text as if it represented a double
    * precision floating point value.
    *
    * @return The double precision floating point value that the token's text
    *         represents.
   **/
  public double toDouble() { return Double.parseDouble(text); }

  /**
    * Attempts to parse the token's text as if it represented a boolean value.
    *
    * @return The boolean value that the token's text represents.
   **/
  public boolean toBoolean() { return text.equals("true"); }

  /**
    * Attempts to parse the token's text as if it represented a character
    * value.
    *
    * @return The character value that the token's text represents.
   **/
  public String toChar() { return text.substring(1, text.length() - 1); }
}

