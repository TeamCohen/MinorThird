package LBJ2.io;

import java.io.IOException;
import java.io.InputStream;


/**
  * This class receives input from another <code>InputStream</code> assuming
  * that data is little endian, hexidecimal text, converts that text to bytes,
  * and makes those bytes available through its interface.  The most common
  * usage of this class will involve passing it to the constructor of another
  * <code>InputStream</code>.  For instance: <br><br>
  *
  * <pre>
  *   ObjectInputStream ois =
  *     new ObjectInputStream(
  *       new GZIPInputStream(
  *         new HexInputStream(new FileInputStream(fileName))));
  * </pre>
  *
  * @see HexOutputStream
  * @author Nick Rizzolo
 **/
public class HexInputStream extends InputStream
{
  /** Characters representing a hexidecimal digit. */
  private static final String digits = "0123456789ABCDEF";

  /**
    * The <code>InputStream</code> from which yet-to-be-converted input
    * should be received.
   **/
  private InputStream in;


  /**
    * Initializes this stream with another input stream.
    *
    * @param i  The input stream from which yet-to-be-converted input should
    *           be received.
   **/
  public HexInputStream(InputStream i) { in = i; }


  /**
    * Reads the next byte of data from the input stream.  The value is
    * returned as an <code>int</code> in the range 0 to 255.  If no byte is
    * available because the end of the stream has been reached, the value -1
    * is returned.  This method blocks until input data is available, the end
    * of the stream is detected, or an exception is thrown.
    *
    * @return The next byte of data, or -1 if the end of the stream is
    *         reached.
   **/
  public int read() throws IOException {
    int d1 = in.read();
    if (d1 == -1) return -1;
    int d2 = in.read();
    if (d2 == -1)
      throw new IOException("HexInputStream: Unexpected end of file");

    int i1 = digits.indexOf((char) d1);
    if (i1 == -1)
      throw new IOException("HexInputStream: Invalid input character: '"
                            + ((char) d1) + "' (" + d1 + ")");
    int i2 = digits.indexOf((char) d2);
    if (i2 == -1)
      throw new IOException("HexInputStream: Invalid input character: '"
                            + ((char) d2) + "' (" + d2 + ")");

    return (i2 << 4) | i1;
  }


