package com.lgc.wsh.inv;
import java.util.logging.*;
import java.util.Date;
import java.util.logging.*;

/** Report progress to default Logger */
public class LogMonitor implements Monitor {
  private double _lastFraction = -1.;
  private Logger _log = null;
  private String _prefix = "";
  private long _time = 0;
  private long _lastTime = 0;

  private static final Logger LOG
    = Logger.getLogger(LogMonitor.class.getPackage().getName());

  /** Progress will be reported to this Logger.
      @param prefix Prefix this string to every report.
      @param logger Send to this Logger.  If null,
      then check arguments but do nothing.
   */
  public LogMonitor(String prefix, Logger logger) {
    _log = logger;
    if (prefix != null)
      _prefix = prefix;
  }

  public void report(double fraction) {
    if (fraction < _lastFraction-0.001) {
      throw new IllegalStateException
        ("Progress cannot decrease from "+_lastFraction+" to "+fraction);
    }
    if (_time == 0) {
      _time = System.currentTimeMillis();
    }
    long time =0;
    if ((fraction > _lastFraction + 0.02 &&
         (time=System.currentTimeMillis()) > _lastTime + 10000)
        || (fraction == 1. &&
            fraction > _lastFraction &&
            (time=System.currentTimeMillis()) > 0) // may not have been done
        ) {
      String progress = "";
      if (_lastFraction >= 0. && fraction > .01) {
        long secSoFar = (long) ((time - _time)/1000.);
        long secRemaining = (long) ((1./fraction -1.)*((time - _time)/1000.));
        long total = secSoFar + secRemaining;
        progress = time(secSoFar);
        if (progress.length() > 0)
          progress = progress+" so far";
        if (secRemaining > 0) {
          String remaining = time(secRemaining) + " remaining";
          if (progress.length() > 0)
            progress = remaining+", "+progress;
          else
            progress = remaining;
        }
        if (progress.length() > 0) {
          progress = "\n  "+progress+", "+time(total)+" total";
        }
        if (fraction >= 1.) {
          progress = "\n  Finished in "+time(total)+" total";
        }
      }
      if (_log != null)
        _log.info(_prefix+" progress: "+((int)(100.*fraction+0.49))+
                  "% complete at "+(new Date())+progress);
      _lastFraction = fraction;
      _lastTime = time;
    }
  }

  private static String time(long seconds) {
    if (seconds == 0) return "";
    String result = "";
    long minutes = seconds/60;
    long hours = minutes/60;
    seconds %= 60;
    minutes %= 60;
    if (seconds != 0)
      result = " " + seconds + " second"+ ((seconds>1)?"s":"") + result;
    if (minutes != 0)
      result = " " + minutes + " minute"+ ((minutes>1)?"s":"") + result;
    if (hours != 0)
      result = " " + hours + " hour" + ((hours>1)?"s":"") + result;
    return result.trim();
  }

  public static void main(String[] a) throws Exception {
    Monitor monitor = new LogMonitor("Test",LOG);
    int n=300;
    for (int i=0; i<n; ++i) {
      monitor.report((double)i/(n-1.));
      Thread.sleep(25);
    }
  }
}

