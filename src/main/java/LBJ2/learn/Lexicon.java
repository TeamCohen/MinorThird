package LBJ2.learn;

import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import LBJ2.classify.Feature;
import LBJ2.util.ByteString;
import LBJ2.util.ClassUtils;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.FVector;
import LBJ2.util.IVector;
import LBJ2.util.IVector2D;
import LBJ2.util.Sort;
import LBJ2.util.TableFormat;


/**
  * A <code>Lexicon</code> contains a mapping from {@link Feature}s to
  * integers.  The integer key of a feature is returned by the
  * {@link #lookup(Feature)} method.  If the feature is not already in the
  * lexicon, then it will be added to the lexicon, and thus lookup calls can
  * be made without the need to check if an entry already exists.  The integer
  * keys are incremented in ascending order starting from 0 as features are
  * added to the lexicon.
  *
  * <p> The map is implemented as a <code>HashMap</code> by default and the
  * <code>Lexicon</code> class has similar functionality.  This class also
  * maintains a second <code>Vector</code> of integers to their associated
  * features for fast reverse lookup using the {@link #lookupKey(int)} method.
  *
  * @author Michael Paul
 **/
public class Lexicon implements Cloneable, Serializable
{
  /**
    * The default capacity of {@link #lexiconInv} and {@link #featureCounts}.
   **/
  private static final int defaultCapacity = 1 << 10;


