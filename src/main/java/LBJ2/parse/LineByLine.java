package LBJ2.parse;

import java.io.BufferedReader;
import java.io.FileReader;


/**
  * This abstract <code>Parser</code> does not define the <code>next()</code>
  * method, but it does define a constructor that opens the specified file and
  * a <code>readLine()</code> method that fetches the next line of text from
  * that file, taking care of exception handling.
  *
  * @author Nick Rizzolo
 **/
public abstract class LineByLine implements Parser
{
  /** Reader for file currently being parsed. */
  protected BufferedReader in;
  /** The name of the file to parse. */
  protected String fileName;


  /** Leaves the member variables uninitialized. */
  protected LineByLine() { }

  /**
    * Creates the parser.
    *
    * @param file The name of the file to parse.
   **/
  public LineByLine(String file) {
    fileName = file;
    try { in = new BufferedReader(new FileReader(fileName)); }
    catch (Exception e) {
      System.err.println("Can't open '" + fileName + "' for input:");
      e.printStackTrace();
      System.exit(1);
    }
  }


  /**
    * Reads a line from the current buffer and returns it.  When there are no
    * more lines in the input file, the stream is closed, and
    * <code>null</code> will be returned by this method thereafter.  Returned
    * strings do not contain line termination characters.
    *
    * @return The next line of text from the input file, or <code>null</code>
    *         if no more lines remain.
   **/
  protected String readLine() {
    if (in == null) return null;

    String line = null;
    try { line = in.readLine(); }
    catch (Exception e) {
      System.err.println("Can't read from '" + fileName + "':");
      e.printStackTrace();
      System.exit(1);
    }

    if (line == null) {
      close();
      in = null;
    }

    return line;
  }


  /** Sets this parser back to the beginning of the raw data. */
  public void reset() {
    close();

    try { in = new BufferedReader(new FileReader(fileName)); }
    catch (Exception e) {
      System.err.println("Can't open '" + fileName + "' for input:");
      e.printStackTrace();
      System.exit(1);
    }
  }


  /** Frees any resources this parser may be holding. */
  public void close() {
    if (in == null) return;
    try { in.close(); }
    catch (Exception e) {
      System.err.println("Can't close '" + fileName + "':");
      e.printStackTrace();
      System.exit(1);
    }
  }
}

