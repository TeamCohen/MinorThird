package LBJ2.parse;


/**
  * Use this parser in conjunction with another parser that returns
  * <code>LinkedVector</code>s, and this parser will return their
  * <code>LinkedChild</code>ren.
  *
  * @author Nick Rizzolo
 **/
public class ChildrenFromVectors implements Parser
{
  /** A parser that returns <code>LinkedVector</code>s. */
  protected Parser parser;
  /** The next child to be returned. */
  protected LinkedChild next;


  /**
    * Creates the parser.
    *
    * @param p  A parser that returns <code>LinkedVector</code>s.
   **/
  public ChildrenFromVectors(Parser p) { parser = p; }


  /**
    * Returns the next <code>LinkedChild</code> parsed.
    *
    * @return The next <code>LinkedChild</code> parsed, or <code>null</code>
    *         if there are no more children in the stream.
   **/
  public Object next() {
    while (next == null) {
      LinkedVector v = (LinkedVector) parser.next();
      if (v == null) return null;
      next = v.get(0);
    }

    LinkedChild result = next;
    next = next.next;
    return result;
  }


  /** Sets this parser back to the beginning of the raw data. */
  public void reset() {
    parser.reset();
    next = null;
  }


  /** Frees any resources this parser may be holding. */
  public void close() { parser.close(); }
}

