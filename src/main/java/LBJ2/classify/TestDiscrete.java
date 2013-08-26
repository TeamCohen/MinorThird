package LBJ2.classify;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import LBJ2.parse.Parser;
import LBJ2.util.ClassUtils;
import LBJ2.util.TableFormat;
import LBJ2.learn.Learner;
import LBJ2.learn.Lexicon;


/**
  * This class is a program that can evaluate any <code>Classifier</code>
  * against an oracle <code>Classifier</code> on the objects returned from a
  * <code>Parser</code>.
  *
  * <p> Usage:
  * <blockquote>
  *   <code>
  *     java LBJ2.classify.TestDiscrete [-t &lt;n&gt;] &lt;classifier&gt;
  *                                     &lt;oracle&gt; &lt;parser&gt;
  *                                     &lt;input file&gt; [&lt;null label&gt;
  *                                     [&lt;null label&gt; ...]]
  *   </code>
  * </blockquote>
  *
  * <p> <b>Options:</b> The <code>-t &lt;n&gt;</code> option is similar to the
  * LBJ compiler's command line option of the same name.  When
  * <code>&lt;n&gt;</code> is greater than 0, a time stamp is printed to
  * <code>STDOUT</code> after every <code>&lt;n&gt;</code> examples are
  * processed.
  *
  * <p> <b>Input:</b> The first three command line parameters are fully
  * qualified class names, e.g.  <code>myPackage.myClassifier</code>.
  * Next, <code>&lt;input file&gt;</code> is passed (as a <code>String</code>)
  * to the constructor of <code>&lt;parser&gt;</code>.  The optional parameter
  * <code>&lt;null label&gt;</code> identifies one of the possible labels
  * produced by <code>&lt;oracle&gt;</code> as representing "no
  * classification".  It is used during the computation of overall precision,
  * recall, and F1 scores.  Finally, it is also assumed that
  * <code>&lt;classifier&gt;</code> is discrete, and that its
  * <code>discreteValue(Object)</code> method is implemented.
  *
  * <p> <b>Output:</b> First some timing information is presented.  The first
  * time reported is the time taken to load the specified classifier's Java
  * class into memory.  This reflects the time taken for LBJ to load the
  * classifier's internal representation <b>if</b> the classifier does
  * <b>not</b> make use of the <code>cachedin</code> keyword.  Next, the time
  * taken to evaluate the first example is reported.  It isn't particularly
  * informative unless the classifier <b>does</b> make use of the
  * <code>cachedin</code> keyword.  In this case, it reflects the time LBJ
  * takes to load the classifier's internal representation better than the
  * first time reported.  Finally, the average time taken to execute the
  * classifier's <code>discreteValue(Object)</code> method is reported.
  *
  * <p> After the timing information, an ASCII table is written to
  * <code>STDOUT</code> reporting precision, recall, and F<sub>1</sub> scores
  * itemized by the values that either the classifier or the oracle produced
  * during the test.  The two rightmost columns are named
  * <code>"LCount"</code> and <code>"PCount"</code> (standing for "labeled
  * count" and "predicted count" respectively), and they report the number of
  * times the oracle produced each label and the number of times the
  * classifier predicted each label respectively.  If a "null label" is
  * specified, overall precision, recall, and F<sub>1</sub> scores and a total
  * count of non-null-labeled examples are reported at the bottom of the
  * table.  In the last row, whether a "null label" is specified or not,
  * overall accuracy is reported in the precision column.  In the count
  * column, the total number of predictions (or labels, equivalently) is
  * reported.
 **/
public class TestDiscrete
{
  /** References the classifier that is to be tested. */
  private static Classifier classifier;
  /** References the oracle classifier to test against. */
  private static Classifier oracle;
  /** References the parser supplying the testing objects. */
  private static Parser parser;
  /** The number of examples processed in between time stamp messages. */
  private static int outputGranularity;