  /** <!-- readLexicon(String) -->
    * Reads and returns a feature lexicon from the specified file.
    *
    * @param filename The name of the file from which to read the feature
    *                 lexicon.
    * @return The lexicon.
   **/
  public static Lexicon readLexicon(String filename) {
    try { return readLexicon(new URL("file:" + filename)); }
    catch (Exception e) {
      System.err.println("Error constructing URL:");
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }


  /** <!-- readLexicon(URL) -->
    * Reads a feature lexicon from the specified location.
    *
    * @param url  The location from which to read the feature lexicon.
    * @return The lexicon.
   **/
  public static Lexicon readLexicon(URL url) {
    return readLexicon(url, true);
  }


  /** <!-- readLexicon(URL,boolean) -->
    * Reads a feature lexicon from the specified location, with the option to
    * ignore the feature counts by setting the second argument to
    * <code>false</code>.
    *
    * @param url        The location from which to read the feature lexicon.
    * @param readCounts Whether or not to read the feature counts.
    * @return The lexicon.
   **/
  public static Lexicon readLexicon(URL url, boolean readCounts) {
    ExceptionlessInputStream in =
      ExceptionlessInputStream.openCompressedStream(url);
    Lexicon result = readLexicon(in, readCounts);
    in.close();
    return result;
  }


  /** <!-- readLexicon(ExceptionlessInputStream,boolean) -->
    * Reads a feature lexicon from the specified stream.
    *
    * @param in The stream from which to read the feature lexicon.
    * @return The lexicon.
   **/
  public static Lexicon readLexicon(ExceptionlessInputStream in) {
    return readLexicon(in, true);
  }


  /** <!-- readLexicon(ExceptionlessInputStream,boolean) -->
    * Reads a feature lexicon from the specified stream, with the option to
    * ignore the feature counts by setting the second argument to
    * <code>false</code>.
    *
    * @param in         The stream from which to read the feature lexicon.
    * @param readCounts Whether or not to read the feature counts.
    * @return The lexicon.
   **/
  public static Lexicon readLexicon(ExceptionlessInputStream in,
                                    boolean readCounts) {
    String name = in.readString();
    if (name == null) return null;
    Class clazz = ClassUtils.getClass(name);

    Lexicon lexicon = null;
    try { lexicon = (Lexicon) clazz.newInstance(); }
    catch (Exception e) {
      System.err.println("Can't instantiate '" + name + "': " + e);
      System.exit(1);
    }

    lexicon.read(in, readCounts);
    return lexicon;
  }


  // Member variables.
  /** The map of features to integer keys. */
  protected Map lexicon;
  /** The inverted map of integer keys to their features. */
  protected FVector lexiconInv;
  /** The encoding to use for new features added to this lexicon. */
  private String encoding;
  /**
    * This flag remembers whether {@link #encoding} has been assigned a value
    * yet or not.  Using this flag, we enforce the constraint that once an
    * encoding has been set, it can never be changed.  This way, a user will
    * only be capable of using the same lexicon object in two different
    * learners if they have the same encoding.  See the implementation of
    * {@link Learner#setLexicon(Lexicon)}.
   **/
  private boolean encodingSet;
  /** Counts the number of occurrences of each feature. */
  protected IVector featureCounts;
  /**
    * Counts the number of occurrences of each feature on a class-by-class
    * basis.
   **/
  protected IVector2D perClassFeatureCounts;
  /**
    * Features at this index in {@link #lexiconInv} or higher have been
    * pruned.  <code>-1</code> indicates that no pruning has been done.
   **/
  protected int pruneCutoff;
  /**
    * Stores features that might appear repeatedly as children of other
    * features, but which are not themselves given indexes in the lexicon.
   **/
  protected ChildLexicon lexiconChildren;


  /** Creates an empty lexicon. */
  public Lexicon() { clear(); }

  /**
    * Creates an empty lexicon with the given encoding.
    *
    * @param e  The encoding to use when adding features to this lexicon.
   **/
  public Lexicon(String e) {
    encoding = e;
    encodingSet = true;
    clear();
  }


  /** Clears the data structures associated with this instance. */
  public void clear() {
    lexicon = new HashMap();
    lexiconInv = new FVector();
    lexiconChildren = null;
    pruneCutoff = -1;
  }


  /**
    * Sets the encoding used when adding features to this lexicon.
    *
    * @param e  The encoding.
   **/
  public void setEncoding(String e) {
    if (encodingSet && (encoding == null ? e != null : !encoding.equals(e))) {
      System.err.println(
          "LBJ ERROR: Once established, the encoding of a lexicon cannot be "
          + "changed.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    encoding = e;
    encodingSet = true;
  }


  /** Simply returns the map stored in {@link #lexicon}. */
  public Map getMap() {
    lazyMapCreation();
    return Collections.unmodifiableMap(lexicon);
  }


  /** Returns the number of features currently stored in {@link #lexicon}. */
  public int size() { return lexiconInv.size(); }
  /**
    * Returns the value of {@link #pruneCutoff}, or {@link #size()} if
    * {@link #pruneCutoff} is -1.
   **/
  public int getCutoff() { return pruneCutoff == -1 ? size() : pruneCutoff; }


  /** <!-- countFeatures(CountPolicy) -->
    * Call this method to initialize the lexicon to count feature occurrences
    * on each call to <code>lookup(feature, true)</code> (counting still won't
    * happen on a call to <code>lookup(feature, false)</code>).
    * Alternatively, this method can also cause the lexicon to discard all its
    * feature counts and cease counting features at any time in the future.
    * The former happens when <code>policy</code> is something other than
    * {@link Lexicon.CountPolicy#none}, and the latter happens when
    * <code>policy</code> is {@link Lexicon.CountPolicy#none}.
    *
    * @see #lookup(Feature,boolean)
    * @param policy The new feature counting policy.
   **/
  public void countFeatures(CountPolicy policy) {
    featureCounts = null;
    perClassFeatureCounts = null;
    if (policy == CountPolicy.global)
      featureCounts = new IVector(defaultCapacity);
    else if (policy == CountPolicy.perClass)
      perClassFeatureCounts = new IVector2D(8, defaultCapacity);
  }


  /** <!-- getCountPolicy() -->
    * Returns the feature counting policy currently employed by this lexicon.
   **/
  public CountPolicy getCountPolicy() {
    if (featureCounts != null) return CountPolicy.global;
    if (perClassFeatureCounts != null) return CountPolicy.perClass;
    return CountPolicy.none;
  }


  /** <!-- perClassToGlobalCounts() -->
    * Collapses per-class feature counts into global counts.
   **/
  public void perClassToGlobalCounts() {
    if (perClassFeatureCounts == null)
      throw new IllegalArgumentException(
          "LBJ ERROR: Lexicon.perClassToGlobalCounts: Cannot be called if "
          + "there are not per-class counts.");

    int rows = perClassFeatureCounts.size(), columns = 0;
    for (int i = 0; i < rows; ++i)
      columns = Math.max(columns, perClassFeatureCounts.size(i));
    featureCounts = new IVector(defaultCapacity);

    for (int j = 0; j < columns; ++j) {
      int count = 0;
      for (int i = 0; i < rows; ++i) count += perClassFeatureCounts.get(i, j);
      featureCounts.set(j, count);
    }

    perClassFeatureCounts = null;
  }


  /** <!-- contains(Feature) -->
    * Returns <code>true</code> if the given feature is already in the
    * lexicon (whether it's past the {@link #pruneCutoff} or not) and
    * <code>false</code> otherwise.  This does not alter or add anything to
    * the lexicon.
    *
    * @param f  The feature to look up.
    * @return A boolean indicating if the given feature is currently in the
    *         lexicon.
   **/
  public boolean contains(Feature f) {
    lazyMapCreation();
    return lexicon.containsKey(f);
  }


  /** <!-- lookup(Feature) -->
    * Looks up a feature's index by calling <code>lookup(f, false)</code>.
    * See {@link #lookup(Feature,boolean,int)} for more details.
    *
    * @param f  The feature to look up.
    * @return The integer key that the feature maps to.
   **/
  public int lookup(Feature f) { return lookup(f, false, -1); }


  /** <!-- lookup(Feature,boolean) -->
    * Looks up a feature's index by calling <code>lookup(f, training,
    * -1)</code>.  See {@link #lookup(Feature,boolean,int)} for more details.
    *
    * @param f        The feature to look up.
    * @param training Whether or not the learner is currently training.
    * @return The integer key that the feature maps to.
   **/
  public int lookup(Feature f, boolean training) {
    return lookup(f, training, -1);
  }


  /** <!-- lookup(Feature,boolean,int) -->
    * Looks up the given feature in the lexicon, possibly counting it and/or
    * expanding the lexicon to accomodate it.  Feature counting and automatic
    * lexicon expansion happen when <code>training</code> is
    * <code>true</code>.  Otherwise, <code>f</code> is not counted even if
    * already in the lexicon, and a previously unobserved feature will cause
    * this method to return the value of {@link #getCutoff()} without
    * expanding the lexicon to accomodate the new feature.
    *
    * @param f        The feature to look up.
    * @param training Whether or not the learner is currently training.
    * @param label    The label of the example containing this feature, or -1
    *                 if we aren't doing per class feature counting.
    * @return The integer key that the feature maps to.
   **/
  public int lookup(Feature f, boolean training, int label) {
    if (label < 0) {
      if (training && perClassFeatureCounts != null)
        throw new IllegalArgumentException(
            "LBJ ERROR: Lexicon.lookup: Must supply a label when training "
            + "with per class feature counts.");
    }
    else if (!training || perClassFeatureCounts == null)
      throw new IllegalArgumentException(
          "LBJ ERROR: Lexicon.lookup: A label has been supplied when not "
          + "training with per class feature counts.");

    lazyMapCreation();
    Integer I = (Integer) lexicon.get(f);

    if (I == null) {
      if (!training) return getCutoff();

      f = f.encode(encoding);

      if (lexiconChildren != null) {
        Feature c = lexiconChildren.remove(f);
        if (c != null) f = c;
      }

      int key = lexiconInv.size();
      lexicon.put(f, new Integer(key));
      lexiconInv.add(f);
      incrementCount(key, label);
      return key;
    }

    int index = I.intValue();
    if (training) incrementCount(index, label);
    return index;
  }


  /**
    * Used to lookup the children of conjunctive and referring features during
    * training, this method checks {@link #lexiconChildren} if the feature
    * isn't present in {@link #lexicon} and {@link #lexiconInv}, and then
    * stores the given feature in {@link #lexiconChildren} if it wasn't
    * present anywhere.
    *
    * @param f      The feature to look up.
    * @param label  The label of the example containing this feature, or -1 if
    *               we aren't doing per class feature counting.
    * @return A feature equivalent to <code>f</code> that is stored in this
    *         lexicon.
   **/
  public Feature getChildFeature(Feature f, int label) {
    lazyMapCreation();
    Integer I = (Integer) lexicon.get(f);
    if (I != null) {
      int index = I.intValue();
      incrementCount(index, label);
      return lexiconInv.get(index);
    }

    if (lexiconChildren == null) lexiconChildren = new ChildLexicon(this);
    return lexiconChildren.getChildFeature(f, -1);
  }


  /**
    * Increments the count of the feature with the given index(es).
    *
    * @param index  The index of the feature.
    * @param label  The label of the example containing this feature, which is
    *               ignored if we aren't doing per class feature counting.
   **/
  protected void incrementCount(int index, int label) {
    if (featureCounts != null) featureCounts.increment(index);
    else if (perClassFeatureCounts != null)
      perClassFeatureCounts.increment(label, index);
  }


  /**
    * Used to lookup the children of conjunctive and referring features while
    * writing the lexicon, this method checks {@link #lexiconChildren} if the
    * feature isn't present in {@link #lexicon} and {@link #lexiconInv}, and
    * will throw an exception if it still can't be found.
    *
    * @param f  The feature to look up.
    * @return If the feature was found in {@link #lexicon}, its associated
    *         integer index is returned.  Otherwise, <code>-i - 1</code> is
    *         returned, where <code>i</code> is the index associated with the
    *         feature in {@link #lexiconChildren}.
    * @throws UnsupportedOperationException If the feature isn't found
    *                                       anywhere in the lexicon.
   **/
  public int lookupChild(Feature f) {
    lazyMapCreation();
    Integer I = (Integer) lexicon.get(f);
    if (I != null) return I.intValue();

    if (lexiconChildren == null)
      throw
        new UnsupportedOperationException(
            "When calling Lexicon.lookupChild(Feature), the feature must be "
            + "present in the lexicon.");

    return -lexiconChildren.lookupChild(f) - 1;
  }


  /** <!-- lookupKey(int) -->
    * Does a reverse lexicon lookup and returns the {@link Feature} associated
    * with the given integer key, and <code>null</code> if no such feature
    * exists.
    *
    * @param i  The integer key to look up.  If <code>i</code> is negative,
    *           {@link #lexiconChildren} is queried instead of
    *           {@link #lexiconInv}.
    * @return The feature that maps to the given integer.
   **/
  public Feature lookupKey(int i) {
    if (i < 0) return lexiconChildren.lookupKey(-i - 1);
    return lexiconInv.get(i);
  }


  /** <!-- isPruned(int,PruningPolicy) -->
    * Determines if the given feature index should be pruned according to the
    * given pruning policy, which must have its thresholds set already in the
    * case that it represents the "Percentage" policy.  This method behaves
    * equivalently to <code>isPruned(i, -1, p)</code>.
    *
    * @see #isPruned(int,int,Lexicon.PruningPolicy)
    * @param i      The feature index.
    * @param policy The pruning policy.
    * @return <code>true</code> iff the feature should be pruned.
   **/
  public boolean isPruned(int i, PruningPolicy policy) {
    return isPruned(i, -1, policy);
  }

  /** <!-- isPruned(int,int,PruningPolicy) -->
    * Determines if the given feature index should be pruned according to the
    * given pruning policy, which must have its thresholds set already in the
    * case that it represents the "Percentage" policy.  The second argument to
    * this method represents the label of the example in which the specified
    * feature appeared.  It is ignored unless per class feature counts are
    * present.  If they are, then when the specified label is -1, all counts
    * for the given feature must be greater than or equal to the corresponding
    * threshold for this method to return <code>true</code>.  When per class
    * feature counts are present and the label is non-negative, only the count
    * corresponding to that label must be greater than or equal to its
    * corresonding threshold.
    *
    * <p> In other words, passing -1 in the second argument gives the behavior
    * expected when pruning the lexicon as in
    * {@link #prune(Lexicon.PruningPolicy)}.  Passing a non-negative label in
    * the second argument gives the behavior expected when pruning the actual
    * examples.
    *
    * @param i      The feature index.
    * @param label  The label of the example containing this feature, or -1 if
    *               we want the lexicon pruning behavior.
    * @param policy The pruning policy.
    * @return <code>true</code> iff the feature should be pruned.
   **/
  public boolean isPruned(int i, int label, PruningPolicy policy) {
    if (policy.isNone()) return false;

    if (featureCounts == null && perClassFeatureCounts == null)
      throw new IllegalArgumentException(
          "LBJ ERROR: Lexicon.isPruned: pruning policy wasn't 'None', but "
          + "there are no counts.");

    if (featureCounts != null)  // if global counting
      return featureCounts.get(i) < policy.getThreshold(0);
    // otherwise, per class counting
    if (label >= 0)
      return
        perClassFeatureCounts.get(label, i) < policy.getThreshold(label);
    for (int j = 0; j < perClassFeatureCounts.size(); ++j)
      if (perClassFeatureCounts.get(j, i) >= policy.getThreshold(j))
        return false;
    return true;
  }


  /** <!-- prune(PruningPolicy) -->
    * Rearranges the order in which features appear in the lexicon based on
    * the compiled feature counts in {@link #featureCounts} or
    * {@link #perClassFeatureCounts} so that pruned features are at the end of
    * the feature space.  This way, learning algorithms can allocate exactly
    * enough space in their weight vectors for the unpruned features.
    *
    * <p> This method returns an array of integers which is a permutation of
    * the integers from 0 (inclusive) to the number of features in the lexicon
    * (exclusive).  It represents a map from the features' original indexes to
    * their new ones after pruning.  The {@link #getCutoff()} method then
    * returns the new index of the first pruned feature (or, equivalently, the
    * number of unpruned features).  All features with a new index greater
    * than or equal to this index are considered pruned in the case of global
    * pruning.  In the case of per-class pruning, the cutoff represents the
    * first feature whose count fell below the threshold for <i>every</i>
    * class.  Thus, in this case, features below the cutoff may still be
    * pruned in any given class; just not all of them.
    *
    * @param policy The type of pruning to perform.
    * @return A map from features' original indexes to their new ones, or
    *         <code>null</code> if <code>policy</code> indicates no pruning.
   **/
  public int[] prune(PruningPolicy policy) {
    if (policy.isNone()) {
      pruneCutoff = -1;
      return null;
    }

    if (featureCounts == null && perClassFeatureCounts == null)
      throw new UnsupportedOperationException(
          "LBJ ERROR: Lexicon.prune: Can't prune if there's no feature "
          + "counts.");

    // Set thresholds in the policy.
    if (policy.isPercentage()) {
      if (featureCounts != null) {  // if global counting
        long t =
          Math.round(Math.ceil(featureCounts.max() * policy.getPercentage()));
        policy.setThresholds(new int[]{ (int) t });
      }
      else {  // if per class counting
        int[] thresholds = new int[perClassFeatureCounts.size()];
        int size = perClassFeatureCounts.size();
        double p = policy.getPercentage();
        for (int i = 0; i < size; ++i)
          thresholds[i] =
            (int) Math.round(Math.ceil(perClassFeatureCounts.max(i) * p));
        policy.setThresholds(thresholds);
      }
    }
    // there's no clause for policy.isAbsolute() here since the appropriate
    // threshold must already be established in that case.
    else if (!policy.isAbsolute())
      throw new UnsupportedOperationException(
          "LBJ ERROR: Lexicon.prune: Pruning policy '" + policy
          + "' is not supported.");

    // Swap features around, remembering how it was done in swapMap.
    pruneCutoff = size();
    int[] swapMap = new int[pruneCutoff];

    // If features at the end of the space are pruned, there's no need to swap
    // anything; just decrement pruneCutoff.
    while (pruneCutoff > 0 && isPruned(pruneCutoff - 1, policy)) {
      --pruneCutoff;
      swapMap[pruneCutoff] = pruneCutoff;
    }

    // Now we know the feature just below the prune cutoff does not need to be
    // pruned (otherwise it would have been handled by the loop above), so we
    // start the loop at pruneCutoff - 2 and do swaps for any feature that
    // needs to be pruned.
    if (pruneCutoff > 0) swapMap[pruneCutoff - 1] = pruneCutoff - 1;

    for (int i = pruneCutoff - 2; i >= 0; --i) {
      if (isPruned(i, policy)) {
        pruneCutoff--;

        Feature pruned = lexiconInv.get(i);
        Feature f = lexiconInv.get(pruneCutoff);
        if (lexicon != null)
          lexicon.put(pruned, lexicon.put(f, new Integer(i)));
        lexiconInv.set(i, f);
        lexiconInv.set(pruneCutoff, pruned);

        if (featureCounts != null)
          featureCounts.set(i,
                            featureCounts.set(pruneCutoff,
                                              featureCounts.get(i)));
        else {
          for (int j = 0; j < perClassFeatureCounts.size(); ++j)
            perClassFeatureCounts.set(
                j, i,
                perClassFeatureCounts.set(j, pruneCutoff,
                                          perClassFeatureCounts.get(j, i)));
        }

        swapMap[i] = swapMap[pruneCutoff];
        swapMap[pruneCutoff] = i;
      }
      else swapMap[i] = i;
    }

    // Invert swapMap.
    // swapMap[i] currently stores the original index of the feature whose new
    // index is i.  but we want the inverse: swapMap[i] should store the new
    // index of the feature whose original index was i.  we also don't want to
    // allocate another array as long as swapMap, even if it's only around
    // temporarily.  so we do this:

    for (int i = 0; i < swapMap.length; ) {
      int newIndex = 0, j = i;

      do {
        int original = swapMap[j];
        swapMap[j] = -newIndex;
        newIndex = j;
        j = original;
      } while (j != i);

      swapMap[i] = newIndex;
      for (i++; i < swapMap.length && swapMap[i] <= 0; ++i)
        swapMap[i] = -swapMap[i];
    }

    return swapMap;
  }


  /**
    * Permanently discards any features that have been pruned via
    * {@link #prune(Lexicon.PruningPolicy)} as well as all feature counts.
   **/
  public void discardPrunedFeatures() {
    if (pruneCutoff == -1) return;
    featureCounts = null;
    perClassFeatureCounts = null;
    for (int i = lexiconInv.size() - 1; i >= pruneCutoff; --i) {
      Feature f = lexiconInv.remove(i);
      if (lexicon != null) lexicon.remove(f);
    }
    lexiconInv = new FVector(lexiconInv);
    pruneCutoff = -1;
  }


  /** <!-- clone() -->
    * Returns a deep clone of this lexicon implemented as a
    * <code>HashMap</code>.
   **/
  public Object clone() {
    Lexicon clone = null;
    try { clone = (Lexicon) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning Lexicon: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    if (lexicon != null) {
      clone.lexicon = new HashMap();
      clone.lexicon.putAll(lexicon);
    }
    clone.lexiconInv = (FVector) lexiconInv.clone();
    if (featureCounts != null)
      clone.featureCounts = (IVector) featureCounts.clone();
    if (perClassFeatureCounts != null)
      clone.perClassFeatureCounts = (IVector2D) perClassFeatureCounts.clone();
    if (lexiconChildren != null)
      clone.lexiconChildren = (ChildLexicon) lexiconChildren.clone();

    return clone;
  }


  /** Returns whether the given Lexicon object is equal to this one. */
  public boolean equals(Object o) {
    if (!o.getClass().equals(getClass())) return false;
    Lexicon l = (Lexicon) o;
    return
      pruneCutoff == l.pruneCutoff
      && (lexicon == null ? l.lexicon == null : lexicon.equals(l.lexicon))
      && (featureCounts == null
          ? l.featureCounts == null : featureCounts.equals(l.featureCounts))
      && (perClassFeatureCounts == null
          ? l.perClassFeatureCounts == null
          : perClassFeatureCounts.equals(l.perClassFeatureCounts))
      && (lexiconChildren == null
          ? l.lexiconChildren == null
          : lexiconChildren.equals(l.lexiconChildren));
  }


  /** Returns a hash code for this lexicon. */
  public int hashCode() { return lexiconInv.hashCode(); }


  /** <!-- write(ExceptionlessOutputStream) -->
    * Writes a binary representation of the lexicon.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    out.writeString(getClass().getName());
    if (lexiconChildren == null) out.writeString(null);
    else lexiconChildren.write(out);

    final FVector inverse = lexiconInv;
    int[] indexes = new int[inverse.size()];
    for (int i = 0; i < indexes.length; ++i) indexes[i] = i;
    Sort.sort(indexes,
              new Sort.IntComparator() {
                public int compare(int i1, int i2) {
                  return inverse.get(i1).compareTo(inverse.get(i2));
                }
              });

    String previousClassName = null;
    String previousPackage = null;
    String previousClassifier = null;
    String previousSIdentifier = null;
    ByteString previousBSIdentifier = null;
    out.writeInt(indexes.length);
    out.writeInt(pruneCutoff);

    for (int i = 0; i < indexes.length; ++i) {
      Feature f = inverse.get(indexes[i]);
      previousClassName =
        f.lexWrite(out, this, previousClassName, previousPackage,
                   previousClassifier, previousSIdentifier,
                   previousBSIdentifier);
      previousPackage = f.getPackage();
      previousClassifier = f.getGeneratingClassifier();
      if (f.hasStringIdentifier())
        previousSIdentifier = f.getStringIdentifier();
      else if (f.hasByteStringIdentifier())
        previousBSIdentifier = f.getByteStringIdentifier();

      out.writeInt(indexes[i]);
    }

    if (featureCounts == null) out.writeInt(0);
    else featureCounts.write(out);
    if (perClassFeatureCounts == null) out.writeInt(0);
    else perClassFeatureCounts.write(out);
  }


  /** <!-- read(ExceptionlessInputStream) -->
    * Reads the binary representation of a lexicon from the specified stream,
    * overwriting the data in this object.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) { read(in, true); }


  /** <!-- read(ExceptionlessInputStream,boolean) -->
    * Reads the binary representation of a lexicon from the specified stream,
    * overwriting the data in this object.  This method also gives the option
    * to ignore any feature counts stored after the feature mappings by
    * setting the second argument to <code>false</code>.
    *
    * @param in         The input stream.
    * @param readCounts Whether or not to read the feature counts.
   **/
  public void read(ExceptionlessInputStream in, boolean readCounts) {
    lexiconChildren = (ChildLexicon) Lexicon.readLexicon(in, readCounts);

    Class previousClass = null;
    String previousPackage = null;
    String previousClassifier = null;
    String previousSIdentifier = null;
    ByteString previousBSIdentifier = null;
    int N = in.readInt();
    pruneCutoff = in.readInt();
    lexicon = null;
    lexiconInv = new FVector(N);

    for (int i = 0; i < N; ++i) {
      Feature f =
        Feature.lexReadFeature(in, this, previousClass, previousPackage,
                               previousClassifier, previousSIdentifier,
                               previousBSIdentifier);
      int index = in.readInt();
      lexiconInv.set(index, f);

      previousClass = f.getClass();
      previousPackage = f.getPackage();
      previousClassifier = f.getGeneratingClassifier();
      if (f.hasStringIdentifier())
        previousSIdentifier = f.getStringIdentifier();
      else if (f.hasByteStringIdentifier())
        previousBSIdentifier = f.getByteStringIdentifier();
    }

    if (readCounts) {
      featureCounts = new IVector();
      featureCounts.read(in);
      if (featureCounts.size() == 0) featureCounts = null;
      perClassFeatureCounts = new IVector2D();
      perClassFeatureCounts.read(in);
      if (perClassFeatureCounts.size() == 0) perClassFeatureCounts = null;
    }
    else {
      featureCounts = null;
      perClassFeatureCounts = null;
    }

    if (lexiconChildren != null) lexiconChildren.setParent(this);
  }


  /**
    * Various other methods in this class call this method to ensure that
    * {@link #lexicon} is populated before performing operations on it.  The
    * only reason it wouldn't be is if it had just been read off disk.
   **/
  protected void lazyMapCreation() {
    if (lexicon == null) {
      lexicon = new HashMap();
      int N = lexiconInv.size();
      for (int i = 0; i < N; ++i)
        lexicon.put(lexiconInv.get(i), new Integer(i));
    }
  }


  /** <!-- readPrunedSize(ExceptionlessInputStream) -->
    * Reads the value of {@link #pruneCutoff} from the specified stream,
    * discarding everything else.
    *
    * @param in The input stream.
   **/
  public static int readPrunedSize(ExceptionlessInputStream in) {
    in.readInt();
    return in.readInt();
  }


  /** Returns a text representation of this lexicon (for debugging). */
  public String toString() {
    StringBuffer result = new StringBuffer();

    for (int i = 0; i < lexiconInv.size(); ++i) {
      result.append(", ");
      result.append(i);
      result.append(": ");
      result.append(lexiconInv.get(i).toString());
    }

    if (lexiconInv.size() > 0) return result.substring(2);
    return result.toString();
  }


  /** <!-- printCountTable(boolean) -->
    * Produces on <code>STDOUT</code> a table of feature counts including a
    * line indicating the position of {@link #pruneCutoff}.  It's probably not
    * a good idea to call this method unless you know your lexicon is small.
    *
    * @param p  Whether or not to include package names in the output.
   **/
  public void printCountTable(boolean p) {
    int rows = lexiconInv.size();
    String[] rowLabels = new String[rows];
    String[] columnLabels = null;
    double[][] data = null;
    int[] sigDigits = null;
    int[] dashRows = { 0, pruneCutoff };

    if (featureCounts != null) {
      data = new double[rows][2];

      for (int i = 0; i < rows; ++i) {
        data[i][0] = i;
        data[i][1] = featureCounts.get(i);
        rowLabels[i] =
          p ? lexiconInv.get(i).toString()
            : lexiconInv.get(i).toStringNoPackage();
      }

      columnLabels = new String[]{ "Index", "Count" };
      sigDigits = new int[2];
    }
    else if (perClassFeatureCounts != null) {
      int columns = perClassFeatureCounts.size() + 1;
      data = new double[rows][columns];

      for (int i = 0; i < rows; ++i) {
        data[i][0] = i;
        for (int j = 0; j < columns - 1; ++j)
          data[i][j + 1] = perClassFeatureCounts.get(j, i);
        rowLabels[i] =
          p ? lexiconInv.get(i).toString()
            : lexiconInv.get(i).toStringNoPackage();
      }

      columnLabels = new String[columns];
      columnLabels[0] = "Index";
      for (int i = 1; i < columns; ++i) columnLabels[i] = "Label " + (i - 1);
      sigDigits = new int[columns];
    }
    else {
      data = new double[rows][1];
      for (int i = 0; i < rows; ++i) {
        data[i][0] = i;
        rowLabels[i] =
          p ? lexiconInv.get(i).toString()
            : lexiconInv.get(i).toStringNoPackage();
      }

      columnLabels = new String[]{ "Index" };
      sigDigits = new int[1];
    }

    TableFormat.printTableFormat(System.out, columnLabels, rowLabels, data,
                                 sigDigits, dashRows);
  }


  // main(String[])
  public static void main(String[] args) {
    String filename = null;
    boolean p = true;

    try {
      filename = args[0];
      if (args.length == 2) p = Boolean.parseBoolean(args[1]);
      if (args.length > 2) throw new Exception();
    }
    catch (Exception e) {
      System.out.println(
        "usage: java LBJ2.learn.Lexicon <lex file> [<package names = true>]");
      System.exit(1);
    }

    Lexicon lexicon = readLexicon(filename);
    lexicon.printCountTable(p);
    if (lexicon.lexiconChildren != null) {
      System.out.println("\nChildren:");
      lexicon.lexiconChildren.printCountTable(p);
    }
  }


  /** <!-- class CountPolicy -->
    * Immutable type representing the feature counting policy of a lexicon.
    * When LBJ's self imposed restriction to use Java 1.4 is lifted, this
    * class will be replaced by an <code>enum</code>.
    *
    * <p> The three feature counting policies are described below.
    *
    * <blockquote>
    * <dl>
    *   <dt> <b>None</b> </dt>
    *   <dd> Features occurrences are not counted. </dd>
    *   <dt> <b>Global</b> </dt>
    *   <dd>
    *     The lexicon stores one integer count per feature, and every
    *     occurrence of the feature adds to this count regardless of the
    *     example it appears in.
    *   </dd>
    *   <dt> <b>Per Class</b> </dt>
    *   <dd>
    *     The lexicon stores one integer count for each (feature, prediction
    *     class) pair.  When a given feature appears in example, this
    *     occurrence adds to the count associated with the example's label,
    *     assuming that examples have a single discrete label.
    *   </dd>
    * </dl>
    * </blockquote>
    *
    * @author Nick Rizzolo
  **/
  public static class CountPolicy
  {
    /** Represents no counting. */
    public static final CountPolicy none = new CountPolicy(0);
    /** Represents global counting. */
    public static final CountPolicy global = new CountPolicy(1);
    /** Represents per class counting. */
    public static final CountPolicy perClass = new CountPolicy(2);

    /** The names of the different counting policies as strings. */
    private static final String[] names = { "none", "global", "per class" };


    /** Can be used to index the {@link #names} array. */
    private int index;


    /** Initializes the object with an index. */
    private CountPolicy(int i) { index = i; }


    /** Retrieves the name of the policy represented by this object. */
    public String toString() { return names[index]; }
  }


  /** <!-- class PruningPolicy -->
    * Represents the feature counting policy of a lexicon.  Objects of this
    * type are used to identify and describe a desired pruning policy.  In
    * particular, the description of a pruning policy includes feature count
    * thresholds which sometimes need to be computed in terms of data.  Space
    * is allocated within objects of this type for storing these thresholds
    * whenever they are computed.
    *
    * <p> The three pruning policies are described below.
    *
    * <blockquote>
    * <dl>
    *   <dt> <b>None</b> </dt>
    *   <dd> No pruning is performed. </dd>
    *   <dt> <b>Absolute</b> </dt>
    *   <dd>
    *     Features whose counts within a given dataset fall below an absolute
    *     threshold are pruned from that dataset.
    *   </dd>
    *   <dt> <b>Percentage</b> </dt>
    *   <dd>
    *     Features whose counts within a given dataset are lower than a given
    *     percentage of the most common feature's count are pruned from that
    *     dataset.
    *   </dd>
    * </dl>
    * </blockquote>
    *
    * @author Nick Rizzolo
   **/
  public static class PruningPolicy
  {
    /** Represents no pruning. */
    public static final int NONE = 0;
    /** Represents pruning with an absolute threshold. */
    public static final int ABSOLUTE = 1;
    /** Represents pruning with a percentage threshold. */
    public static final int PERCENTAGE = 2;

    /** The names of the different counting policies as strings. */
    private static final String[] names =
      { "none", "absolute", "percentage" };


    /** Can be used to index the {@link #names} array. */
    private int index;
    /**
      * The percentage associated with the "Percentage" policy described
      * above.
     **/
    private double percentage;
    /**
      * Feature count thresholds which may either be specified by the policy
      * explicitly or computed in terms of data.
     **/
    private int[] thresholds;


    /** Creates a new pruning policy in which no features will be pruned. */
    public PruningPolicy() { index = NONE; }

    /**
      * Creates a new "Percentage" policy with the given percentage.
      *
      * @param p  The percentage.
     **/
    public PruningPolicy(double p) {
      index = PERCENTAGE;
      percentage = p;
    }

    /**
      * Creates a new "Absolute" policy with the given threshold.
      *
      * @param t  The threshold.
     **/
    public PruningPolicy(int t) {
      index = ABSOLUTE;
      thresholds = new int[]{ t };
    }


    /** <code>true</code> iff the policy is no pruning. */
    public boolean isNone() { return index == NONE; }
    /** <code>true</code> iff the policy is absolute thresholding. */
    public boolean isAbsolute() { return index == ABSOLUTE; }
    /** <code>true</code> iff the policy is percentage thresholding. */
    public boolean isPercentage() { return index == PERCENTAGE; }


    /**
      * Use this method to establish feature count thresholds in the
      * "Percentage" policy.
      *
      * @param t  The new feature count thresholds.
     **/
    public void setThresholds(int[] t) {
      if (index != PERCENTAGE)
        throw new UnsupportedOperationException(
            "LBJ ERROR: Lexicon.PruningPolicy.setThresholds should not be "
            + "called unless the policy is 'Percentage'.");
      thresholds = (int[]) t.clone();
    }


    /**
      * Returns the value of the <code>i</code><sup>th</sup> threshold in
      * {@link #thresholds} when in "Percentage" mode, but ignores the
      * parameter <code>i</code> and returns the first element of
      * {@link #thresholds} when in "Absolute" mode.
      *
      * @param i  An index.
      * @return A feature count threshold.
     **/
    public int getThreshold(int i) {
      if (index == NONE)
        throw new UnsupportedOperationException(
            "LBJ ERROR: Lexicon.PruningPolicy.getThreshold should never be "
            + "called if the pruning policy is 'None'.");
      if (index == ABSOLUTE) return thresholds[0];
      return thresholds[i];
    }


    /** Returns the value of {@link #percentage}. */
    public double getPercentage() {
      if (index != PERCENTAGE)
        throw new UnsupportedOperationException(
            "LBJ ERROR: PruningPolicy: Can't get percentage when pruning "
            + "policy isn't 'Percentage'.");
      return percentage;
    }


    /** Retrieves the name of the policy represented by this object. */
    public String toString() {
      String result = names[index];
      if (index == PERCENTAGE) result += "(" + percentage + ")";
      if (index != NONE && thresholds != null) {
        result += ": [";
        for (int i = 0; i < thresholds.length; ++i)
          result += thresholds[i] + (i + 1 < thresholds.length ? ", " : "");
        result += "]";
      }
      return result;
    }
  }
}

