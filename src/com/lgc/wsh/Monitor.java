package com.lgc.wsh.inv;

/** Implement this interface to receive notifications of progress  */
public interface Monitor {
  /** This instance will ignore all reports */
  public static Monitor NULL_MONITOR = new Monitor() {
      public void report(double fraction) {}};

  /** This method will be called with the current fraction
      of work done.  Values range from 0 at the beginning
      to 1 when all work is done.
  */
  public void report(double fraction);
}