  /**
    * The entry point of this program.
    *
    * @param args The command line parameters.
   **/
  public static void main(String[] args) {
    long totalTime = -System.currentTimeMillis();
    TestDiscrete tester = instantiate(args);
    totalTime += System.currentTimeMillis();
    System.out.println("Classifier loaded in " + (totalTime / 1000.0)
                       + " seconds.");
    testDiscrete(tester, classifier, oracle, parser, true, outputGranularity);
  }


  /**
    * Tests the given discrete classifier against the given oracle using the
    * given parser to provide the labeled testing data.  This simplified
    * interface to
    * {@link #testDiscrete(TestDiscrete,Classifier,Classifier,Parser,boolean,int)}
    * assumes there are no null predictions and that output should not be
    * generated on <code>STDOUT</code>.
    *
    * @param classifier The classifier to be tested.
    * @param oracle     The classifier to test against.
    * @param parser     The parser supplying the labeled example objects.
    * @return A new <code>TestDiscrete</code> object filled with testing
    *         statistics.
   **/
  public static TestDiscrete testDiscrete(Classifier classifier,
                                          Classifier oracle, Parser parser) {
    return
      testDiscrete(new TestDiscrete(), classifier, oracle, parser, false, 0);
  }


  /**
    * Tests the given discrete classifier against the given oracle using the
    * given parser to provide the labeled testing data.  If the parser returns
    * examples as <code>Object[]</code>s containing arrays of
    * <code>int</code>s and <code>double</code>s, as would be the case if
    * pre-extraction was performed, then it is assumed that this example array
    * already includes the label, so this is used directly and the oracle
    * classifier is ignored.  In this case, it is also assumed that the given
    * discrete classifier is an instance of <code>Learner</code> and thus
    * a lexicon of label mappings can be retrieved from it.
    *
    * @param tester     An object of this class that has already been told via
    *                   {@link #addNull(String)} which prediction values are
    *                   considered to be null predictions.
    * @param classifier The classifier to be tested.
    * @param oracle     The classifier to test against.
    * @param parser     The parser supplying the labeled example objects.
    * @param output     Whether or not to produce output on
    *                   <code>STDOUT</code>.
    * @param outputGranularity
    *                   The number of examples processed in between time stamp
    *                   messages.
    * @return The same <code>TestDiscrete</code> object passed in the first
    *         argument, after being filled with statistics.
   **/
  public static TestDiscrete testDiscrete(TestDiscrete tester,
                                          Classifier classifier,
                                          Classifier oracle,
                                          Parser parser,
                                          boolean output,
                                          int outputGranularity) {
    int processed = 1;
    long totalTime = 0;
    Lexicon labelLexicon = null;
    Runtime runtime = null;
    boolean preExtraction = false;

    if (output && outputGranularity > 0) {
      runtime = Runtime.getRuntime();
      System.out.println("0 examples tested at " + new Date());
      System.out.println("Total memory before first example: "
                         + runtime.totalMemory());
      Object example = parser.next();
      if (example == null) return tester;

      totalTime -= System.currentTimeMillis();
      String prediction = classifier.discreteValue(example);
      totalTime += System.currentTimeMillis();
      System.out.println("First example processed in " + (totalTime / 1000.0)
                         + " seconds.");
      System.out.println("Total memory after first example: "
                         + runtime.totalMemory());

      String gold;
      if (example instanceof Object[]
          && ((Object[]) example)[0] instanceof int[]) {
        preExtraction = true;
        labelLexicon = ((Learner) classifier).getLabelLexicon();
        gold =
          ((Feature)
           labelLexicon.lookupKey(((int[]) ((Object[]) example)[2])[0]))
          .getStringValue();
      }
      else gold = oracle.discreteValue(example);

      tester.reportPrediction(prediction, gold);

      for (example = parser.next(); example != null;
           example = parser.next(), ++processed) {
        if (processed % outputGranularity == 0)
          System.out.println(processed + " examples tested at " + new Date());

        totalTime -= System.currentTimeMillis();
        prediction = classifier.discreteValue(example);
        totalTime += System.currentTimeMillis();
        assert prediction != null
             : "Classifier returned null prediction for example " + example;

        if (preExtraction)
          gold =
            ((Feature)
             labelLexicon.lookupKey(((int[]) ((Object[]) example)[2])[0]))
            .getStringValue();
        else gold = oracle.discreteValue(example);

        tester.reportPrediction(prediction, gold);
      }

      System.out.println(processed + " examples tested at " + new Date()
                         + "\n");
    }
    else {
      if (output) {
        runtime = Runtime.getRuntime();
        System.out.println("Total memory before first example: "
                           + runtime.totalMemory());
      }

      Object example = parser.next();
      if (example == null) return tester;

      totalTime -= System.currentTimeMillis();
      String prediction = classifier.discreteValue(example);
      totalTime += System.currentTimeMillis();
      if (output) {
        System.out.println("First example processed in "
                           + (totalTime / 1000.0) + " seconds.");
        System.out.println("Total memory after first example: "
                           + runtime.totalMemory());
      }

      String gold;
      if (example instanceof Object[]
          && ((Object[]) example)[0] instanceof int[]) {
        preExtraction = true;
        labelLexicon = ((Learner) classifier).getLabelLexicon();
        gold =
          ((Feature)
           labelLexicon.lookupKey(((int[]) ((Object[]) example)[2])[0]))
          .getStringValue();
      }
      else gold = oracle.discreteValue(example);

      tester.reportPrediction(prediction, gold);

      for (example = parser.next(); example != null;
           example = parser.next(), ++processed) {
        totalTime -= System.currentTimeMillis();
        prediction = classifier.discreteValue(example);
        totalTime += System.currentTimeMillis();
        assert prediction != null
             : "Classifier returned null prediction for example " + example;

        if (preExtraction)
          gold =
            ((Feature)
             labelLexicon.lookupKey(((int[]) ((Object[]) example)[2])[0]))
            .getStringValue();
        else gold = oracle.discreteValue(example);

        tester.reportPrediction(prediction, gold);
      }
    }

    if (output) {
      System.out.println("Average evaluation time: "
                         + (totalTime / (1000.0 * processed)) + " seconds\n");
      tester.printPerformance(System.out);
    }

    return tester;
  }


