package LBJ2.classify;

import LBJ2.learn.Lexicon;
import LBJ2.util.ByteString;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * A discrete array feature keeps track of its index in the classifier's
  * returned array as well as the total number of features in that array.
  *
  * @author Nick Rizzolo
 **/
public class DiscreteArrayStringFeature extends DiscretePrimitiveStringFeature
{
  /** The feature's index in the returned array it is contained in. */
  protected int arrayIndex;
  /** The size of the returned array this feature is contained in. */
  protected int arrayLength;


  /**
    * For internal use only.
    *
    * @see Feature#readFeature(ExceptionlessInputStream)
   **/
  protected DiscreteArrayStringFeature() { }

  /**
    * Sets the identifier, value, array index, and size of the containing
    * array.  The value index and total allowable values, having not been
    * specified, default to -1 and 0 respectively.
    *
    * @param p  The new discrete feature's package.
    * @param c  The name of the classifier that produced this feature.
    * @param id The new discrete feature's identifier.
    * @param v  The new discrete feature's value.
    * @param i  The index of this feature in the returned array.
    * @param l  The length of the array this feature is contained in.
   **/
  public DiscreteArrayStringFeature(String p, String c, String id,
                                    String v, int i, int l) {
    this(p, c, id, v, (short) -1, (short) 0, i, l);
  }

  /**
    * Sets all member variables.
    *
    * @param p  The new discrete feature's package.
    * @param c  The name of the classifier that produced this feature.
    * @param id The new discrete feature's identifier.
    * @param v  The new discrete feature's value.
    * @param vi The index corresponding to the value.
    * @param t  The total allowable values for this feature.
    * @param i  The index of this feature in the returned array.
    * @param l  The length of the array this feature is contained in.
   **/
  public DiscreteArrayStringFeature(String p, String c, String id, String v,
                                    short vi, short t, int i, int l)
  {
    super(p, c, id, v, vi, t);
    arrayIndex = i;
    arrayLength = l;
  }


  /** Returns the array index of this feature. */
  public int getArrayIndex() { return arrayIndex; }


  /** Returns the length of the feature array that this feature comes from. */
  public int getArrayLength() { return arrayLength; }


  /**
    * Determines if this feature comes from an array.
    *
    * @return <code>true</code>.
   **/
  public boolean fromArray() { return true; }


  /**
    * If this feature is an array feature, call this method to set its array
    * length; otherwise, this method has no effect.
    *
    * @param l  The new length.
   **/
  public void setArrayLength(int l) { arrayLength = l; }


  /**
    * Return the feature that should be used to index this feature into a
    * lexicon.  If the feature is binary, we return the feature with an empty
    * value so that it will be mapped to the same weight whether it is active
    * or not.  If the feature can take multiple values, then simply return it
    * as-is.
    *
    * @param lexicon  The lexicon into which this feature will be indexed.
    * @param training Whether or not the learner is currently training.
    * @param label    The label of the example containing this feature, or -1
    *                 if we aren't doing per class feature counting.
    * @return A feature object appropriate for use as the key of a map.
   **/
  public Feature getFeatureKey(Lexicon lexicon, boolean training, int label) {
    if (totalValues() == 2)
      return
        new DiscreteArrayStringFeature(
              containingPackage, generatingClassifier, identifier, "",
              (short) -1, (short) 2, getArrayIndex(), 0);
    return this;
  }


  /**
    * Returns a {@link RealArrayFeature} whose
    * {@link RealPrimitiveFeature#value value} field is set to the strength of
    * the current feature, and whose {@link #identifier} field contains all
    * the information necessary to distinguish this feature from other
    * features.
   **/
  public RealFeature makeReal() {
    if (totalValues == 2)
      return
        new RealArrayStringFeature(
              containingPackage, generatingClassifier, identifier, valueIndex,
              arrayIndex, arrayLength);
    else {
      StringBuffer id = new StringBuffer(identifier);
      id.append('_');
      id.append(value);
      return
        new RealArrayStringFeature(
              containingPackage, generatingClassifier, id.toString(), 1,
              arrayIndex, arrayLength);
    }
  }


