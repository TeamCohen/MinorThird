package LBJ2.classify;

import LBJ2.learn.Lexicon;
import LBJ2.util.ByteString;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * A real feature takes on any value representable by a <code>double</code>.
  *
  * @author Nick Rizzolo
 **/
public class RealPrimitiveFeature extends RealFeature
{
  /**
    * The <code>identifier</code> string distinguishes this
    * <code>Feature</code> from other <code>Feature</code>s.
   **/
  protected ByteString identifier;
  /** The real value is represented as a double. */
  protected double value;


  /**
    * For internal use only.
    *
    * @see Feature#readFeature(ExceptionlessInputStream)
   **/
  protected RealPrimitiveFeature() { }

  /**
    * Sets both the identifier and the value.
    *
    * @param p  The new real feature's package.
    * @param c  The name of the classifier that produced this feature.
    * @param i  The new <code>RealPrimitiveFeature</code>'s identifier.
    * @param v  The new <code>RealPrimitiveFeature</code>'s value.
   **/
  public RealPrimitiveFeature(String p, String c, ByteString i, double v) {
    super(p, c);
    identifier = i;
    value = v;
  }


  /**
    * Determines if this feature contains a byte string identifier field.
    *
    * @return <code>true</code> iff this feature contains a byte string
    *         identifier field.
   **/
  public boolean hasByteStringIdentifier() { return true; }


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
  public String getStringIdentifier() { return identifier.toString(); }


  /**
    * Retrieves this feature's identifier as a byte string.
    *
    * @return This feature's identifier as a byte string.
   **/
  public ByteString getByteStringIdentifier() {
    return (ByteString) identifier.clone();
  }


  /** Simply returns the value of {@link #value}. */
  public double getStrength() { return value; }


  /**
    * Return the feature that should be used to index this feature into a
    * lexicon.  Specifically, we return this feature with a value of 0 so that
    * the same features with different real values will map to the same
    * object.
    *
    * @param lexicon  The lexicon into which this feature will be indexed.
    * @param training Whether or not the learner is currently training.
    * @param label    The label of the example containing this feature, or -1
    *                 if we aren't doing per class feature counting.
    * @return A feature object appropriate for use as the key of a map.
   **/
  public Feature getFeatureKey(Lexicon lexicon, boolean training, int label) {
    return
      new RealPrimitiveFeature(containingPackage, generatingClassifier,
                               identifier, 0);
  }


  /**
    * Returns a new feature object, the same as this one in all respects
    * except the {@link #value} field has been multiplied by the specified
    * number.
    *
    * @param m  The multiplier.
    * @return A new real feature whose value is the product of this feature's
    *         value and the specified multiplier.
   **/
  public RealFeature multiply(double m) {
    return
      new RealPrimitiveFeature(containingPackage, generatingClassifier,
                               identifier, value * m);
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
    return
      new RealPrimitiveFeature(containingPackage, generatingClassifier,
                               identifier, s);
  }


  /**
    * Returns a feature object in which any strings that are being used to
    * represent an identifier or value have been encoded in byte strings.
    *
    * @param e  The encoding to use.
    * @return A feature object as above; possible this object.
   **/
  public Feature encode(String e) { return this; }


  /**
    * The hash code of a <code>RealPrimitiveFeature</code> is the sum of the
    * hash codes of the containing package, the identifier, and the value.
    *
    * @return The hash code for this feature.
   **/
  public int hashCode() {
    return 31 * super.hashCode() + 17 * identifier.hashCode()
           + new Double(value).hashCode();
  }


  /**
    * Two <code>RealPrimitiveFeature</code>s are equivalent when their
    * containing packages and identifiers are equivalent and their values are
    * equal.
    *
    * @param o  The object with which to compare this feature.
    * @return <code>true</code> iff the parameter is an equivalent feature.
   **/
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;
    if (o instanceof RealPrimitiveFeature) {
      RealPrimitiveFeature f = (RealPrimitiveFeature) o;
      return identifier.equals(f.identifier) && value == f.value;
    }

    RealPrimitiveStringFeature f = (RealPrimitiveStringFeature) o;
    return identifier.equals(f.identifier) && value == f.value;
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
    return f instanceof RealPrimitiveStringFeature;
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
    RealPrimitiveFeature f = (RealPrimitiveFeature) o;
    d = identifier.compareTo(f.identifier);
    if (d != 0) return d;
    double difference = value - f.value;
    if (difference < 0) return -1;
    if (difference > 0) return 1;
    return 0;
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
    buffer.append(identifier.toString());
  }


  /**
    * Writes a complete binary representation of the feature.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    super.write(out);
    identifier.write(out);
    out.writeDouble(value);
  }


  /**
    * Reads the representation of a feaeture with this object's run-time type
    * from the given stream, overwriting the data in this object.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) {
    super.read(in);
    identifier = ByteString.readByteString(in);
    value = in.readDouble();
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
    identifier.lexWrite(out, bi);
    // NOTE: The lexicon has no use for a real-valued feature's value.
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
    identifier = ByteString.lexReadByteString(in, bi);
    value = 0;
  }
}