  /**
    * Given command line parameters representing the fully qualified names of
    * the classifier to be tested, the oracle classifier to test against, the
    * parser supplying the testing objects, and the input parameter to the
    * parser's constructor this method instantiates all three objects.
    *
    * @param args The command line.
    * @return A new tester object containing the "null" labels.
   **/
  private static TestDiscrete instantiate(String[] args) {
    String classifierName = null, oracleName = null, parserName = null;
    String inputFile = null;
    TestDiscrete result = new TestDiscrete();

    try {
      int offset = 0;

      if (args[0].charAt(0) == '-') {
        if (!args[0].equals("-t")) throw new Exception();
        outputGranularity = Integer.parseInt(args[1]);
        offset = 2;
      }

      classifierName = args[offset];
      oracleName = args[offset + 1];
      parserName = args[offset + 2];
      inputFile = args[offset + 3];
      for (int i = offset + 4; i < args.length; ++i) result.addNull(args[i]);
    }
    catch (Exception e) {
      System.err.println(
      "usage:\n"
    + "  java LBJ2.classify.TestDiscrete [-t <n>] <classifier> <oracle> \\\n"
    + "                                  <parser> <input file> \\\n"
    + "                                  [<null label> [<null label> ...]]");
      System.exit(1);
    }

    classifier = ClassUtils.getClassifier(classifierName);
    oracle = ClassUtils.getClassifier(oracleName);
    parser =
      ClassUtils.getParser(parserName, new Class[]{ String.class },
                           new String[]{ inputFile });

    return result;
  }


  /** The histogram of correct labels. */
  protected HashMap goldHistogram;
  /** The histogram of predictions. */
  protected HashMap predictionHistogram;
  /** The histogram of correct predictions. */
  protected HashMap correctHistogram;
  /**
    * The set of "null" labels whose statistics are not included in overall
    * precision, recall, F1, or accuracy.
   **/
  protected HashSet nullLabels;


