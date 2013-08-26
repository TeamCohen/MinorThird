package LBJ2.parse;

import java.util.*;


/**
  * Useful when performing <i>k</i>-fold cross validation, this parser filters
  * the examples coming from another parser.  Conceptually, the examples from
  * the original parser are first split into <i>k</i> "folds" (or partitions)
  * depending on the selected splitting policy.  A particular fold is then
  * selected as the pivot, and this parser can be configured either to return
  * all and only the examples from that fold, or all and only the examples
  * from other folds.
  *
  * <p> The <i>k</i> folds are referred to by their indexes, which are 0, 1,
  * ..., <i>k</i> - 1.  This index is used to select the pivot fold.
  *
  * @see    FoldParser.SplitPolicy
  * @author Dan Muriello, Nick Rizzolo
 **/
public class FoldParser implements Parser
{
  /** The parser whose examples are being filtered. */
  protected Parser parser;
  /** The total number of folds. */
  protected int K;
  /** The way in which examples are partitioned into folds. */
  protected SplitPolicy splitPolicy;
  /**
    * The examples from this fold are exclusively selected for or excluded
    * from the set of examples returned by this parser.
   **/
  protected int pivot;
  /** Whether examples will come from the pivot fold or not. */
  protected boolean fromPivot;
  /** The total number of examples coming from {@link #parser}. */
  protected int examples;

  /** Keeps track of the index of the next example to be returned. */
  protected int exampleIndex;
  /** Keeps track of the current fold; used only in manual splitting. */
  protected int fold;
  /**
    * A lower bound for an index relating to the pivot fold.  The index
    * variable in question may either be {@link #exampleIndex} or
    * {@link #shuffleIndex}.
   **/
  protected int lowerBound;
  /**
    * An upper bound for an index relating to the pivot fold.  The index
    * variable in question may either be {@link #exampleIndex} or
    * {@link #shuffleIndex}.
   **/
  protected int upperBound;
  /**
    * Used only by the random splitting policy to remember which example
    * indexes are in which folds.
   **/
  protected int[] shuffled;
  /** An index pointing into {@link #shuffled}. */
  protected int shuffleIndex;


  /**
    * Constructor for when you don't know how many examples are in the data.
    * Using a constructor that allows specification of the number of examples
    * in the data only saves computation when the splitting policy is either
    * sequential or random.
    *
    * @param parser The parser whose examples are being filtered.
    * @param K      The total number of folds; this value is ignored if the
    *               splitting policy is manual.
    * @param split  The way in which examples are partitioned into folds.
    * @param pivot  The index of the pivot fold.
    * @param f      Whether to extract examples from the pivot.
   **/
  public FoldParser(Parser parser, int K, SplitPolicy split, int pivot,
                    boolean f) {
    this(parser, K, split, pivot, f, -1);
  }

  /**
    * Constructor for when you know neither how many examples are in the data
    * nor <i>K</i>, i.e., how many folds are in the data.  This constructor
    * can only be used when the splitting policy is manual.  Using a
    * constructor that allows specification of the number of examples in the
    * data only saves computation when the splitting policy is either
    * sequential or random.
    *
    * @param parser The parser whose examples are being filtered.
    * @param split  The way in which examples are partitioned into folds.
    * @param pivot  The index of the pivot fold.
    * @param f      Whether to extract examples from the pivot.
   **/
  public FoldParser(Parser parser, SplitPolicy split, int pivot, boolean f) {
    this(parser, -1, split, pivot, f, -1);
  }

  /**
    * Full constructor.
    *
    * @param parser The parser whose examples are being filtered.
    * @param K      The total number of folds; this value is ignored if the
    *               splitting policy is manual.
    * @param split  The way in which examples are partitioned into folds.
    * @param pivot  The index of the pivot fold.
    * @param f      Whether to extract examples from the pivot.
    * @param e      The total number of examples coming from
    *               <code>parser</code>, or -1 if unknown.
   **/
  public FoldParser(Parser parser, int K, SplitPolicy split, int pivot,
                    boolean f, int e) {
    this.K = K;
    splitPolicy = split;
    fromPivot = f;
    examples = e;

    if (examples == -1
        && (splitPolicy == SplitPolicy.sequential
            || splitPolicy == SplitPolicy.random)) {
      ++examples;
      for (Object example = parser.next(); example != null;
           example = parser.next())
        if (example != FoldSeparator.separator) ++examples;
      parser.reset();
    }

    if (splitPolicy == SplitPolicy.random) {
      shuffled = new int[examples];
      for (int i = 0; i < examples; ++i) shuffled[i] = i;
      Random r = new Random();

      for (int i = 0; i < examples; ++i) {
        int j = i + r.nextInt(examples - i);
        int t = shuffled[i];
        shuffled[i] = shuffled[j];
        shuffled[j] = t;
      }

      for (int i = 0; i < K; ++i) {
        setPivot(i);
        Arrays.sort(shuffled, lowerBound, upperBound);
      }
    }

    if (splitPolicy == SplitPolicy.manual) {
      this.K = 1;
      for (Object example = parser.next(); example != null;
           example = parser.next())
        if (example == FoldSeparator.separator) ++this.K;
      parser.reset();
    }

    setPivot(pivot);
    this.parser = parser;
  }