  /**
    * Returns a new feature object that's identical to this feature except its
    * strength is given by <code>s</code>.
    *
    * @param s  The strength of the new feature.
    * @return A new feature object as above, or <code>null</code> if this
    *         feature cannot take the specified strength.
   **/
  public Feature withStrength(double s) {
    if (totalValues != 2 || !(s == 0 || s == 1)) return null;
    return
      new DiscreteArrayStringFeature(
            containingPackage, generatingClassifier, identifier, "",
            (short) Math.round(s), (short) 2, arrayIndex, arrayLength);
  }


  /**
    * Returns a feature object in which any strings that are being used to
    * represent an identifier or value have been encoded in byte strings.
    *
    * @param e  The encoding to use.
    * @return A feature object as above; possible this object.
   **/
  public Feature encode(String e) {
    if (e == null || e == "String") return this;
    ByteString id =
      identifier.length() == 0 ? ByteString.emptyString
                               : new ByteString(identifier, e);
    return
      new DiscreteArrayFeature(containingPackage, generatingClassifier, id,
                               new ByteString(value, e), valueIndex,
                               totalValues, arrayIndex, arrayLength);
  }


  /**
    * The hash code of a <code>DiscreteArrayStringFeature</code> is the sum of
    * the hash codes of the containing package, the identifier, the value and
    * the array index.
    *
    * @return The hash code of this <code>Feature</code>.
   **/
  public int hashCode() { return 31 * super.hashCode() + arrayIndex; }


  /**
    * Two <code>DiscreteArrayStringFeature</code>s are equivalent when their
    * containing packages, identifiers, indices, and values are equivalent.
    *
    * @param o  The object with which to compare this <code>Feature</code>.
    * @return True iff the parameter is an equivalent <code>Feature</code>.
   **/
  public boolean equals(Object o) {
    return
      super.equals(o)
      && (o instanceof DiscreteArrayStringFeature
          ? arrayIndex == ((DiscreteArrayStringFeature) o).arrayIndex
          : arrayIndex == ((DiscreteArrayFeature) o).arrayIndex);
  }


  /**
    * Some features are functionally equivalent, differing only in the
    * encoding of their values; this method will return <code>true</code> iff
    * the class of this feature and <code>f</code> are different, but they
    * differ only because they encode their values differently.  This method
    * does not compare the values themselves, however.
    *
    * @param f  Another feature.
    * @return See above.
   **/
  public boolean classEquivalent(Feature f) {
    return f instanceof DiscreteArrayFeature;
  }


  /**
    * Used to sort features into an order that is convenient both to page
    * through and for the lexicon to read off disk.
    *
    * @param o  An object to compare with.
    * @return Integers appropriate for sorting features first by package, then
    *         by identifier, then by array index, then by value.
   **/
  public int compareTo(Object o) {
    int d = compareNameStrings(o);
    if (d != 0) return d;
    DiscreteArrayStringFeature f = (DiscreteArrayStringFeature) o;
    d = identifier.compareTo(f.identifier);
    if (d != 0) return d;
    d = arrayIndex - f.arrayIndex;
    if (d != 0) return d;
    return value.compareTo(f.value);
  }


  /**
    * Writes a string representation of this <code>Feature</code> to the
    * specified buffer.
    *
    * @param buffer The buffer to write to.
   **/
  public void write(StringBuffer buffer) {
    writeNameString(buffer);
    buffer.append("[");
    buffer.append(arrayIndex);
    buffer.append("](");
    buffer.append(value);
    buffer.append(")");
  }


  /**
    * Writes a complete binary representation of the feature.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeInt(arrayIndex);
    out.writeInt(arrayLength);
  }


  /**
    * Reads the representation of a feaeture with this object's run-time type
    * from the given stream, overwriting the data in this object.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) {
    super.read(in);
    arrayIndex = in.readInt();
    arrayLength = in.readInt();
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
    out.writeInt(arrayIndex);
    out.writeInt(arrayLength);
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
    arrayIndex = in.readInt();
    arrayLength = in.readInt();
  }
}