  /** Default constructor. */
  public TestDiscrete() {
    goldHistogram = new HashMap();
    predictionHistogram = new HashMap();
    correctHistogram = new HashMap();
    nullLabels = new HashSet();
  }


  /**
    * Whenever a prediction is made, report that prediction and the correct
    * label with this method.
    *
    * @param p  The prediction.
    * @param l  The correct label.
   **/
  public void reportPrediction(String p, String l) {
    histogramAdd(goldHistogram, l, 1);
    histogramAdd(predictionHistogram, p, 1);
    if (p.equals(l)) histogramAdd(correctHistogram, p, 1);
  }


  /**
    * Report all the predictions in the argument's histograms.
    *
    * @param t  Another object of this class.
   **/
  public void reportAll(TestDiscrete t) {
    histogramAddAll(goldHistogram, t.goldHistogram);
    histogramAddAll(predictionHistogram, t.predictionHistogram);
    histogramAddAll(correctHistogram, t.correctHistogram);
  }


  /**
    * Returns the set of labels that have been reported so far.
    *
    * @return An array containing the labels that have been reported so far.
   **/
  public String[] getLabels() {
    return (String[]) goldHistogram.keySet().toArray(new String[0]);
  }


  /**
    * Returns the set of predictions that have been reported so far.
    *
    * @return An array containing the predictions that have been reported so
    *         far.
   **/
  public String[] getPredictions() {
    return (String[]) predictionHistogram.keySet().toArray(new String[0]);
  }


  /**
    * Returns the set of all classes reported as either predictions or labels.
    *
    * @return An array containing all classes reported as either predictions
    *         or labels.
   **/
  public String[] getAllClasses() {
    HashSet result = new HashSet(goldHistogram.keySet());
    result.addAll(predictionHistogram.keySet());
    return (String[]) result.toArray(new String[0]);
  }


  /**
    * Adds a label to the set of "null" labels.
    *
    * @param n  The label to add.
   **/
  public void addNull(String n) { nullLabels.add(n); }


  /**
    * Removes a label from the set of "null" labels.
    *
    * @param n  The label to remove.
   **/
  public void removeNull(String n) { nullLabels.remove(n); }


  /**
    * Determines if a label is treated as a "null" label.
    *
    * @param n  The label in question.
    * @return <code>true</code> iff <code>n</code> is one of the "null"
    *         labels.
   **/
  public boolean isNull(String n) { return nullLabels.contains(n); }


  /** Returns <code>true</code> iff there exist "null" labels. */
  public boolean hasNulls() { return nullLabels.size() > 0; }


  /**
    * Takes a histogram implemented as a map and increments the count for the
    * given key by the given amount.
    *
    * @param histogram  The histogram.
    * @param key        The key whose count should be incremented.
    * @param amount     The amount by which to increment.
   **/
  protected void histogramAdd(HashMap histogram, String key, int amount) {
    Integer I = (Integer) histogram.get(key);
    if (I == null) I = new Integer(0);
    histogram.put(key, new Integer(I.intValue() + amount));
  }


  /**
    * Takes a histogram implemented as a map and retrieves the count for the
    * given key.
    *
    * @param histogram  The histogram.
    * @param key        The key whose count should be retrieved.
    * @return The count of the specified key.
   **/
  protected int histogramGet(HashMap histogram, String key) {
    Integer I = (Integer) histogram.get(key);
    if (I == null) I = new Integer(0);
    return I.intValue();
  }


  /**
    * Takes two histograms implemented as maps and adds the amounts found in
    * the second histogram to the amounts found in the first.
    *
    * @param h1 The first histogram, whose values will be modified.
    * @param h2 The second histogram, whose values will be added into the
    *           first's.
   **/
  protected void histogramAddAll(HashMap h1, HashMap h2) {
    for (Iterator I = h2.entrySet().iterator(); I.hasNext(); ) {
      Map.Entry e = (Map.Entry) I.next();
      histogramAdd(h1, (String) e.getKey(),
                   ((Integer) e.getValue()).intValue());
    }
  }


