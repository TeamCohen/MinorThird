package LBJ2.parse;


/**
  * Any parser that extends this interface can be sent to a
  * <code>Learner</code> for batch training.
  *
  * @see LBJ2.learn.Learner
  * @author Nick Rizzolo
 **/
public interface Parser
{
  /**
    * Use this method to retrieve the next object parsed from the raw input
    * data.
    *
    * @return The next object parsed from the input data.
   **/
  public Object next();


  /** Sets this parser back to the beginning of the raw data. */
  public void reset();


  /** Frees any resources this parser may be holding. */
  public void close();
}

