package LBJ2.infer;


/**
  * Represents either an equality or an inequality between two values, a
  * classifier application and a value, or two classifier applications.
  *
  * @author Nick Rizzolo
 **/
public abstract class FirstOrderEquality extends FirstOrderConstraint
{
  /** <code>true</code> if equality, <code>false</code> if inequality. */
  protected boolean equality;
  /**
    * This object provides the implementation of the method that replaces the
    * values and variables in an equality given new settings of the
    * quantification variables; if this member variable is set to
    * <code>null</code>, it means this <code>FirstOrderEquality</code> is not
    * nested in a quantification.
   **/
  protected EqualityArgumentReplacer replacer;
  /**
    * The map that this constraint's variables have been consolidated into, or
    * <code>null</code> if variable consolidation has not been performed.
   **/
  protected java.util.AbstractMap variableMap;


  /**
    * Initializing constructor.
    *
    * @param e  Indicates whether this is an equality or an inequality.
   **/
  public FirstOrderEquality(boolean e) { this(e, null); }

  /**
    * This constructor specifies a variable setter for when this equality is
    * quantified.
    *
    * @param e  Indicates whether this is an equality or an inequality.
    * @param r  An argument replacer.
   **/
  public FirstOrderEquality(boolean e, EqualityArgumentReplacer r) {
    equality = e;
    replacer = r;
    variableMap = null;
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() { return new FirstOrderConstraint[0]; }
}

