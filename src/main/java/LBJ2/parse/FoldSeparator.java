package LBJ2.parse;


/**
  * This is a dummy class which is only used to signify the separation between
  * folds for use in the cross validation method.  This class is ignored by
  * the LBJ compiler unless the <code>manual</code> split policy is being
  * used.  In that case, whenever the object referenced by {@link #separator}
  * is encountered, it is interpreted as a division between two folds.
  *
  * @author Dan Muriello
 **/
public class FoldSeparator
{
  /** The only instance of this class is stored here. */
  public static final FoldSeparator separator = new FoldSeparator();


  /** Blank Constructor, takes nothing, does nothing. */
  private FoldSeparator(){ }
}