  /**
    * This method has the same effect as <code>read(b, 0, b.length)</code>.
    *
    * @param b  A buffer in which the converted input is stored.
    * @return The total number of bytes read into the buffer, or -1 if there
    *         is no more data because the end of the stream was previously
    *         reached.
   **/
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }


  /**
    * Reads up to <code>len</code> bytes of data from another input stream
    * into an array of bytes.  An attempt is made to read as many as
    * <code>len</code> bytes, but a smaller number may be read, possibly zero.
    * The number of bytes actually read is returned as an integer. <br><br>
    *
    * This method blocks until input data is available, end of file is
    * detected, or an exception is thrown. <br><br>
    *
    * If <code>b</code> is <code>null</code>, a
    * <code>NullPointerException</code> is thrown. <br><br>
    *
    * If <code>off</code> is negative, or <code>len</code> is negative, or
    * <code>off+len</code> is greater than the length of the array
    * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
    * thrown.  <br><br>
    *
    * If <code>len</code> is zero, then no bytes are read and 0 is returned;
    * otherwise, there is an attempt to read at least one byte.  If no byte is
    * available because the stream is at end of file, the value -1 is
    * returned; otherwise, at least one byte is read and stored into
    * <code>b</code>. <br><br>
    *
    * The first byte read is stored into element <code>b[off]</code>, the next
    * one into <code>b[off+1]</code>, and so on.  The number of bytes read is,
    * at most, equal to <code>len</code>.  Let <i>k</i> be the number of bytes
    * actually read; these bytes will be stored in elements
    * <code>b[off]</code> through <code>b[off+k-1]</code>, leaving elements
    * <code>b[off+k]</code> through <code>b[off+len-1]</code> unaffected.
    * <br><br>
    *
    * In every case, elements <code>b[0]</code> through <code>b[off-1]</code>
    * and elements <code>b[off+len]</code> through <code>b[b.length-1]</code>
    * are unaffected. <br><br>
    *
    * If the first byte cannot be read for any reason other than end of file,
    * then an <code>IOException</code> is thrown.  In particular, an
    * <code>IOException</code> is thrown if the input stream has been closed.
    *
    * @param b    A buffer into which the converted input is stored.
    * @param off  The offset in the buffer at which to begin writing.
    * @param len  The amount of bytes to be received and written into the
    *             buffer.
    * @return The total number of bytes read into the buffer, or -1 if there
    *         is no more data because the end of the stream has been reached.
   **/
  public int read(byte[] b, int off, int len) throws IOException {
    byte[] hex = new byte[2 * len];
    int bytesRead = in.read(hex);
    if (bytesRead == -1) return -1;
    if (bytesRead % 2 == 1)
      throw new IOException("HexInputStream: Unexpected end of file");

    for (int i = 0; i < bytesRead; i += 2) {
      int d1 = digits.indexOf((char) hex[i]);
      if (d1 == -1)
        throw new IOException("HexInputStream: Invalid input character: '"
                              + ((char) hex[i]) + "' (" + ((int) hex[i])
                              + ")");
      int d2 = digits.indexOf((char) hex[i + 1]);
      if (d2 == -1)
        throw new IOException("HexInputStream: Invalid input character: '"
                              + ((char) hex[i + 1]) + "' ("
                              + ((int) hex[i + 1]) + ")");

      b[i / 2] = (byte) ((d2 << 4) | d1);
    }

    return bytesRead / 2;
  }


  /**
    * Skips over and discards <code>n</code> bytes of data from this input
    * stream.  The skip method may, for a variety of reasons, end up skipping
    * over some smaller number of bytes, possibly 0.  This may result from any
    * of a number of conditions; reaching end of file before <code>n</code>
    * bytes have been skipped is only one possibility.  The actual number of
    * bytes skipped is returned. If <code>n</code> is negative, no bytes are
    * skipped.
    *
    * @param n  The number of bytes to be skipped.
    * @return The actual number of bytes skipped.
   **/
  public long skip(long n) throws IOException { return in.skip(n * 2); }


  /**
    * Returns the number of bytes that can be read (or skipped over) from this
    * input stream without blocking by the next caller of a method for this
    * input stream.  The next caller might be the same thread or or another
    * thread.
    *
    * @return The number of bytes that can be read from this input stream
    *         without blocking.
   **/
  public int available() throws IOException { return in.available() / 2; }


  /**
    * Closes this input stream and releases any system resources associated
    * with the stream.
   **/
  public void close() throws IOException { in.close(); }


  /**
    * Marks the current position in this input stream.  A subsequent call to
    * the <code>reset</code> method repositions this stream at the last marked
    * position so that subsequent reads re-read the same bytes.
    *
    * The <code>readlimit</code> argument tells this input stream to allow
    * that many bytes to be read before the mark position gets invalidated.
    *
    * The general contract of mark is that, if the method
    * <code>markSupported</code> returns <code>true</code>, the stream somehow
    * remembers all the bytes read after the call to mark and stands ready to
    * supply those same bytes again if and whenever the method
    * <code>reset</code> is called. However, the stream is not required to
    * remember any data at all if more than <code>readlimit</code> bytes are
    * read from the stream before <code>reset</code> is called.
    *
    * @param readlimit  The maximum limit of bytes that can be read before the
    *                   mark position becomes invalid.
   **/
  public void mark(int readlimit) { in.mark(readlimit * 2); }


  /**
    * Repositions this stream to the position at the time the
    * <code>mark</code> method was last called on this input stream.
   **/
  public void reset() throws IOException { in.reset(); }


  /**
    * Tests if this input stream supports the mark and reset methods.  Whether
    * or not <code>mark</code> and <code>reset</code> are supported is an
    * invariant property of the provided input stream instance.
    *
    * @return <code>true</code> iff the provided input stream instance
    *         supports the <code>mark</code> and <code>reset</code> methods.
   **/
  public boolean markSupported() { return in.markSupported(); }
}