  /**
    * Returns the number of times the requested label was reported.
    *
    * @param l  The label in question.
    * @return The number of times <code>l</code> was reported.
   **/
  public int getLabeled(String l) { return histogramGet(goldHistogram, l); }


  /**
    * Returns the number of times the requested prediction was reported.
    *
    * @param p  The prediction in question.
    * @return The number of times <code>p</code> was reported.
   **/
  public int getPredicted(String p) {
    return histogramGet(predictionHistogram, p);
  }


  /**
    * Returns the number of times the requested prediction was reported
    * correctly.
    *
    * @param p  The prediction in question.
    * @return The number of times <code>p</code> was reported.
   **/
  public int getCorrect(String p) {
    return histogramGet(correctHistogram, p);
  }


  /**
    * Returns the precision associated with the given prediction.
    *
    * @param p  The given prediction.
    * @return The precision associated with <code>p</code>.
   **/
  public double getPrecision(String p) {
    return getCorrect(p) / (double) getPredicted(p);
  }


  /**
    * Returns the recall associated with the given label.
    *
    * @param l  The given label.
    * @return The precision associated with <code>l</code>.
   **/
  public double getRecall(String l) {
    return getCorrect(l) / (double) getLabeled(l);
  }


  /**
    * Returns the F<sub>1</sub> score associated with the given label.
    *
    * @param l  The given label.
    * @return The F<sub>1</sub> score associated with <code>l</code>.
   **/
  public double getF1(String l) { return getF(1, l); }


  /**
    * Returns the F<sub>beta</sub> score associated with the given label.
    * F<sub>beta</sub> is defined as:
    * <blockquote>
    *   <i>F<sub>beta</sub> = (beta<sup>2</sup> + 1) * P * R</i>
    *   <i>/ (beta<sup>2</sup> * P + R)</i>
    * </blockquote>
    *
    * @param b  The value of beta.
    * @param l  The given label.
    * @return The F<sub>beta</sub> score associated with <code>l</code>.
   **/
  public double getF(double b, String l) {
    double precision = getPrecision(l);
    double recall = getRecall(l);
    return (b * b + 1) * precision * recall / (b * b * precision + recall);
  }


  /**
    * Computes overall the overall statistics precision, recall,
    * F<sub>1</sub>, and accuracy.  Note that these statistics are all
    * equivalent unless "null" labels have been added.
    *
    * @return An array in which the first element represents overall
    *         precision, the second represents overall recall, then F1, and
    *         finally accuracy.
   **/
  public double[] getOverallStats() { return getOverallStats(1); }


  /**
    * Computes overall the overall statistics precision, recall,
    * F<sub>beta</sub>, and accuracy.  Note that these statistics are all
    * equivalent unless "null" labels have been added.
    *
    * @param b  The value of beta.
    * @return An array in which the first element represents overall
    *         precision, the second represents overall recall, then F1, and
    *         finally accuracy.
   **/
  public double[] getOverallStats(double b) {
    String[] allClasses = getAllClasses();

    int totalCorrect = 0;
    int totalPredicted = 0;
    int notNullCorrect = 0;
    int notNullPredicted = 0;
    int notNullLabeled = 0;

    for (int i = 0; i < allClasses.length; ++i) {
      int correct = getCorrect(allClasses[i]);
      int predicted = getPredicted(allClasses[i]);
      int labeled = getLabeled(allClasses[i]);

      totalCorrect += correct;
      totalPredicted += predicted;

      if (hasNulls() && !isNull(allClasses[i])) {
        notNullCorrect += correct;
        notNullPredicted += predicted;
        notNullLabeled += labeled;
      }
    }

    double[] result = new double[4];
    result[3] = totalCorrect / (double) totalPredicted;

    if (hasNulls()) {
      result[0] = notNullCorrect / (double) notNullPredicted;
      result[1] = notNullCorrect / (double) notNullLabeled;
      result[2] = (b * b + 1) * result[0] * result[1]
                  / (b * b * result[0] + result[1]);
    }
    else result[0] = result[1] = result[2] = result[3];

    return result;
  }


