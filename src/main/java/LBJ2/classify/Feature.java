package LBJ2.classify;

import java.io.IOException;
import java.io.Serializable;

import LBJ2.learn.ChildLexicon;
import LBJ2.learn.Lexicon;
import LBJ2.util.ByteString;
import LBJ2.util.ClassUtils;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;


/**
  * Objects of this class represent the value of a <code>Classifier</code>'s
  * decision.
  *
  * @author Nick Rizzolo
 **/
public abstract class Feature implements Cloneable, Comparable, Serializable
{
  /**
    * The Java <code>package</code> containing the classifier that produced
    * this feature.
   **/
  protected String containingPackage;
  /** The name of the LBJ classifier that produced this feature. */
  protected String generatingClassifier;


  /**
    * For internal use only.
    *
    * @see #readFeature(ExceptionlessInputStream)
   **/
  protected Feature() { }

  /**
    * Initializing constructor.
    *
    * @param p  The package containing the classifier that produced this
    *           feature.
    * @param c  The name of the classifier that produced this feature.
   **/
  public Feature(String p, String c) {
    containingPackage = p;
    generatingClassifier = c;
  }


  /** Retrieves this feature's package. */
  public String getPackage() { return containingPackage; }


  /** Retrieves the name of the classifier that produced this feature. */
  public String getGeneratingClassifier() { return generatingClassifier; }


  /**
    * Retrieves this feature's identifier as a string.
    *
    * @return This feature's identifier as a string.
   **/
  public abstract String getStringIdentifier();


  /**
    * Retrieves this feature's identifier as a byte string.
    *
    * @return This feature's identifier as a byte string.
   **/
  public abstract ByteString getByteStringIdentifier();


  /**
    * Gives a string representation of the value of this feature.
    *
    * @return A string representation of the value of this feature.
   **/
  public abstract String getStringValue();


  /**
    * Gives a string representation of the value of this feature.
    *
    * @return A string representation of the value of this feature.
   **/
  public abstract ByteString getByteStringValue();


  /**
    * Determines whether or not the parameter is equivalent to the string
    * representation of the value of this feature.
    *
    * @param v  The string to compare against.
    * @return <code>true</code> iff the parameter is equivalent to the string
    *         representation of the value of this feature.
   **/
  public abstract boolean valueEquals(String v);


  /**
    * Determines if this feature is discrete.
    *
    * @return <code>true</code> iff this is discrete.
   **/
  public abstract boolean isDiscrete();


  /**
    * Determines if this feature contains a byte string identifier field.
    *
    * @return <code>true</code> iff this feature contains a byte string
    *         identifier field.
   **/
  public boolean hasByteStringIdentifier() { return false; }


  /**
    * Determines if this feature contains a string identifier field.
    *
    * @return <code>true</code> iff this feature contains a string identifier
    *         field.
   **/
  public boolean hasStringIdentifier() { return false; }


  /**
    * Determines if this feature is primitive.
    *
    * @return <code>true</code> iff this is primitive.
   **/
  public boolean isPrimitive() { return false; }


  /**
    * Determines if this feature is conjunctive.
    *
    * @return <code>true</code> iff this feature is conjunctive.
   **/
  public boolean isConjunctive() { return false; }


  /**
    * Determines if this feature is a referring feature.
    *
    * @return <code>true</code> iff this feature is a referring feature.
   **/
  public boolean isReferrer() { return false; }


  /**
    * Determines if this feature comes from an array.
    *
    * @return <code>true</code> iff this feature comes from an array.
   **/
  public boolean fromArray() { return false; }


  /**
    * The depth of a feature is one more than the maximum depth of any of its
    * children, or 0 if it has no children.
    *
    * @return The depth of this feature as described above.
   **/
  public int depth() { return 0; }


  /**
    * Returns the index in the generating classifier's value list of this
    * feature's value.
    *
    * @return A non-negative integer index, or -1 if this feature is real or
    *         doesn't have a value list.
   **/
  public short getValueIndex() { return -1; }