  /**
    * Retrieves the value of {@link #K}, which may have been computed in the
    * constructor if the splitting policy is manual.
   **/
  public int getK() { return K; }


  /**
    * Sets the value of {@link #fromPivot}, which controls whether examples
    * will be taken from the pivot fold or from all other folds.
    *
    * @param f  The new value for {@link #fromPivot}.
   **/
  public void setFromPivot(boolean f) { fromPivot = f; }


  /**
    * Sets the pivot fold, which also causes {@link #parser} to be reset.
    *
    * @param p  The index of the new pivot fold.
   **/
  public void setPivot(int p) {
    pivot = p;
    if (p < K) reset();
  }


  /** Returns the value of {@link #pivot}. */
  public int getPivot() { return pivot; }
  /** Returns the value of {@link #parser}. */
  public Parser getParser() { return parser; }


  /**
    * Sets this parser back to the beginning of the raw data.  This means
    * arranging for all relevant state variables to be reset appropriately as
    * well, since the value of {@link #pivot} may have changed.
    *
    * @see #setPivot(int)
   **/
  public void reset() {
    if (parser != null) parser.reset();

    if (splitPolicy == SplitPolicy.sequential
        || splitPolicy == SplitPolicy.random) {
      lowerBound = pivot * (examples / K) + Math.min(pivot, examples % K);
      upperBound =
        (pivot + 1) * (examples / K) + Math.min(pivot + 1, examples % K);
    }

    if (splitPolicy == SplitPolicy.random) shuffleIndex = lowerBound;
    if (splitPolicy == SplitPolicy.manual) fold = 0;
    exampleIndex = 0;
  }


  /**
    * Convenient for determining if the next example should be returned or
    * not.
    *
    * @param example  The next example object.
    * @return <code>true</code> iff the next example should be returned.
   **/
  protected boolean filter(Object example) {
    if (example == FoldSeparator.separator) return false;
    if (splitPolicy == SplitPolicy.sequential)
      return fromPivot
             == (exampleIndex >= lowerBound && exampleIndex < upperBound);
    if (splitPolicy == SplitPolicy.random)
      return fromPivot
             == (shuffleIndex < upperBound
                 && shuffled[shuffleIndex] == exampleIndex);
    if (splitPolicy == SplitPolicy.kth)
      return fromPivot == (exampleIndex % K == pivot);
    // splitPolicy == SplitPolicy.manual
    return fromPivot == (fold == pivot);
  }


  /**
    * Changes state to reflect retrieval of the next example from the parser.
    *
    * @param example  The previous example object.
   **/
  protected void increment(Object example) {
    if (example == FoldSeparator.separator) {
      if (splitPolicy == SplitPolicy.manual) ++fold;
    }
    else {
      if (splitPolicy == SplitPolicy.random) {
        if (shuffleIndex < upperBound
            && shuffled[shuffleIndex] == exampleIndex)
          ++shuffleIndex;
      }

      ++exampleIndex;
    }
  }


  /** Retrieves the next example object. */
  public Object next() {
    Object result = parser.next();
    for (; result != null && !filter(result); result = parser.next())
      increment(result);
    if (result != null) increment(result);
    return result;
  }


  /** Frees any resources this parser may be holding. */
  public void close() { parser.close(); }


  /**
    * Immutable type representing the way in which examples are partitioned
    * into folds.  When LBJ's self imposed restriction to use Java 1.4 is
    * lifted, this class will be replaced by an <code>enum</code>.
    *
    * <p> The four implemented splitting strategies are described below.  Note
    * that in all cases except "Manual", the size of the folds are as equal as
    * possible, with any extra examples allocated to earlier folds.
    *
    * <blockquote>
    * <dl>
    *   <dt> <b>Sequential</b> </dt>
    *   <dd> The examples are simply partitioned into sequential folds. </dd>
    *   <dt> <b>k<sup>th</sup></b> </dt>
    *   <dd> Every k<sup>th</sup> example is in the same fold. </dd>
    *   <dt> <b>Random</b> </dt>
    *   <dd> Examples are randomly assigned to folds. </dd>
    *   <dt> <b>Manual</b> </dt>
    *   <dd>
    *      Same as sequential, except fold boundaries are indicated by an
    *      appearance of the {@link FoldSeparator} in place of an example
    *      object.
    *   </dd>
    * </dl>
    * </blockquote>
    *
    * @author Nick Rizzolo
  **/
  public static class SplitPolicy
  {
    /** Represents the random split policy. */
    public static final SplitPolicy random = new SplitPolicy(0);
    /** Represents the sequential split policy. */
    public static final SplitPolicy sequential = new SplitPolicy(1);
    /**
      * Represents the split policy in which every k<sup>th</sup> example is
      * part of the same fold.
    **/
    public static final SplitPolicy kth = new SplitPolicy(2);
    /**
      * Represents the split policy in which the user manually inserts fold
      * separation objects.
    **/
    public static final SplitPolicy manual = new SplitPolicy(3);

    /** The names of the different split strategies as strings. */
    private static final String[] names =
      { "random", "sequential", "kth", "manual" };


    /** Can be used to index the {@link #names} array. */
    private int index;


    /** Initializes the object with an index. */
    private SplitPolicy(int i) { index = i; }


    /** Retrieves the name of the policy represented by this object. */
    public String toString() { return names[index]; }
  }
}

