package com.lgc.wsh.util;
import java.util.logging.*;
import java.io.*;

/** Wrap a Logger as a PrintStream.
    Useful mainly for porting code that previously
    logged to System.out or a proxy.
    Calling LoggerStream.println()
    will call Logger.info() for Level.INFO.
    A call to flush() or to a println() method
    will flush previously written text, and
    complete a call to Logger.  You may be surprised
    by extra newlines, if you call print("\n")
    and flush() instead of println();
 */
public class LoggerStream extends PrintStream {
  private Level _level = null;
  private Logger _logger = null;
  private ByteArrayOutputStream _baos = null;

  /** Wrap a Logger as a PrintStream .
      @param logger Everything written to this PrintStream
      will be passed to the appropriate method of the Logger
      @param level This indicates which method of the
      Logger should be called.
   */
  public LoggerStream(Logger logger, Level level) {
    super (new ByteArrayOutputStream(), true);
    _baos = (ByteArrayOutputStream) (this.out);
    _logger = logger;
    _level = level;
  }

  // from PrintStream
  public synchronized void flush() {
    super.flush();
    if (_baos.size() ==0) return;
    String out = _baos.toString();

    logit (out);

    _baos.reset();
  }

  // from PrintStream
  public synchronized void println() {
    flush();
  }

  // from PrintStream
  public synchronized void println(Object x) {
    super.print(x); // flush already adds a newline
    flush();
  }

  // from PrintStream
  public synchronized void println(String x) {
    super.print(x); // flush already adds a newline
    flush();
  }

  // from PrintStream
  public synchronized void close() {
    flush();
    super.close();
  }

  // from PrintStream
  public synchronized boolean checkError() {
    flush();
    return super.checkError();
  }

  private synchronized void logit(String s) {
    if (Level.CONFIG.equals(_level)) {
      _logger.config(s);
    } else if (Level.FINE.equals(_level)) {
      _logger.fine(s);
    } else if (Level.FINER.equals(_level)) {
      _logger.finer(s);
    } else if (Level.FINEST.equals(_level)) {
      _logger.finest(s);
    } else if (Level.INFO.equals(_level)) {
      _logger.info(s);
    } else if (Level.SEVERE.equals(_level)) {
      _logger.severe(s);
    } else if (Level.WARNING.equals(_level)) {
      _logger.warning(s);
    } else {
      throw new IllegalArgumentException
        ("You constructed a LoggerStream with an invalid Level "+_level);
    }
  }

  /** test code */
  public static void main(String[] args) {
    Logger logger = Logger.getLogger("com.lgc.wsh.util");
    PrintStream psInfo = new LoggerStream(logger, Level.INFO);
    PrintStream psWarning = new LoggerStream(logger, Level.WARNING);
    psInfo.print(3.);
    psInfo.println("*3.=9.");
    psWarning.print(3.);
    psWarning.println("*3.=9.");
    psInfo.print(3.);
    psInfo.flush();
    psInfo.println("*3.=9.");
    psInfo.println();
    psInfo.print("x");
    psInfo.close();
  }
}