  /**
    * Returns the total number of values this feature might possibly be set
    * to.
    *
    * @return Some integer greater than 1 iff this feature is a discrete
    *         feature with a specified value list or a conjunctive feature
    *         whose arguments have value lists, and 0 otherwise.
   **/
  public short totalValues() { return 0; }


  /**
    * If this feature is an array feature, call this method to set its array
    * length; otherwise, this method has no effect.
    *
    * @param l  The new length.
   **/
  public void setArrayLength(int l) { }


  /**
    * Returns the strength of this feature if it were to be placed in a
    * mathematical vector space.
   **/
  public abstract double getStrength();


  /**
    * Return the feature that should be used to index this feature into a
    * lexicon.  This method simply calls <code>getFeatureKey(lexicon, true,
    * -1)</code>.
    *
    * @see #getFeatureKey(Lexicon,boolean,int)
    * @param lexicon  The lexicon into which this feature will be indexed.
    * @return A feature object appropriate for use as the key of a map.
   **/
  public Feature getFeatureKey(Lexicon lexicon) {
    return getFeatureKey(lexicon, true, -1);
  }


  /**
    * Return the feature that should be used to index this feature into a
    * lexicon.
    *
    * @param lexicon  The lexicon into which this feature will be indexed.
    * @param training Whether or not the learner is currently training.
    * @param label    The label of the example containing this feature, or -1
    *                 if we aren't doing per class feature counting.
    * @return A feature object appropriate for use as the key of a map.
   **/
  public abstract Feature getFeatureKey(Lexicon lexicon, boolean training,
                                        int label);


  /**
    * Returns a {@link RealFeature} whose value is the strength of the current
    * feature, and whose <code>identifier</code> field contains all the
    * information necessary to distinguish this feature from other features.
    * When defining this method, <code>RealFeature</code>s may simply return
    * themselves.
   **/
  public abstract RealFeature makeReal();


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  public abstract Feature conjunction(Feature f, Classifier c);


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
    return new RealConjunctiveFeature(c, f, this);
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
  protected Feature conjunctWith(RealFeature f, Classifier c) {
    return new RealConjunctiveFeature(c, f, this);
  }


  /**
    * Returns a new feature object that's identical to this feature except its
    * strength is given by <code>s</code>.
    *
    * @param s  The strength of the new feature.
    * @return A new feature object as above, or <code>null</code> if this
    *         feature cannot take the specified strength.
   **/
  public abstract Feature withStrength(double s);


  /**
    * Returns a feature object in which any strings that are being used to
    * represent an identifier or value have been encoded in byte strings.
    *
    * @param e  The encoding to use.
    * @return A feature object as above; possible this object.
   **/
  public abstract Feature encode(String e);


  /**
    * Takes care of any feature-type-specific tasks that need to be taken care
    * of when removing a feature of this type from a {@link ChildLexicon}, in
    * particular updating parent counts and removing children of this feature
    * if necessary.
    *
    * @param lex  The child lexicon this feature is being removed from.
   **/
  public void removeFromChildLexicon(ChildLexicon lex) {
  }


  /**
    * Does a feature-type-specific lookup of this feature in the given
    * {@link ChildLexicon}.
    *
    * @param lex    The child lexicon this feature is being looked up in.
    * @param label  The label of the example containing this feature, or -1 if
    *               we aren't doing per class feature counting.
    * @return The index of <code>f</code> in this lexicon.
   **/
  public int childLexiconLookup(ChildLexicon lex, int label) {
    return lex.childLexiconLookup(this, label);
  }


  /**
    * The hash code of a <code>Feature</code> is a function of the hash codes
    * of {@link #containingPackage} and {@link #generatingClassifier}.
    *
    * @return The hash code of this <code>Feature</code>.
   **/
  public int hashCode() {
    return 31 * containingPackage.hashCode()
           + generatingClassifier.hashCode();
  }


