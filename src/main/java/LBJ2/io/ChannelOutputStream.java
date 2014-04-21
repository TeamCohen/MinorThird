package LBJ2.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;


/**
  * This class implements an output stream that buffers output in a directly
  * allocated <code>ByteBuffer</code> before writing it to a channel.
  *
  * @author Nick Rizzolo
 **/
public class ChannelOutputStream extends OutputStream
{
  /** The default capacity of {@link #buffer}. */
  private static int defaultCapacity = 1 << 13;

  /** Holds data until it is written. */
  protected ByteBuffer buffer;
  /** The channel where the data will be written. */
  protected WritableByteChannel channel;


  /**
    * Creates the stream from the channel where the data will be written.
    *
    * @param out  The channel where the data will be written.
   **/
  public ChannelOutputStream(WritableByteChannel out) {
    this(out, defaultCapacity);
  }

  /**
    * Creates the stream from the channel where the data will be written and a
    * buffer size.
    *
    * @param out  The channel where the data will be written.
    * @param size The buffer size.
   **/
  public ChannelOutputStream(WritableByteChannel out, int size) {
    if (size < 0) size = 0;
    buffer = ByteBuffer.allocateDirect(size);
    channel = out;
  }


  /**
    * Writes the specified byte to this channel output stream.
    *
    * @param b  The byte to be written.
    * @exception IOException  Possible while {@link #flush}ing.
   **/
  public synchronized void write(int b) throws IOException {
    if (buffer.position() == buffer.capacity()) flush();
    buffer.put((byte) b);
  }


  /**
    * Writes <code>len</code> bytes from the specified byte array
    * starting at offset <code>off</code> to this channel output stream.
    *
    * @param b    The data.
    * @param off  The start offset in the data.
    * @param len  The number of bytes to write.
    * @exception IOException  Possible while {@link #flush}ing.
   **/
  public synchronized void write(byte[] b, int off, int len)
      throws IOException {
    int r = buffer.capacity() - buffer.position();
    if (len > r) {
      buffer.put(b, off, r);
      flush();
      off += r;
      len -= r;
      r = buffer.capacity();
    }

    while (len > r) {
      buffer.put(b, off, r);
      flush();
      off += r;
      len -= r;
    }

    if (len > 0) buffer.put(b, off, len);
  }


  /**
    * Forces any buffered output bytes to be written to {@link #channel}.
    *
    * @exception IOException  Possible while writing to {@link #channel}.
   **/
  public synchronized void flush() throws IOException {
    buffer.flip();
    channel.write(buffer);
    buffer.clear();
  }


  /**
    * Flushes the {@link #buffer} and closes the {@link #channel}.
    *
    * @exception IOException  Possible while closing {@link #channel}.
   **/
  public void close() throws IOException {
    flush();
    channel.close();
  }
}

