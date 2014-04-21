package LBJ2.classify;

import LBJ2.learn.Lexicon;
import LBJ2.util.ByteString;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * A referring discrete feature is one that has its own identifier, but whose
  * value comes from a separate feature that it refers to.
  *
  * @author Nick Rizzolo
 **/
public class DiscreteReferringStringFeature extends DiscreteReferrer
{
  /**
    * The <code>identifier</code> string distinguishes this
    * <code>Feature</code> from other <code>Feature</code>s.
   **/
  protected String identifier;


  /**
    * For internal use only.
    *
    * @see Feature#readFeature(ExceptionlessInputStream)
   **/
  protected DiscreteReferringStringFeature() { }

  /**
    * Sets both the identifier and the referent.
    *
    * @param c  The classifier that produced this feature.
    * @param i  The new referring feature's identifier.
    * @param r  The discrete feature referred to by this new feature.
   **/
  public DiscreteReferringStringFeature(Classifier c, String i,
                                        DiscreteFeature r) {
    this(c.containingPackage, c.name, i, r, r.getValueIndex(),
         r.totalValues());
  }

  /**
    * Sets both the identifier and the referent.
    *
    * @param c  The classifier that produced this feature.
    * @param i  The new referring feature's identifier.
    * @param r  The discrete feature referred to by this new feature.
    * @param av The allowable values of the classifier that produced
    *           <code>r</code>.
   **/
  public DiscreteReferringStringFeature(Classifier c, String i,
                                        DiscreteFeature r, String[] av) {
    super(c, r, av);
    identifier = i;
  }

  /**
    * Constructs a new referring feature.
    *
    * @param p  The new discrete feature's package.
    * @param c  The name of the classifier that produced this feature.
    * @param i  The new discrete feature's identifier.
    * @param r  The discrete feature referred to by this new feature.
    * @param vi The index corresponding to the value.
    * @param t  The total allowable values for this feature.
   **/
  public DiscreteReferringStringFeature(String p, String c, String i,
                                        DiscreteFeature r, short vi, short t)
  {
    super(p, c, r, vi, t);
    identifier = i;
  }


  /**
    * Determines if this feature contains a string identifier field.
    *
    * @return <code>true</code> iff this feature contains a string identifier
    *         field.
   **/
  public boolean hasStringIdentifier() { return true; }


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
    DiscreteFeature f =
      (DiscreteFeature) referent.getFeatureKey(lexicon, training, label);
    if (training) f = (DiscreteFeature) lexicon.getChildFeature(f, label);
    else if (f == referent) return this;
    return
      new DiscreteReferringStringFeature(
            containingPackage, generatingClassifier, identifier, f,
            valueIndex, totalValues);
  }


  /**
    * Returns a {@link RealPrimitiveFeature} whose
    * {@link RealPrimitiveFeature#value value} field is set to the strength of
    * the current feature, and whose {@link #identifier} field contains all
    * the information necessary to distinguish this feature from other
    * features.
   **/
  public RealFeature makeReal() {
    return
      new RealReferringStringFeature(
            containingPackage, generatingClassifier, identifier,
            referent.makeReal());
  }


  /**
    * Returns the strength of this feature if it were to be placed in a
    * mathematical vector space.
   **/
  public double getStrength() { return referent.getStrength(); }


  /**
    * Returns a new feature object that's identical to this feature except its
    * strength is given by <code>s</code>.
    *
    * @param s  The strength of the new feature.
    * @return A new feature object as above, or <code>null</code> if this
    *         feature cannot take the specified strength.
   **/
  public Feature withStrength(double s) {
    DiscreteFeature f = (DiscreteFeature) referent.withStrength(s);
    if (f == null) return null;
    return
      new DiscreteReferringStringFeature(
            containingPackage, generatingClassifier, identifier, f,
            (short) Math.round(f.getStrength()), totalValues);
  }


  /**
    * Returns a feature object in which any strings that are being used to
    * represent an identifier or value have been encoded in byte strings.
    *
    * @param e  The encoding to use.
    * @return A feature object as above; possible this object.
   **/
  public Feature encode(String e) {
    DiscreteFeature newReferent = (DiscreteFeature) referent.encode(e);
    if (referent == newReferent && (e == null || e == "String")) return this;
    ByteString id =
      identifier.length() == 0 ? ByteString.emptyString
                               : new ByteString(identifier, e);
    return
      new DiscreteReferringFeature(
            containingPackage, generatingClassifier, id, newReferent,
            valueIndex, totalValues);
  }


  /**
    * The hash code of a <code>DiscreteReferringStringFeature</code> is the
    * sum of the hash codes of its containing package, identifier, and the
    * referent feature.
    *
    * @return The hash code of this feature.
   **/
  public int hashCode() {
    return 17 * super.hashCode() + identifier.hashCode();
  }


  /**
    * Two <code>DiscreteReferringStringFeature</code>s are equivalent when
    * their containing packages, identifiers, and referent features are
    * equivalent.
    *
    * @param o  The object with which to compare this feature.
    * @return <code>true</code> iff the parameter is an equivalent feature.
   **/
  public boolean equals(Object o) {
    return
      super.equals(o)
      && (o instanceof DiscreteReferringStringFeature
          ? identifier.equals(((DiscreteReferringStringFeature) o).identifier)
          : identifier.equals(((DiscreteReferringFeature) o).identifier))
      && referent.equals(((DiscreteReferrer) o).referent);
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
    return f instanceof DiscreteReferringFeature;
  }


  /**
    * Compares only the run-time types, packages, classifier names, and
    * identifiers of the features.
    *
    * @param o  An object to compare with.
    * @return Integers appropriate for sorting features first by package, then
    *         by classifier name, and then by identifier.
   **/
  public int compareNameStrings(Object o) {
    int d = super.compareNameStrings(o);
    if (d != 0) return d;
    return
      identifier.compareTo(((DiscreteReferringStringFeature) o).identifier);
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
  }
}

