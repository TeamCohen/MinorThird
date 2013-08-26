package LBJ2.learn;

import LBJ2.classify.DiscreteConjunctiveFeature;
import LBJ2.classify.DiscreteReferrer;
import LBJ2.classify.Feature;
import LBJ2.classify.RealConjunctiveFeature;
import LBJ2.classify.RealReferrer;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.IVector;


/**
  * Instances of this class are intended to store features that are children
  * of other features and which do not correspond to their own weights in any
  * learner's weight vector.  While a {@link Lexicon} will store an instance
  * of this class in its {@link Lexicon#lexiconChildren} field, an instance of
  * this class will never do so.  Also, the {@link #lookupChild(Feature)}
  * method behaves differently in this class, since it is assumed that
  * children are stored here.
 **/
public class ChildLexicon extends Lexicon
{
  /**
    * The elements of this vector (which correspond to the features in
    * {@link #lexiconInv}) serve a dual purpose; first, to indicate by
    * absolute value the number of other features currently stored in this
    * object that have the corresponding feature as a child, and second, to
    * indicate by sign if the corresponding feature has been marked for
    * removal.
   **/
  private IVector parents;  // Initialization happens in clear()
  /**
    * A reference to the lexicon that uses this lexicon as its child lexicon.
   **/
  private Lexicon parentLexicon;


  /** Creates an empty lexicon. */
  public ChildLexicon() { }

  /**
    * Creates an empty lexicon.
    *
    * @param p  The lexicon that uses this lexicon as its child lexicon.
   **/
  public ChildLexicon(Lexicon p) { parentLexicon = p; }
  // Lexicon's constructor will call clear(), so there's no need to initialize
  // parents.

  /**
    * Creates an empty lexicon with the given encoding.
    *
    * @param p  The lexicon that uses this lexicon as its child lexicon.
    * @param e  The encoding to use when adding features to this lexicon.
   **/
  public ChildLexicon(Lexicon p, String e) {
    super(e);
    // The super constructor will call clear(), so there's no need to
    // initialize parents.
    parentLexicon = p;
  }


  /** Clears the data structures associated with this instance. */
  public void clear() {
    super.clear();
    parents = new IVector();
  }


  /**
    * Sets the value of {@link #parentLexicon} and makes sure that any
    * features marked for removal in this lexicon are the identical objects
    * also present in the parent.  This is useful in particular just after
    * lexicons have been read from disk.
    *
    * @param p  The new parent lexicon.
   **/
  public void setParent(Lexicon p) {
    parentLexicon = p;
    int N = lexiconInv.size();

    for (int i = 0; i < N; ++i) {
      Feature f = lexiconInv.get(i);

      if (f != null && parents.get(i) < 0) {
        Feature pf = p.lookupKey(p.lookup(f));
        if (pf == null) {
          System.err.println("LBJ ERROR: Can't find feature " + f
                             + " in parent lexicon.");
          new Exception().printStackTrace();
          System.exit(1);
        }

        lexiconInv.set(i, pf);
        if (lexicon != null) lexicon.put(pf, lexicon.remove(f));
      }
    }
  }


