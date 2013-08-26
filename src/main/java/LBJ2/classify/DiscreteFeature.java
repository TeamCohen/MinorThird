package LBJ2.classify;

import LBJ2.learn.Lexicon;
import LBJ2.util.ByteString;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * A discrete feature takes on one value from a set of discontinuous values.
  * The set of values that a given <code>DiscreteFeature</code> may take is
  * defined in the <code>Classifier</code> that produced the feature.
  *
  * @author Nick Rizzolo
 **/
public abstract class DiscreteFeature extends Feature
{
  /** Convient access to a common allowable value set. */
  public static final String[] BooleanValues = { "false", "true" };


  /** Index into the set of allowable values corresponding to this value. */
  protected short valueIndex;
  /** The total number of allowable values for this feature. */
  protected short totalValues;


  /**
    * For internal use only.
    *
    * @see Feature#readFeature(ExceptionlessInputStream)
   **/
  DiscreteFeature() { }

  /**
    * Sets the identifier, value, value index, and total allowable values.
    *
    * @param p  The new discrete feature's package.
    * @param c  The name of the classifier that produced this feature.
    * @param vi The index corresponding to the value.
    * @param t  The total allowable values for this feature.
   **/
  DiscreteFeature(String p, String c, short vi, short t) {
    super(p, c);
    valueIndex = vi;
    totalValues = t;
  }


  /**
    * Determines if this feature is discrete.
    *
    * @return <code>true</code> iff this is discrete.
   **/
  public boolean isDiscrete() { return true; }


  /**
    * Returns the index in the generating classifier's value list of this
    * feature's value.
    *
    * @return A non-negative integer index, or -1 if this feature doesn't have
    *         a value list.
   **/
  public short getValueIndex() { return valueIndex; }


  /**
    * Returns the total number of values this feature might possibly be set
    * to.
    *
    * @return Some integer greater than 1 iff this feature is a discrete
    *         feature with a specified value list, and 0 otherwise.
   **/
  public short totalValues() { return totalValues; }


  /**
    * Returns the strength of this feature if it were to be placed in a
    * mathematical vector space.
   **/
  public double getStrength() { return totalValues == 2 ? valueIndex : 1; }


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  public Feature conjunction(Feature f, Classifier c) {
    return f.conjunctWith(this, c);
  }


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  protected Feature conjunctWith(DiscreteFeature f, Classifier c) {
    return new DiscreteConjunctiveFeature(c, f, this);
  }


  /**
    * Writes a complete binary representation of the feature.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeShort(valueIndex);
    out.writeShort(totalValues);
  }


  /**
    * Reads the representation of a feaeture with this object's run-time type
    * from the given stream, overwriting the data in this object.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) {
    super.read(in);
    valueIndex = in.readShort();
    totalValues = in.readShort();
  }


  /**
    * Writes a binary representation of the feature intended for use by a
    * lexicon, omitting redundant information when possible.
    *
    * @param out  The output stream.
    * @param lex  The lexicon out of which this feature is being written.
    * @param c    The fully qualified name of the assumed class.  The runtime
    *             class of this feature won't be written if it's equivalent to
    *             <code>c</code>.
    * @param p    The assumed package string.  This feature's package string
    *             won't be written if it's equivalent to <code>p</code>.
    * @param g    The assumed classifier name string.  This feature's
    *             classifier name string won't be written if it's equivalent
    *             to <code>g</code>.
    * @param si   The assumed identifier as a string.  If this feature has a
    *             string identifier, it won't be written if it's equivalent to
    *             <code>si</code>.
    * @param bi   The assumed identifier as a byte string.  If this feature
    *             has a byte string identifier, it won't be written if it's
    *             equivalent to <code>bi</code>.
    * @return The name of the runtime type of this feature.
   **/
  public String lexWrite(ExceptionlessOutputStream out, Lexicon lex, String c,
                         String p, String g, String si, ByteString bi) {
    String result = super.lexWrite(out, lex, c, p, g, si, bi);
    out.writeShort(valueIndex);
    out.writeShort(totalValues);
    return result;
  }


  /**
    * Reads the representation of a feature with this object's run-time type
    * as stored by a lexicon, overwriting the data in this object.
    *
    * <p> This method is appropriate for reading features as written by
    * {@link #lexWrite(ExceptionlessOutputStream,Lexicon,String,String,String,String,ByteString)}.
    *
    * @param in   The input stream.
    * @param lex  The lexicon we are reading in to.
    * @param p    The assumed package string.  If no package name is given in
    *             the input stream, the instantiated feature is given this
    *             package.
    * @param g    The assumed classifier name string.  If no classifier name
    *             is given in the input stream, the instantiated feature is
    *             given this classifier name.
    * @param si   The assumed identifier as a string.  If the feature being
    *             read has a string identifier field and no identifier is
    *             given in the input stream, the feature is given this
    *             identifier.
    * @param bi   The assumed identifier as a byte string.  If the feature
    *             being read has a byte string identifier field and no
    *             identifier is given in the input stream, the feature is
    *             given this identifier.
   **/
  public void lexRead(ExceptionlessInputStream in, Lexicon lex, String p,
                      String g, String si, ByteString bi) {
    super.lexRead(in, lex, p, g, si, bi);
    valueIndex = in.readShort();
    totalValues = in.readShort();
  }
}

