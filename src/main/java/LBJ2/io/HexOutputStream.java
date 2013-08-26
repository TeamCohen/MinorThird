package LBJ2.io;

import java.io.IOException;
import java.io.OutputStream;


/**
  * This class will convert whatever data is sent to it into little endian,
  * hexidecimal text and send that text on to another
  * <code>OutputStream</code>.  The most common usage of this class will
  * involve passing it to the constructor of another
  * <code>OutputStream</code>.  For instance: <br><br>
  *
  * <pre>
  *   ObjectOutputStream oos =
  *     new ObjectOutputStream(
  *       new GZIPOutputStream(
  *         new HexOutputStream(new FileOutputStream(fileName))));
  * </pre>
  *
  * @see HexInputStream
  * @author Nick Rizzolo
 **/
public class HexOutputStream extends OutputStream
{
  /** Characters representing a hexidecimal digit. */
  private static final String digits = "0123456789ABCDEF";

  /**
    * The <code>OutputStream</code> to which converted output should be sent.
   **/
  private OutputStream out;


  /**
    * Initializes this stream with another output stream.
    *
    * @param o  The output stream to which converted output should be sent.
   **/
  public HexOutputStream(OutputStream o) { out = o; }


  /**
    * Writes the specified byte to this output stream.  The general contract
    * for <code>write</code> is that one byte is written to the output stream.
    * The byte to be written is the eight low-order bits of the argument
    * <code>b</code>.  The 24 high-order bits of <code>b</code> are ignored.
    *
    * @param b  The byte to be written.
   **/
  public void write(int b) throws IOException {
    b &= 255;
    out.write(digits.charAt(b & 15));
    out.write(digits.charAt((b & 240) >> 4));
  }


  /**
    * Writes <code>b.length</code> bytes from the specified byte array to this
    * output stream.  The general contract for <code>write(b)</code> is that
    * it should have exactly the same effect as the call <code>write(b, 0,
    * b.length)</code>.
    *
    * @param b  The bytes to be written.
   **/
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }


  /**
    * Writes <code>len</code> bytes from the specified byte array starting at
    * offset <code>off</code> to this output stream.  The general contract for
    * <code>write(b, off, len)</code> is that some of the bytes in the array
    * <code>b</code> are written to the output stream in order; element
    * <code>b[off]</code> is the first byte written and
    * <code>b[off+len-1]</code> is the last byte written by this operation.
    * <br><br>
    *
    * If <code>b</code> is <code>null</code>, a
    * <code>NullPointerException</code> is thrown. <br><br>
    *
    * If <code>off</code> is negative, or <code>len</code> is negative, or
    * <code>off+len</code> is greater than the length of the array
    * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
    * thrown.
    *
    * @param b    A buffer containing the bytes to be written.
    * @param off  The offset of the first byte to be written.
    * @param len  The amount of bytes to be written.
   **/
  public void write(byte[] b, int off, int len) throws IOException {
    byte[] hex = new byte[2 * len];
    for (int i = 0; i < len; ++i) {
      hex[2 * i] = (byte) digits.charAt(b[off + i] & 15);
      hex[2 * i + 1] = (byte) digits.charAt((b[off + i] & 240) >> 4);
    }

    out.write(hex);
  }


  /**
    * Flushes this output stream and forces any buffered output bytes to be
    * written out.  The general contract of <code>flush</code> is that calling
    * it is an indication that, if any bytes previously written have been
    * buffered by the implementation of the output stream, such bytes should
    * immediately be written to their intended destination.
   **/
  public void flush() throws IOException { out.flush(); }


  /**
    * Closes this output stream and releases any system resources associated
    * with this stream.  The general contract of <code>close</code> is that it
    * closes the output stream.  A closed stream cannot perform output
    * operations and cannot be reopened.
   **/
  public void close() throws IOException { out.close(); }
}