  /**
    * Performance results are written to the given stream in the form of
    * precision, recall, and F1 statistics.
    *
    * @param out  The stream to write to.
   **/
  public void printPerformance(PrintStream out) {
    String[] allClasses = getAllClasses();
    final HashSet n = nullLabels;
    Arrays.sort(allClasses,
                new Comparator() {
                  public int compare(Object o1, Object o2) {
                    String s1 = (String) o1;
                    String s2 = (String) o2;
                    int n1 = n.contains(s1) ? 1 : 0;
                    int n2 = n.contains(s2) ? 1 : 0;
                    if (n1 != n2) return n1 - n2;
                    return s1.compareTo(s2);
                  }
                });

    int rows = allClasses.length + 1;
    if (hasNulls()) ++rows;
    String[] rowLabels = new String[rows];
    System.arraycopy(allClasses, 0, rowLabels, 0, allClasses.length);
    rowLabels[rows - 1] = "Accuracy";
    if (hasNulls()) rowLabels[rows - 2] = "Overall";

    String[] columnLabels =
      new String[]{ "Label", "Precision", "Recall", "F1", "LCount",
                    "PCount" };

    int totalCorrect = 0;
    int totalPredicted = 0;
    int notNullCorrect = 0;
    int notNullPredicted = 0;
    int notNullLabeled = 0;

    Double[][] table = new Double[rows][];
    Double zero = new Double(0);

    for (int i = 0; i < allClasses.length; ++i) {
      int correct = getCorrect(allClasses[i]);
      int predicted = getPredicted(allClasses[i]);
      int labeled = getLabeled(allClasses[i]);

      totalCorrect += correct;
      totalPredicted += predicted;

      if (hasNulls() && !isNull(allClasses[i])) {
        notNullCorrect += correct;
        notNullPredicted += predicted;
        notNullLabeled += labeled;
      }

      table[i] =
        new Double[]{ zero, zero, zero, new Double(labeled),
                      new Double(predicted) };

      if (predicted > 0)
        table[i][0] = new Double(100 * correct / (double) predicted);
      if (labeled > 0)
        table[i][1] = new Double(100 * correct / (double) labeled);

      if (correct > 0) {
        double p = table[i][0].doubleValue();
        double r = table[i][1].doubleValue();
        table[i][2] = new Double(2 * p * r / (p + r));
      }
    }

    int[] dashRows = null;

    if (hasNulls()) {
      table[rows - 2] =
        new Double[]{ zero, zero, zero, new Double(notNullLabeled),
                      new Double(notNullPredicted) };

      if (notNullPredicted > 0)
        table[rows - 2][0] =
          new Double(100 * notNullCorrect / (double) notNullPredicted);
      if (notNullLabeled > 0)
        table[rows - 2][1] =
          new Double(100 * notNullCorrect / (double) notNullLabeled);

      if (notNullCorrect > 0) {
        double p = table[rows - 2][0].doubleValue();
        double r = table[rows - 2][1].doubleValue();
        table[rows - 2][2] = new Double(2 * p * r / (p + r));
      }

      int nonNullLabels = allClasses.length - nullLabels.size();
      dashRows = new int[]{ 0, nonNullLabels, allClasses.length };
    }
    else dashRows = new int[]{ 0, allClasses.length };

    double accuracy =
      totalPredicted == 0 ? 0 : 100 * totalCorrect / (double) totalPredicted;
    table[rows - 1] =
      new Double[]{ new Double(accuracy), null, null, null,
                    new Double(totalPredicted) };

    TableFormat.printTableFormat(out, columnLabels, rowLabels, table,
                                 new int[]{ 3, 3, 3, 0, 0 }, dashRows);
  }
}

