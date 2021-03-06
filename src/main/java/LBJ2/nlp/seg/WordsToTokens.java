package LBJ2.nlp.seg;

import LBJ2.nlp.Word;
import LBJ2.parse.LinkedVector;
import LBJ2.parse.Parser;


/**
  * This parser calls another parser that returns {@link LinkedVector}s of
  * {@link Word}s, converts the {@link Word}s to {@link Token}s, and returns
  * {@link LinkedVector}s of {@link Token}s.
  *
  * @author Nick Rizzolo
 **/
public class WordsToTokens implements Parser
{
  /** A parser that returns {@link LinkedVector}s of {@link Word}s. */
  protected Parser parser;


  /**
    * Creates the parser.
    *
    * @param p  A parser that returns {@link LinkedVector}s of {@link Word}s.
   **/
  public WordsToTokens(Parser p) { parser = p; }


  /**
    * Returns the next {@link LinkedVector} of {@link Token}s.
    *
    * @return The next {@link LinkedVector} of {@link Token}s parsed, or
    *         <code>null</code> if there are no more children in the stream.
   **/
  public Object next() {
    return convert((LinkedVector) parser.next());
  }


  /**
    * Given a {@link LinkedVector} containing {@link Word}s, this method
    * creates a new {@link LinkedVector} containing {@link Token}s.
    *
    * @param v  A {@link LinkedVector} of {@link Word}s.
    * @return A {@link LinkedVector} of {@link Token}s corresponding to the
    *         input {@link Word}s.
   **/
  public static LinkedVector convert(LinkedVector v) {
    if (v == null) return null;
    if (v.size() == 0) return v;

    Word w = (Word) v.get(0);
    Token t = new Token(w, null, null);
    for (w = (Word) w.next; w != null; w = (Word) w.next) {
      t.next = new Token(w, t, null);
      t = (Token) t.next;
    }

    return new LinkedVector(t);
  }


  /** Sets this parser back to the beginning of the raw data. */
  public void reset() { parser.reset(); }


  /** Frees any resources this parser may be holding. */
  public void close() { parser.close(); }
}