  /**
    * Two <code>Feature</code>s are equal when their packages and generating
    * classifiers are equivalent.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>Feature</code>.
   **/
  public boolean equals(Object o) {
    assert (getClass() == o.getClass())
           == (getClass().getName().equals(o.getClass().getName()))
         : "getClass() doesn't behave as expected.";
    if (!(o instanceof Feature)) return false;
    Feature f = (Feature) o;
    if (getClass() != o.getClass() && !classEquivalent(f)) return false;

    assert !(f.containingPackage.equals(containingPackage)
             && f.containingPackage != containingPackage)
         : "Features \"" + f + "\" and \"" + this
           + " have equivalent package strings in different objects.";
    assert !(f.generatingClassifier.equals(generatingClassifier)
             && f.generatingClassifier != generatingClassifier)
         : "Features \"" + f + "\" and \"" + this
           + " have equivalent classifier name strings in different objects.";

    return f.containingPackage == containingPackage
           && f.generatingClassifier == generatingClassifier;
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
  public boolean classEquivalent(Feature f) { return false; }


  /**
    * Used to sort features into an order that is convenient both to page
    * through and for the lexicon to read off disk.
    *
    * @param o  An object to compare with.
    * @return Integers appropriate for sorting features first by package, then
    *         by classifier name, and then by identifier.
   **/
  public abstract int compareTo(Object o);

  /**
    * Compares only the run-time types, packages, classifier names, and
    * identifiers of the features.  This method must be overridden in order to
    * accomplish the comparison of identifiers, but the overriding method will
    * still have the convenience of calling this method to accomplish the
    * majority of the work.
    *
    * @param o  An object to compare with.
    * @return Integers appropriate for sorting features first by package, then
    *         by classifier name, and then by identifier.
   **/
  public int compareNameStrings(Object o) {
    int d = compareTypes(o);
    if (d != 0) return d;

    Feature f = (Feature) o;
    d = containingPackage.compareTo(f.containingPackage);
    if (d != 0) return d;
    return generatingClassifier.compareTo(f.generatingClassifier);
  }


  /**
    * Compares only the run-time types of the features.
    *
    * @param o  An object to compare with.
    * @return Integers appropriate for sorting features by run-time type.
   **/
  private int compareTypes(Object o) {
    if (!(o instanceof Feature)) return -1;
    Feature f = (Feature) o;

    boolean b1 = isDiscrete();
    boolean b2 = f.isDiscrete();
    int d = (b2 ? 1 : 0) - (b1 ? 1 : 0);
    if (d != 0) return d;

    int i1 = depth();
    int i2 = f.depth();
    d = i1 - i2;
    if (d != 0) return d;

    b1 = isReferrer();
    b2 = f.isReferrer();
    d = (b2 ? 1 : 0) - (b1 ? 1 : 0);
    if (d != 0) return d;

    b1 = fromArray();
    b2 = f.fromArray();
    d = (b1 ? 1 : 0) - (b2 ? 1 : 0);
    if (d != 0) return d;

    b1 = hasStringIdentifier();
    b2 = f.hasStringIdentifier();
    return (b1 ? 1 : 0) - (b2 ? 1 : 0);
  }


  /**
    * Writes a string representation of this <code>Feature</code> to the
    * specified buffer.
    *
    * @param buffer The buffer to write to.
   **/
  public abstract void write(StringBuffer buffer);


  /**
    * Writes a string representation of this <code>Feature</code>'s package,
    * generating classifier, and sometimes identifier information to the
    * specified buffer.  This method will need to be overridden to write the
    * identifier information, but at least the overriding method will have the
    * convenience of calling this method to accomplish most of the work first.
    *
    * @param buffer The buffer to write to.
   **/
  public void writeNameString(StringBuffer buffer) {
    if (containingPackage != null && containingPackage.length() > 0) {
      buffer.append(containingPackage);
      buffer.append(".");
    }
    buffer.append(generatingClassifier);
    if (hasByteStringIdentifier()) buffer.append("|B|");
  }


  /**
    * Writes a complete binary representation of the feature.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    out.writeString(getClass().getName());
    out.writeString(containingPackage);
    out.writeString(generatingClassifier);
  }


  /**
    * Writes a string representation of this <code>Feature</code> to the
    * specified buffer, omitting the package name.
    *
    * @param buffer The buffer to write to.
   **/
  public void writeNoPackage(StringBuffer buffer) {
    String p = containingPackage;
    containingPackage = null;
    write(buffer);
    containingPackage = p;
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
    String clazz = getClass().getName();
    out.writeString(clazz.equals(c) ? null : clazz);
    out.writeString(containingPackage == p ? null : containingPackage);
    out.writeString(generatingClassifier == g ? null : generatingClassifier);
    return clazz;
  }


  /**
    * Reads the binary representation of a feature of any type from the given
    * stream.  The stream is expected to first return a string containing the
    * fully qualified class name of the feature.  If the <i>short</i> value
    * <code>-1</code> appears instead, this method returns <code>null</code>.
    *
    * <p> This method is appropriate for reading features as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param in The input stream.
    * @return The feature read from the stream.
   **/
  public static Feature readFeature(ExceptionlessInputStream in) {
    String name = in.readString();
    if (name == null) return null;
    Class c = ClassUtils.getClass(name);
    Feature result = null;

    try { result = (Feature) c.newInstance(); }
    catch (Exception e) {
      System.err.println("Error instantiating feature '" + name + "':");
      e.printStackTrace();
      in.close();
      System.exit(1);
    }

    result.read(in);
    return result;
  }


  /**
    * Reads the representation of a feature with this object's run-time type
    * from the given stream, overwriting the data in this object.
    *
    * <p> This method is appropriate for reading features as written by
    * {@link #write(ExceptionlessOutputStream)}.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) {
    containingPackage = in.readString().intern();
    generatingClassifier = in.readString().intern();
  }


  /**
    * Reads the representation of a feature of any type as stored by a
    * lexicon, omitting redundant information.
    *
    * <p> This method is appropriate for reading features as written by
    * {@link #lexWrite(ExceptionlessOutputStream,Lexicon,String,String,String,String,ByteString)}.
    *
    * @param in   The input stream.
    * @param lex  The lexicon we are reading in to.
    * @param c    The assumed class.  If no class name is given in the input
    *             stream, a feature of this type is instantiated.
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
    * @return The feature read from the stream.
   **/
  public static Feature lexReadFeature(ExceptionlessInputStream in,
                                       Lexicon lex, Class c, String p,
                                       String g, String si, ByteString bi) {
    String name = in.readString();
    if (name != null) c = ClassUtils.getClass(name);
    Feature result = null;

    try { result = (Feature) c.newInstance(); }
    catch (Exception e) {
      System.err.println("Error instantiating feature '" + name + "':");
      e.printStackTrace();
      in.close();
      System.exit(1);
    }

    result.lexRead(in, lex, p, g, si, bi);
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
    containingPackage = in.readString();
    if (containingPackage == null) containingPackage = p;
    else containingPackage = containingPackage.intern();
    generatingClassifier = in.readString();
    if (generatingClassifier == null) generatingClassifier = g;
    else generatingClassifier = generatingClassifier.intern();
  }


  /** Returns a string representation of this <code>Feature</code>. */
  public String toString() {
    StringBuffer result = new StringBuffer();
    write(result);
    return result.toString();
  }


  /**
    * Returns a string representation of this <code>Feature</code> omitting
    * the package.
   **/
  public String toStringNoPackage() {
    StringBuffer result = new StringBuffer();
    writeNoPackage(result);
    return result.toString();
  }


  /** Returns a shallow clone of this <code>Feature</code>. */
  public Object clone() {
    Object result = null;

    try { result = super.clone(); }
    catch (Exception e) {
      System.err.println("Can't clone feature '" + this + "':");
      e.printStackTrace();
    }

    return result;
  }


  /**
    * Special handling during deserialization to ensure that
    * <code>Strings</code> are <code>intern()</code>ed.
    *
    * @param in The stream to deserialize from.
   **/
  private void readObject(java.io.ObjectInputStream in)
          throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    containingPackage = containingPackage.intern();
    generatingClassifier = generatingClassifier.intern();
  }
}

