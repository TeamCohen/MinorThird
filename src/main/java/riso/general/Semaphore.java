package riso.general;

/** An instance of this class represents a counting semaphore.
  * The file <tt>Semaphore.java</tt> was written by Stephen Hartley;
  * see <a href="http://www.mcs.drexel.edu/~shartley/ConcProgJava/">http://www.mcs.drexel.edu/~shartley/ConcProgJava/</a>.
  * This file is released under the terms of the GNU GPL by 
  * permission of Stephen Hartley:
  *
  * <pre>
  *  Date:  Sun, 27 Jan 2002 07:47:54 -0500 (EST)
  *  From: "Stephen J. Hartley" <hartley@elvis.rowan.edu>
  *  To: "Robert Dodier" <robert_dodier@yahoo.com>
  *  Subject: Re: Permission to redistribute concurrent software?
  *  
  *  Robert,
  *  
  *    Yes, you have my permission.  Please include a header
  *  comment on the files as to their origin.  I am glad
  *  that my software was able to help you.
  *    I'd like to see the program sometime.  Can you
  *  send me a URL (when ready) to where I could look
  *  over the source code?
  *    Thanks.
  *  
  *  Steve Hartley
  *  
  *  >I am releasing RISO under the terms of the GNU 
  *  >General Public License (GPL). I would like to 
  *  >redistribute Semaphore.java and
  *  >WouldBlockException.java under the GPL. Do I have your
  *  >permission to redistribute those two files under GPL?
  * </pre>
  */
public class Semaphore {

   // if value < 0, then abs(value) is the size of the P() queue
   protected int value = 0;

   public Semaphore() {value = 0;}

   public Semaphore(int initial) {
      // Don't be silent about bad initial value; tell the user!
      if (initial < 0) throw new IllegalArgumentException("initial<0");
      value = initial;
   }

   public synchronized void P() {
      value--;
      if (value < 0) {
         while (true) {     // we must be notified not interrupted
            try {
               wait();
               break;       // notify(), so P() succeeds
            } catch (InterruptedException e) {
/*
 *  A race condition exists if a notify() occurs at about the same
 * time that a waiting thread is interrupted by some other thread
 * (a thread is interrupted if its interrupt() method is invoked).
 * Suppose that the waiting set of this semaphore object has exactly
 * one thread in it, and at about the same time, some thread wants to
 * enter V() to call notify() and some other thread calls interrupt()
 * on the waiting thread.  The waiting thread is moved from the waiting
 * set to the ready queue, where it must reacquire the semaphore object
 * lock to reenter.  One of two things can happen.  The notify() is
 * done and since the waiting set is empty, nothing happens; then the
 * interrupted thread reenters P() inside the catch block and waits
 * again.  Or, the interrupted thread reenters P() and calls wait
 * again, then gets notified.  The signal is lost in the first case
 * but not the second.  Lost signals can lead to deadlock and violation
 * of mutual exclusion.
 *  The fix is to move the `while(true)' outside the `value--;
 * if (value < 0)' and to insert `value++' just before the `continue'.
 * Alternatively, insert `if (value >= 0) break' just above `continue'.
 * This is left as an exercise for those readers whose programs use
 * interrupt().
 */
               System.err.println
                  ("Semaphore.P(): InterruptedException, wait again");
               if (value >= 0) break; // race condition fix
               else continue;         // no V() yet
            }
         }
      }
   }

   public synchronized void V() { // this technique prevents
      value++;                    // barging since any caller of
      if (value <= 0) notify();   // P() will wait even if it
   }                              // enters before signaled thread

   // do not do a `if (S.value() > 0) S.P(); else ...'
   // because there is a race condition; use S.tryP() instead
   public synchronized int value() {
      return value;
   }

   public synchronized String toString() {
      return String.valueOf(value);
   }

   public synchronized void tryP() throws WouldBlockException {
      if (value > 0) this.P();  // nested locking, but already have lock
      else throw new WouldBlockException();
   }

   public synchronized void interruptibleP() throws InterruptedException {
      value--;
      if (value < 0) {
         try { wait(); }
         catch (InterruptedException e) {
            System.err.println
               ("Semaphore.interruptibleP(): InterruptedException");
            value++;      // backout, i.e., restore semaphore value
            throw e;
         }
      }
   }
}