  /**
    * Removes the mapping for the given feature from this lexicon and returns
    * the feature object representing it that was stored here.
    *
    * @param f  The feature to remove.
    * @return The representation of <code>f</code> that used to be stored
    *         here, or <code>null</code> if it wasn't present.
   **/
  public Feature remove(Feature f) {
    if (contains(f)) {  // contains(Feature) calls lazyMapCreation()
      int index = lookup(f);
      int count = parents.get(index);
      if (count == 0) {
        f.removeFromChildLexicon(this); // Calls decrementParentCounts
        lexicon.remove(f);
        return lexiconInv.set(index, null);
      }
      else if (count > 0) {
        parents.set(index, -count);
        return lexiconInv.get(index);
      }
      else {
        System.err.println(
            "LBJ ERROR: Marking feature as removable for the second time: "
            + f);
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

    return null;
  }


  /**
    * The parent of feature <code>f</code> is being removed, so we decrement
    * <code>f</code>'s parent counts and remove it if it's ready.
    *
    * @param f  The child feature whose parent counts need updating and which
    *           may be removed as well.
   **/
  public void decrementParentCounts(Feature f) {
    int index = lookup(f);
    int count = parents.get(index);

    if (count == 0) {
      System.err.println(
          "LBJ ERROR: Parent count incorrect for feature " + f);
      new Exception().printStackTrace();
      System.exit(1);
    }
    else if (count < 0) {
      parents.increment(index);
      if (count == -1) {
        f.removeFromChildLexicon(this);
        lexicon.remove(f);
        lexiconInv.set(index, null);
      }
    }
    else parents.decrement(index);
  }


  /**
    * This method adds the given feature to this lexicon and also recursively
    * adds its children, if any.
    *
    * @param f      The feature to look up.
    * @param label  The label of the example containing this feature, or -1 if
    *               we aren't doing per class feature counting.
    * @return A feature equivalent to <code>f</code> that is stored in this
    *         lexicon.
   **/
  public Feature getChildFeature(Feature f, int label) {
    return lexiconInv.get(f.childLexiconLookup(this, label));
  }


  /**
    * Updates the counts in {@link #parents} for the children of
    * <code>f</code>.
    *
    * @param f      The feature to look up.
    * @param label  The label of the example containing this feature, or -1 if
    *               we aren't doing per class feature counting.
    * @return The index of <code>f</code> in this lexicon.
   **/
  public int childLexiconLookup(Feature f, int label) {
    return lookup(f, true, label);
  }


  /**
    * Updates the counts in {@link #parents} for the children of
    * <code>f</code>.
    *
    * @param f      The feature to look up.
    * @param label  The label of the example containing this feature, or -1 if
    *               we aren't doing per class feature counting.
    * @return The index of <code>f</code> in this lexicon.
   **/
  public int childLexiconLookup(DiscreteConjunctiveFeature f, int label) {
    int oldSize = lexiconInv.size();
    int result = lookup(f, true, label);
    if (oldSize < lexiconInv.size()) {
      incrementParentCounts(f.getLeft(), label);
      incrementParentCounts(f.getRight(), label);
    }
    return result;
  }


  /**
    * Updates the counts in {@link #parents} for the children of
    * <code>f</code>.
    *
    * @param f      The feature to look up.
    * @param label  The label of the example containing this feature, or -1 if
    *               we aren't doing per class feature counting.
    * @return The index of <code>f</code> in this lexicon.
   **/
  public int childLexiconLookup(RealConjunctiveFeature f, int label) {
    int oldSize = lexiconInv.size();
    int result = lookup(f, true, label);
    if (oldSize < lexiconInv.size()) {
      incrementParentCounts(f.getLeft(), label);
      incrementParentCounts(f.getRight(), label);
    }
    return result;
  }


  /**
    * Updates the counts in {@link #parents} for the children of
    * <code>f</code>.
    *
    * @param f      The feature to look up.
    * @param label  The label of the example containing this feature, or -1 if
    *               we aren't doing per class feature counting.
    * @return The index of <code>f</code> in this lexicon.
   **/
  public int childLexiconLookup(DiscreteReferrer f, int label) {
    int oldSize = lexiconInv.size();
    int result = lookup(f, true, label);
    if (oldSize < lexiconInv.size())
      incrementParentCounts(f.getReferent(), label);
    return result;
  }


  /**
    * Updates the counts in {@link #parents} for the children of
    * <code>f</code>.
    *
    * @param f      The feature to look up.
    * @param label  The label of the example containing this feature, or -1 if
    *               we aren't doing per class feature counting.
    * @return The index of <code>f</code> in this lexicon.
   **/
  public int childLexiconLookup(RealReferrer f, int label) {
    int oldSize = lexiconInv.size();
    int result = lookup(f, true, label);
    if (oldSize < lexiconInv.size())
      incrementParentCounts(f.getReferent(), label);
    return result;
  }


  /**
    * Helper method for methods like
    * {@link #childLexiconLookup(DiscreteConjunctiveFeature,int)} that
    * actually does the work of looking up the child feature and updating its
    * parent counts.
    *
    * @param f      The feature to look up.
    * @param label  The label of the example containing this feature, or -1 if
    *               we aren't doing per class feature counting.
   **/
  protected void incrementParentCounts(Feature f, int label) {
    int index = f.childLexiconLookup(this, label);
    // Increment count while preserving sign to indicate mark for removal.
    if (parents.get(index) == 0)
      parents.set(index, parentLexicon.contains(f) ? -1 : 1);
    else if (parents.get(index) > 0) parents.increment(index);
    else parents.decrement(index);
  }


  /**
    * Unlike the overridden method in {@link Lexicon}, this method simply
    * checks {@link #lexicon} for the feature and will throw an exception if
    * it can't be found.
    *
    * @param f  The feature to look up.
    * @return If the feature was found in {@link #lexicon}, its associated
    *         integer index is returned.
    * @throws UnsupportedOperationException If the feature isn't found
    *                                       anywhere in the lexicon.
   **/
  public int lookupChild(Feature f) {
    lazyMapCreation();
    Integer I = (Integer) lexicon.get(f);
    if (I != null) return I.intValue();
    throw
      new UnsupportedOperationException(
          "When calling ChildLexicon.lookupChild(Feature), the feature must "
          + "be present in the lexicon. (" + f + ")");
  }


  /** <!-- write(ExceptionlessOutputStream) -->
    * Writes a binary representation of the lexicon.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    int size = lexiconInv.size();
    int n = 0; while (n < size && lexiconInv.get(n) != null) ++n;
    int i = n; while (i < size && lexiconInv.get(i) == null) ++i;
    while (i < size) {
      parents.set(n++, parents.get(i++));
      while (i < size && lexiconInv.get(i) == null) ++i;
    }

    size = parents.size();
    if (n < size) {
      for (i = size - 1; i >= n; --i) parents.remove(i);
      parents = new IVector(parents);
    }

    lexiconInv.consolidate();
    lexicon = null;
    super.write(out);
    parents.write(out);
  }


  /** <!-- read(ExceptionlessInputStream) -->
    * Reads a binary representation of the lexicon.
    *
    * @param in         The input stream.
    * @param readCounts Whether or not to read the feature counts.
   **/
  public void read(ExceptionlessInputStream in, boolean readCounts) {
    super.read(in, readCounts);
    parents.read(in);
  }


  /** <!-- printCountTable(boolean) -->
    * Produces on <code>STDOUT</code> a table of feature counts including a
    * line indicating the position of {@link #pruneCutoff}.  It's probably not
    * a good idea to call this method unless you know your lexicon is small.
    *
    * @param p  Whether or not to include package names in the output.
   **/
  public void printCountTable(boolean p) {
    featureCounts = parents;
    super.printCountTable(p);
    featureCounts = null;
  }
}

