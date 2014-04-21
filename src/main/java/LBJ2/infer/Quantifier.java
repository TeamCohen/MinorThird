package LBJ2.infer;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;


/**
  * A quantifier is a first order constraint parameterized by an object taken
  * from a Java <code>Collection</code> of objects.
  *
  * @author Nick Rizzolo
 **/
public abstract class Quantifier extends FirstOrderConstraint
{
  /** The name of the quantification variable. */
  protected String quantificationVariable;
  /** The collection of objects to iterate over. */
  protected Collection collection;
  /** The constraint being quantified. */
  protected FirstOrderConstraint constraint;
  /**
    * A list of the objects stored in the quantification variables of
    * enclosing quantifiers.
   **/
  protected Vector enclosingQuantificationSettings;
  /**
    * The implementation of the functions that compute any parameters this
    * quantifier may have.
   **/
  protected QuantifierArgumentReplacer replacer;


  /**
    * Initializing constructor.
    *
    * @param q    The name of the quantification variable.
    * @param col  The collection of objects to iterate over.
    * @param con  The constraint being quantified.
   **/
  public Quantifier(String q, Collection col, FirstOrderConstraint con) {
    this(q, col, con, null);
  }

  /**
    * This constructor specifies a variable setter for when this quantifier is
    * itself quantified.
    *
    * @param q    The name of the quantification variable.
    * @param col  The collection of objects to iterate over.
    * @param con  The constraint being quantified.
    * @param qar  The variable setter.
   **/
  public Quantifier(String q, Collection col, FirstOrderConstraint con,
                    QuantifierArgumentReplacer qar) {
    quantificationVariable = q;
    collection = col;
    constraint = con;
    replacer = qar;
  }


  /**
    * Returns the children of this constraint in an array.
    *
    * @return The children of this constraint in an array.
   **/
  public Constraint[] getChildren() {
    return new FirstOrderConstraint[]{ constraint };
  }


  /**
    * Makes sure that the <code>enclosingQuantificationSettings</code> vector
    * exists, then adds a place holder for this quantifier's quantification
    * variable setting.
    *
    * @return The index of this quantifier's quantification variable.
   **/
  protected int initialize() {
    if (enclosingQuantificationSettings == null)
      enclosingQuantificationSettings = new Vector();
    enclosingQuantificationSettings.add(null);
    return enclosingQuantificationSettings.size() - 1;
  }


  /**
    * Sets the variable map object stored in this object to the given
    * argument; also instantiates all quantified variables and stores them in
    * the map.
    *
    * @param m  The map in which to find unique copies of the variables.
   **/
  public void consolidateVariables(AbstractMap m) {
    int index = initialize();

    for (Iterator I = collection.iterator(); I.hasNext(); ) {
      enclosingQuantificationSettings.set(index, I.next());
      constraint.setQuantificationVariables(enclosingQuantificationSettings);
      constraint.consolidateVariables(m);
    }

    enclosingQuantificationSettings.removeElementAt(index);
  }


  /**
    * The hash code of a <code>Quantifier</code> is the sum of the hash codes
    * of its children plus three.
    *
    * @return The hash code for this <code>Quantifier</code>.
   **/
  public int hashCode() {
    int result = constraint.hashCode();
    if (replacer != null) result += replacer.hashCode();
    else result += collection.hashCode();
    return result;
  }


  /**
    * Two <code>Quantifier</code>s are equivalent when their children are
    * equivalent.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>Quantifier</code>.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof Quantifier)) return false;
    Quantifier q = (Quantifier) o;
    return replacer == q.replacer
           && (replacer != null
               || replacer == null && collection.equals(q.collection))
           && constraint.equals(q.constraint);
  }
}

