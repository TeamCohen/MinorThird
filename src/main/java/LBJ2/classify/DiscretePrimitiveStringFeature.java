package LBJ2.classify;

import LBJ2.learn.Lexicon;
import LBJ2.util.ByteString;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * This feature is functionally equivalent to
  * {@link DiscretePrimitiveFeature}, however its {@link #value} is stored as
  * a <code>String</code> instead of a {@link ByteString}.  Discrete
  * classifiers return features of this type (or
  * {@link DiscreteConjunctiveFeature}s or {@link DiscreteReferringFeature}s
  * that contain features of this type).  Before storing these features in a
  * lexicon, however, they are converted to {@link DiscretePrimitiveFeature}s
  * using the specified encoding.
  *
  * @author Nick Rizzolo
 **/
public class DiscretePrimitiveStringFeature extends DiscreteFeature
{
  /**
    * The <code>identifier</code> string distinguishes this
    * <code>Feature</code> from other <code>Feature</code>s.
   **/
  protected String identifier;
  /** The discrete value is represented as a string. */
  protected String value;


  /**
    * For internal use only.
    *
    * @see Feature#readFeature(ExceptionlessInputStream)
   **/
  protected DiscretePrimitiveStringFeature() { }

  /**
    * Sets both the identifier and the value.  The value index and total
    * allowable values, having not been specified, default to -1 and 0
    * respectively.
    *
    * @param p  The new discrete feature's package.
    * @param c  The name of the classifier that produced this feature.
    * @param i  The new discrete feature's identifier.
    * @param v  The new discrete feature's value.
   **/
  public DiscretePrimitiveStringFeature(String p, String c, String i,
                                        String v) {
    this(p, c, i, v, (short) -1, (short) 0);
  }

  /**
    * Sets the identifier, value, value index, and total allowable values.
    *
    * @param p  The new discrete feature's package.
    * @param c  The name of the classifier that produced this feature.
    * @param i  The new discrete feature's identifier.
    * @param v  The new discrete feature's value.
    * @param vi The index corresponding to the value.
    * @param t  The total allowable values for this feature.
   **/
  public DiscretePrimitiveStringFeature(String p, String c, String i,
                                        String v, short vi, short t) {
    super(p, c, vi, t);
    identifier = i;
    value = v;
  }


  /**
    * Determines if this feature contains a string identifier field.
    *
    * @return <code>true</code> iff this feature contains a string identifier
    *         field.
   **/
  public boolean hasStringIdentifier() { return true; }


  /**
    * Determines if this feature is primitive.
    *
    * @return <code>true</code> iff this is primitive.
   **/
  public boolean isPrimitive() { return true; }


  /**
    * Retrieves this feature's identifier as a string.
    *
    * @return This feature's identifier as a string.
   **/
  public String getStringIdentifier() { return identifier; }


  /**
    * Retrieves this feature's identifier as a byte string.
    *
    * @return This feature's identifier as a byte string.
   **/
  public ByteString getByteStringIdentifier() {
    return new ByteString(identifier);
  }


  /**
    * Gives a string representation of the value of this feature.
    *
    * @return {@link #value}.
   **/
  public String getStringValue() { return value; }


  /**
    * Gives a string representation of the value of this feature.
    *
    * @return The byte string encoding of {@link #value}.
   **/
  public ByteString getByteStringValue() { return new ByteString(value); }


  /**
    * Determines whether or not the parameter is equivalent to the string
    * representation of the value of this feature.
    *
    * @param v  The string to compare against.
    * @return <code>true</code> iff the parameter is equivalent to the string
    *         representation of the value of this feature.
   **/
  public boolean valueEquals(String v) { return v.equals(value); }


  /**
    * Return the feature that should be used to index this feature into a
    * lexicon.  If it is a binary feature, we return the feature with an empty
    * value so that the feature will be mapped to the same weight whether it
    * is active or not.  If the feature can take multiple values, then simply
    * return the feature object as-is.
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
        new DiscretePrimitiveStringFeature(
              containingPackage, generatingClassifier, identifier, "",
              (short) -1, (short) 2);
    return this;
  }


  /**
    * Returns a {@link RealPrimitiveFeature} whose
    * {@link RealPrimitiveFeature#value value} field is set to the strength of
    * the current feature, and whose {@link #identifier} field contains all
    * the information necessary to distinguish this feature from other
    * features.
   **/
  public RealFeature makeReal() {
    if (totalValues == 2)
      return
        new RealPrimitiveStringFeature(
              containingPackage, generatingClassifier, identifier,
              valueIndex);
    else {
      StringBuffer id = new StringBuffer(identifier);
      id.append('_');
      id.append(value);
      return
        new RealPrimitiveStringFeature(
              containingPackage, generatingClassifier, id.toString(), 1);
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
      new DiscretePrimitiveStringFeature(
            containingPackage, generatingClassifier, identifier, "",
            (short) Math.round(s), (short) 2);
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
      new DiscretePrimitiveFeature(containingPackage, generatingClassifier,
                                   id, new ByteString(value, e), valueIndex,
                                   totalValues);
  }


  /**
    * The hash code of a <code>DiscretePrimitiveStringFeature</code> is the
    * sum of the hash codes of its containing package, identifier, and value.
    *
    * @return The hash code of this feature.
   **/
  public int hashCode() {
    return 31 * super.hashCode() + 17 * identifier.hashCode()
           + value.hashCode();
  }


  /**
    * Two <code>DiscretePrimitiveStringFeature</code>s are equivalent when
    * their containing packages, identifiers, and values are equivalent.
    *
    * @param o  The object with which to compare this feature.
    * @return <code>true</code> iff the parameter is an equivalent feature.
   **/
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;
    if (o instanceof DiscretePrimitiveStringFeature) {
      DiscretePrimitiveStringFeature f = (DiscretePrimitiveStringFeature) o;
      return identifier.equals(f.identifier)
             && valueIndex > -1 ? valueIndex == f.valueIndex
                                : value.equals(f.value);
    }

    DiscretePrimitiveFeature f = (DiscretePrimitiveFeature) o;
    return f.identifier.equals(identifier)
           && valueIndex > -1 ? valueIndex == f.valueIndex
                              : f.value.equals(value);
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
    return f instanceof DiscretePrimitiveFeature;
  }


  /**
    * Used to sort features into an order that is convenient both to page
    * through and for the lexicon to read off disk.
    *
    * @param o  An object to compare with.
    * @return Integers appropriate for sorting features first by package, then
    *         by identifier, then by value.
   **/
  public int compareTo(Object o) {
    int d = compareNameStrings(o);
    if (d != 0) return d;
    DiscretePrimitiveStringFeature f = (DiscretePrimitiveStringFeature) o;
    d = identifier.compareTo(f.identifier);
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
    buffer.append("(");
    buffer.append(value);
    buffer.append(")");
  }


  /**
    * Writes a string representation of this <code>Feature</code>'s package,
    * generating classifier, and identifier information to the specified
    * buffer.
    *
    * @param buffer The buffer to write to.
   **/
  public void writeNameString(StringBuffer buffer) {
    super.writeNameString(buffer);
    buffer.append(":");
    buffer.append(identifier);
  }


  /**
    * Writes a complete binary representation of the feature.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    out.writeString(identifier);
    out.writeString(value);
  }


  /**
    * Reads the representation of a feaeture with this object's run-time type
    * from the given stream, overwriting the data in this object.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) {
    super.read(in);
    identifier = in.readString();
    value = in.readString();
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
    out.writeString(identifier.equals(si) ? null : identifier);
    out.writeString(value);
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
    identifier = in.readString();
    if (identifier == null) identifier = si;
    value = in.readString();
  }
}

