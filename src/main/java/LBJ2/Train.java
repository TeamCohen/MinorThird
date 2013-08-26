package LBJ2;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

import LBJ2.IR.AST;
import LBJ2.IR.ClassifierAssignment;
import LBJ2.IR.CodedClassifier;
import LBJ2.IR.Constant;
import LBJ2.IR.ConstraintDeclaration;
import LBJ2.IR.InferenceDeclaration;
import LBJ2.IR.LearningClassifierExpression;
import LBJ2.IR.ParameterSet;
import LBJ2.learn.Accuracy;
import LBJ2.learn.BatchTrainer;
import LBJ2.learn.Learner;
import LBJ2.learn.Lexicon;
import LBJ2.learn.TestingMetric;
import LBJ2.parse.ArrayFileParser;
import LBJ2.parse.Parser;
import LBJ2.util.ClassUtils;


/**
  * After code has been generated with {@link TranslateToJava}, this pass
  * trains any classifiers for which training was indicated.
  *
  * @see    LBJ2.TranslateToJava
  * @author Nick Rizzolo
 **/
public class Train extends Pass
{
  /** <!-- stackTrace(Throwable) -->
    * Generates a <code>String</code> containing the name of the specified
    * <code>Throwable</code> and its stack trace.
    *
    * @param t  <code>Throwable</code>.
    * @return The generated message.
   **/
  private static String stackTrace(Throwable t) {
    String message = "  " + t + "\n";
    StackTraceElement[] elements = t.getStackTrace();
    if (elements.length == 0) message += "    no stack trace available\n";
    for (int i = 0; i < elements.length; ++i)
      message += "    " + elements[i] + "\n";
    return message;
  }


  /**
    * Remembers which files have been compiled via {@link #runJavac(String)}.
   **/
  private static final TreeSet compiledFiles = new TreeSet();


  /** <!-- runJavac(String) -->
    * Run the <code>javac</code> compiler with the specified arguments in
    * addition to those specified on the command line.
    *
    * @param arguments  The arguments to send to <code>javac</code>.
    * @return <code>true</code> iff errors were encountered.
   **/
  public static boolean runJavac(String arguments) {
    String[] files = arguments.split("\\s+");
    arguments = "";
    for (int i = 0; i < files.length; ++i)
      if (compiledFiles.add(files[i]))
        arguments += " " + files[i];
    if (arguments.length() == 0) return false;

    Process javac = null;
    String pathArguments = "-classpath " + Main.classPath + " -sourcepath "
                           + Main.sourcePath;

    if (Main.generatedSourceDirectory != null) {
      String gsd = Main.generatedSourceDirectory;
      int packageIndex = -1;
      if (AST.globalSymbolTable.getPackage().length() != 0)
        packageIndex =
          gsd.lastIndexOf(File.separator + AST.globalSymbolTable.getPackage()
                                           .replace('.', File.separatorChar));
      if (packageIndex != -1) gsd = gsd.substring(0, packageIndex);
      pathArguments += File.pathSeparator + gsd;
    }

    if (Main.classPackageDirectory != null)
      pathArguments += " -d " + Main.classPackageDirectory;

    String command = Configuration.javac + " " + Main.javacArguments + " "
                     + pathArguments + arguments;

    try { javac = Runtime.getRuntime().exec(command); }
    catch (Exception e) {
      System.err.println("Failed to execute 'javac': " + e);
      System.exit(1);
    }

    BufferedReader error =
      new BufferedReader(new InputStreamReader(javac.getErrorStream()));
    try {
      for (String line = error.readLine(); line != null;
           line = error.readLine())
        System.out.println(line);
    }
    catch (Exception e) {
      System.err.println("Error reading STDERR from 'javac': " + e);
      System.exit(1);
    }

    int exit = 0;
    try { exit = javac.waitFor(); }
    catch (Exception e) {
      System.err.println("Error waiting for 'javac' to terminate: " + e);
      System.exit(1);
    }

    return exit != 0;
  }


  // Member variables.
  /**
    * Progress output will be printed every <code>progressOutput</code>
    * examples.
   **/
  protected int progressOutput;
  /**
    * Set to <code>true</code> iff there existed a
    * {@link LearningClassifierExpression} for which new code was generated.
   **/
  protected boolean newCode;
  /**
    * An array of the training threads, which is never modified after it is
    * constructed.
   **/
  protected TrainingThread[] threads;
  /** A map of all the training threads indexed by the name of the learner. */
  protected HashMap threadMap;
  /**
    * The keys of this map are the names of learners; the values are
    * <code>LinkedList</code>s of the names of the learners that the learner
    * named by the key depends on.
   **/
  protected HashMap learnerDependencies;


  // Constructor.
  /**
    * Instantiates a pass that runs on an entire {@link AST}.
    *
    * @param ast    The program to run this pass on.
    * @param output Progress output will be printed every <code>output</code>
    *               examples.
   **/
  public Train(AST ast, int output) {
    super(ast);
    progressOutput = output;
  }


  // Methods related to learnerDependencies.
  /** <!-- addDependency(String, String) -->
    * Adds an edge from dependor to dependency in the
    * {@link #learnerDependencies} graph.  If <code>dependency</code> is
    * <code>null</code>, no new list item is added, but the
    * <code>HashSet</code> associated with <code>dependor</code> is still
    * created if it didn't already exist.
    *
    * @param dependor   The name of the node doing the depending.
    * @param dependency The name of the node depended on.
   **/
  private void addDependency(String dependor, String dependency) {
    HashSet dependencies = (HashSet) learnerDependencies.get(dependor);

    if (dependencies == null) {
      dependencies = new HashSet();
      learnerDependencies.put(dependor, dependencies);
    }

    if (dependency != null) dependencies.add(dependency);
  }


  /** <!-- fillLearnerDependorsDAG() -->
    * This method initializes the {@link #learnerDependencies} graph such
    * that the entry for each learner contains the names of all learners that
    * depend on it, except that cycles are broken by preferring that learners
    * appearing earlier in the source get trained first.
   **/
  protected void fillLearnerDependorsDAG() {
    threads =
      (TrainingThread[]) threadMap.values().toArray(new TrainingThread[0]);
    Arrays.sort(threads,
                new Comparator() {
                  public int compare(Object o1, Object o2) {
                    TrainingThread t1 = (TrainingThread) o1;
                    TrainingThread t2 = (TrainingThread) o2;
                    return t2.byteOffset - t1.byteOffset;
                  }
                });

    for (int i = 0; i < threads.length - 1; ++i)
      for (int j = i + 1; j < threads.length; ++j) {
        if (SemanticAnalysis.isDependentOn(threads[i].getName(),
                                           threads[j].getName()))
          addDependency(threads[i].getName(), threads[j].getName());
        else if (SemanticAnalysis.isDependentOn(threads[j].getName(),
                                                threads[i].getName()))
          addDependency(threads[j].getName(), threads[i].getName());
      }
  }


  /** <!-- executeReadyThreads(String) -->
    * This method updates the {@link #learnerDependencies} graph by removing
    * the specified name from every dependencies list, and then starts every
    * thread that has no more dependencies.
    *
    * @param name The name of a learner whose training has completed.
   **/
  protected void executeReadyThreads(String name) {
    LinkedList ready = new LinkedList();

    synchronized (learnerDependencies) {
      for (Iterator I = learnerDependencies.entrySet().iterator();
           I.hasNext(); ) {
        Map.Entry e = (Map.Entry) I.next();
        HashSet dependencies = (HashSet) e.getValue();
        dependencies.remove(name);
        if (dependencies.size() == 0) ready.add(e.getKey());
      }
    }

    for (Iterator I = ready.iterator(); I.hasNext(); ) {
      TrainingThread thread = null;

      synchronized (threadMap) {
        thread = (TrainingThread) threadMap.remove(I.next());
      }

      if (thread != null) {
        thread.start();

        if (!Main.concurrentTraining) {
          try { thread.join(); }
          catch (InterruptedException e) {
            System.err.println("LBJ ERROR: Training of " + thread.getName()
                               + " has been interrupted.");
            fatalError = true;
          }
        }
      }
    }
  }


  /** <!-- run(AST) -->
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ast  The node to process.
   **/
  public void run(AST ast) {
    if (RevisionAnalysis.noChanges) return;
    threadMap = new HashMap();
    learnerDependencies = new HashMap();

    if (Main.fileNames.size() > 0) {
      String files = "";
      for (Iterator I = Main.fileNames.iterator(); I.hasNext(); )
        files += " " + I.next();
      System.out.println("Compiling generated code");
      if (runJavac(files)) return;
    }

    Main.fileNames.clear();

    runOnChildren(ast);

    fillLearnerDependorsDAG();
    executeReadyThreads(null);

    for (int i = 0; i < threads.length; ++i) {
      try { threads[i].join(); }
      catch (InterruptedException e) {
        System.err.println("LBJ ERROR: Training of " + threads[i].getName()
                           + " has been interrupted.");
        fatalError = true;
      }
    }

    if (!fatalError && newCode) {
      String files = "";
      for (Iterator I = Main.fileNames.iterator(); I.hasNext(); )
        files += " " + I.next();
      System.out.println("Compiling generated code");
      compiledFiles.clear();
      runJavac(files);
    }
  }


  /** <!-- run(LearningClassifierExpression) -->
    * Runs this pass on all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce) {
    runOnChildren(lce);

    String lceName = lce.name.toString();

    if (lce.parser == null
        ? !RevisionAnalysis.revisionStatus.get(lceName)
           .equals(RevisionAnalysis.REVISED)
        : lce.learningStatus.equals(RevisionAnalysis.UNAFFECTED)
          && !lce.onlyCodeGeneration)
      return;

    newCode |= true;

    TrainingThread thread = new TrainingThread(lceName, lce.byteOffset, lce);
    threadMap.put(lceName, thread);
    addDependency(lceName, null);
  }


  // The following three methods are here to stop AST traversal.
  /** <!-- run(CodedClassifier) -->
    * Runs this pass on all nodes of the indicated type.  There's no reason to
    * traverse children of {@link CodedClassifier}s, so this method exists
    * simply to stop that from happening.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc) { }


  /** <!-- run(ConstraintDeclaration) -->
    * Runs this pass on all nodes of the indicated type.  There's no reason to
    * traverse children of {@link ConstraintDeclaration}s, so this method
    * exists simply to stop that from happening.
    *
    * @param cd The node to process.
   **/
  public void run(ConstraintDeclaration cd) { }


  /** <!-- run(InferenceDeclaration) -->
    * Runs this pass on all nodes of the indicated type.  There's no reason to
    * traverse children of {@link InferenceDeclaration}s, so this method
    * exists simply to stop that from happening.
    *
    * @param id The node to process.
   **/
  public void run(InferenceDeclaration id) { }


  /** <!-- increment(int[],int[]) -->
    * Helps the {@link TrainingThread#getParameterCombinations()} method
    * iterate through all combinations and permutations of integers such that
    * each integer is at least 0 and less than the corresponding element of
    * <code>maxes</code>.
    *
    * @param I      The current array of integers.
    * @param maxes  The maximums for each element of <code>I</code>.
   **/
  private static boolean increment(int[] I, int[] maxes) {
    int i = 0;
    while (i < I.length && ++I[i] == maxes[i]) I[i++] = 0;
    return i < I.length;
  }


  /**
    * This class contains the code that trains a learning classifier.  It is a
    * subclass of <code>Thread</code> so that it may be executed concurrently.
    *
    * @author Nick Rizzolo
   **/
  protected class TrainingThread extends Thread
  {
    // Member variables.
    /** The byte offset at which the learner appeared. */
    public int byteOffset;
    /** The expression that specified the learner. */
    protected LearningClassifierExpression lce;
    /** The learning classifier being trained. */
    protected Learner learner;
    /** The class of {@link #learner}. */
    protected Class learnerClass;
    /** {@link #learner}'s <code>Parameters</code> class. */
    protected Class parametersClass;
    /** The file into which training examples are extracted. */
    protected String exFilePath;
    /** The file into which testing examples are extracted. */
    protected String testExFilePath;
    /** The directory into which class files, model files, etc are written. */
    protected String classDir;
    /** Whether or not example vectors should be pre-extracted. */
    protected boolean preExtract;
    /** Whether or not pre-extracted example files should be compressed. */
    protected boolean preExtractZip;
    /** Actually does the training. */
    protected BatchTrainer trainer;
    /** The parser from which testing objects are obtained. */
    protected Parser testParser;
    /**
      * The metric with which to measure the learner's performance on a test
      * set.
     **/
    protected TestingMetric testingMetric;


    // Constructor.
    /**
      * Initializing constructor.
      *
      * @param n    The name of the learner.
      * @param b    The byte offset at which the learner appeared.
      * @param lce  The expression that specified the learner.
     **/
    public TrainingThread(String n, int b, LearningClassifierExpression lce) {
      super(n);
      byteOffset = b;
      this.lce = lce;
      if (lce.onlyCodeGeneration) return;

      classDir = Main.classDirectory == null
                 ? "" : Main.classDirectory + File.separator;
      learner = getLearner(classDir);

      preExtract = lce.preExtract != null
                   && !lce.preExtract.value.equals("false")
                   && !lce.preExtract.value.equals("\"false\"")
                   && !lce.preExtract.value.equals("\"none\"");
      boolean preExtractToDisk =
        preExtract && !lce.preExtract.value.startsWith("\"mem");
      preExtractZip = preExtract && lce.preExtract.value.endsWith("Zip\"");

      if (preExtractToDisk) {
        exFilePath = getName() + ".ex";
        testExFilePath = getName() + ".test.ex";
        if (Main.generatedSourceDirectory != null) {
          exFilePath =
            Main.generatedSourceDirectory + File.separator + exFilePath;
          testExFilePath =
            Main.generatedSourceDirectory + File.separator + testExFilePath;
        }
      }

      Parser parser = null;
      if (lce.parser != null) {
        if (lce.featuresStatus == RevisionAnalysis.UNAFFECTED) {
          // Implies preExtractToDisk is true because of RevisionAnalysis;
          // therefore, exFilePath != null
          parser =
            new ArrayFileParser(exFilePath,
                                lce.preExtract.value.endsWith("Zip\""));
          if (lce.pruneStatus != RevisionAnalysis.UNAFFECTED)
            learner.readLexiconOnDemand(classDir + getName() + ".lex");
        }
        else parser = getParser("getParser");
      }

      if (lce.testParser != null) {
        if (lce.pruneStatus == RevisionAnalysis.UNAFFECTED
            && new File(testExFilePath).exists())
          // If pruneStatus is affected, pruning will rearrange our lexicon,
          // so we must re-extract the test set from the original parser.  In
          // addition, pruneStatus == UNAFFECTED implies featuresStatus ==
          // UNAFFECTED.  So, like above, as soon as we know pruneStatus ==
          // UNAFFECTED, we know testExFilePath != null
          testParser =
            new ArrayFileParser(testExFilePath,
                                lce.preExtract.value.endsWith("Zip\""));
        else testParser = getParser("getTestParser");
      }

      testingMetric = getTestingMetric();

      if (lce.progressOutput != null)
        progressOutput = Integer.parseInt(lce.progressOutput.value);

      trainer = new BatchTrainer(learner, parser, progressOutput);
    }


    /** <!-- getLearner(String) -->
      * Obtain an instance of the learner appropriate for the revision status
      * of the source file.  This method also fills in the
      * {@link #learnerClass} and {@link #parametersClass} fields.
      *
      * <p> If the only change between the last run of the compiler and this
      * run is that more training rounds were added, the entire model file can
      * be loaded from disk.  Failing that, if features are unaffected
      * according to {@link RevisionAnalysis}, it means only the label lexicon
      * should be read.  Otherwise, we just start with a fresh instance of the
      * learner via its static <code>getInstance()</code> method.  In any
      * case, the learner is initialized so that it will write its model
      * and/or lexicon files to the specified directory as necessary.
      *
      * @param dir  The directory in which the model and lexicon are written.
      * @return An instance of the learner.
     **/
    private Learner getLearner(String dir) {
      String fullyQualified = AST.globalSymbolTable.getPackage();
      if (fullyQualified.length() > 0) fullyQualified += ".";
      fullyQualified += getName();
      learnerClass = ClassUtils.getClass(fullyQualified, true);

      Class[] declaredClasses = learnerClass.getDeclaredClasses();
      int c = 0;
      while (c < declaredClasses.length
             && !declaredClasses[c].getName()
                 .endsWith(getName() + "$Parameters"))
        ++c;

      if (c == declaredClasses.length) {
        System.err.println(
            "LBJ ERROR: Expected to find a single member class inside "
            + getName() + " named 'Parameters'.");
        for (int i = 0; i < declaredClasses.length; ++i)
          System.err.println(i + ": " + declaredClasses[i].getName());
        System.exit(1);
      }
      parametersClass = declaredClasses[c];

      Learner l = null;

      if (lce.startingRound > 1) {
        // In the condition above, note that before setting
        // lce.startingRound > 1, RevisionAnalysis ensures that the lce is
        // unaffected other than the number of rounds and that there will be
        // no parameter tuning or cross validation.
        l = Learner.readLearner(dir + getName() + ".lc");
        l.setLexiconLocation(dir + getName() + ".lex");
      }
      else if (lce.featuresStatus == RevisionAnalysis.UNAFFECTED) {
        Constructor noArg = null;
        try { noArg = parametersClass.getConstructor(new Class[0]); }
        catch (Exception e) {
          System.err.println(
              "LBJ ERROR: Can't find a no-argument constructor for "
              + getName() + ".Parameters.");
          System.exit(1);
        }

        Learner.Parameters p = null;
        try { p = (Learner.Parameters) noArg.newInstance(new Object[0]); }
        catch (Exception e) {
          System.err.println(
              "LBJ ERROR: Can't instantiate " + getName() + ".Parameters:");
          e.printStackTrace();
          System.exit(1);
        }

        l = Learner.readLearner(dir + getName() + ".lc", false);
        l.setParameters(p);
        l.setLexiconLocation(dir + getName() + ".lex");
      }
      else {
        Method getInstance = null;
        try {
          getInstance =
            learnerClass.getDeclaredMethod("getInstance", new Class[0]);
        }
        catch (Exception e) {
          System.err.println("LBJ ERROR: Could not access method '"
                             + fullyQualified + ".getInstance()':");
          System.exit(1);
        }

        try { l = (Learner) getInstance.invoke(null, null); }
        catch (Exception e) {
          System.err.println("LBJ ERROR: Could not get unique instance of '"
                             + fullyQualified + "': " + e);
          e.getCause().printStackTrace();
          System.exit(1);
        }

        if (l == null) {
          System.err.println("LBJ ERROR: Could not get unique instance of '"
                             + fullyQualified + "'.");
          System.exit(1);
        }

        l.setModelLocation(dir + getName() + ".lc");
        l.setLexiconLocation(dir + getName() + ".lex");
      }

      return l;
    }


    /** <!-- getParser(String) -->
      * Call the specified method of {@link #learnerClass}, and return the
      * <code>Parser</code> returned by that method.
      *
      * @param name The name of the method.
      * @return The parser returned by the named method.
     **/
    private Parser getParser(String name) {
      Method m = null;
      try { m = learnerClass.getDeclaredMethod(name, new Class[0]); }
      catch (Exception e) {
        reportError(lce.line,
                    "Could not access method '" + lce.name + "." + name
                    + "()': " + e);
        return null;
      }

      Parser result = null;

      try { result = (Parser) m.invoke(null, null); }
      catch (Exception e) {
        System.err.println(
            "Could not instantiate parser '" + lce.parser.name + "': " + e
            + ", caused by");
        Throwable cause = e.getCause();
        System.err.print(stackTrace(cause));

        if (cause instanceof ExceptionInInitializerError) {
          System.err.println("... caused by");
          System.err.print(
              stackTrace(((ExceptionInInitializerError) cause).getCause()));
        }

        return null;
      }

      return result;
    }


    /** <!-- getTestingMetric() -->
      * Call the <code>getTestingMetric()</code> method of
      * {@link #learnerClass} and return the testing metric it returns.
     **/
    private TestingMetric getTestingMetric() {
      TestingMetric testingMetric = null;
      if (lce.testingMetric != null) {
        Method getTestingMetric = null;
        try {
          getTestingMetric =
            learnerClass.getDeclaredMethod("getTestingMetric", new Class[0]);
        }
        catch (Exception e) {
          reportError(lce.line,
                      "Could not access method'" + getName()
                      + ".getTestingMetric()': " + e);
          return null;
        }

        try {
          testingMetric = (TestingMetric) getTestingMetric.invoke(null, null);
        }
        catch (Exception e) {
          System.err.println(
              "Could not instantiate testing metric '" + lce.parser.name
              + "': " + e + ", caused by");
          System.err.print(stackTrace(e.getCause()));
          return null;
        }
      }
      else testingMetric = new Accuracy();

      return testingMetric;
    }


    /** <!-- preExtractAndPrune() -->
      * Handles feature pre-extraction and dataset pruning under the
      * assumption that pre-extraction has been called for by the source code.
      * The two go hand-in-hand, as we only need to compute and store feature
      * counts during pre-extraction if we are pruning.
     **/
    private void preExtractAndPrune() {
      Lexicon.PruningPolicy pruningPolicy = new Lexicon.PruningPolicy();
      Lexicon.CountPolicy countPolicy = Lexicon.CountPolicy.none;
      if (lce.pruneCountType != null) {
        pruningPolicy =
          lce.pruneThresholdType.value.equals("\"count\"")
          ? new Lexicon.PruningPolicy(
              Integer.parseInt(lce.pruneThreshold.value))
          : new Lexicon.PruningPolicy(
              Double.parseDouble(lce.pruneThreshold.value));
        countPolicy =
          lce.pruneCountType.value.equals("\"global\"")
          ? Lexicon.CountPolicy.global : Lexicon.CountPolicy.perClass;
      }

      Learner preExtractLearner = learner;  // Needed in case we're pruning.
      Lexicon lexicon = null; // Needed for pre-extracting the test set.
        // As seen below, we can always read the lexicon off disk just before
        // pre-extracting the test set, but if one of the operations between
        // now and then obtains the lexicon incidentally, we'll keep it here
        // to avoid reading it from disk again.

      if (pruningPolicy.isNone()) {
        if (lce.featuresStatus != RevisionAnalysis.UNAFFECTED)
          lexicon = trainer.preExtract(exFilePath, preExtractZip);
        else if (lce.pruneStatus != RevisionAnalysis.UNAFFECTED)
          lexicon = learner.getLexiconDiscardCounts();
        else trainer.fillInSizes();
      }
      else if (lce.featuresStatus != RevisionAnalysis.UNAFFECTED
               || lce.pruneStatus != RevisionAnalysis.UNAFFECTED
                  && lce.previousPruneCountType == null)
        preExtractLearner =
          trainer.preExtract(exFilePath, preExtractZip, countPolicy);
      else if (lce.previousPruneCountType != null
               && !lce.previousPruneCountType.equals(lce.pruneCountType)) {
        if (lce.previousPruneCountType.value.equals("\"global\""))
          // implies lce.pruneCountType.equals("\"perClass\"")
          preExtractLearner =
            trainer.preExtract(exFilePath, preExtractZip, countPolicy);
        else // lce.previousPruneCountType.value.equals("\"perClass\"")
          learner.getLexicon().perClassToGlobalCounts();
      }
      // else pruneThresholdType or pruneThreshold may have changed, but
      // that does not require recounting of features.

      if (lce.featuresStatus == RevisionAnalysis.UNAFFECTED
          ? lce.pruneStatus != RevisionAnalysis.UNAFFECTED
          : !pruningPolicy.isNone()) {
        trainer.pruneDataset(exFilePath, preExtractZip, pruningPolicy,
                             preExtractLearner);
        lexicon = preExtractLearner.getLexicon();
        if (preExtractLearner == learner) learner.setLexicon(null);
      }

      if (testParser != null
          && (lce.pruneStatus != RevisionAnalysis.UNAFFECTED
              || !(new File(testExFilePath).exists()))) {
        if (lexicon == null)
          learner.readLexiconOnDemand(classDir + getName() + ".lex");
        else {
          learner.setLexicon(lexicon);
          lexicon = null; // See comment below
        }

        BatchTrainer preExtractor =
          new BatchTrainer(learner, testParser, trainer.getProgressOutput(),
                           "test set: ");
        preExtractor.preExtract(testExFilePath, preExtractZip,
                                Lexicon.CountPolicy.none);
        testParser = preExtractor.getParser();
      }

      // At this point, it should be the case that (lexicon == null) implies
      // that the lexicon is not in memory.  Above, we intentionally discard
      // the lexicon when pre-extracting the test set, since that process will
      // add unwanted features (since pre-extraction always happens under the
      // assumption that we are training).

      // Given the above comment, we now ensure that when this learning classifier (ie,
      // the one whose feature vectors we have just pre-extracted) is
      // called as a feature for some other learning classifier defined in the
      // same sourcefile, it will be prepared take a raw example object as
      // input.
      String name = getName();
      HashSet dependors = (HashSet) SemanticAnalysis.dependorGraph.get(name);
      if (lexicon != null && dependors.size() > 0)
        learner.setLexicon(lexicon);
      else learner.readLexiconOnDemand(classDir + name + ".lex");
    }


    /** <!-- getParameterCombinations() -->
      * Uses the various {@link LBJ2.IR.ParameterSet ParameterSet}s in the AST
      * to generate an array of parameter combinations representing the cross
      * product of all {@link LBJ2.IR.ParameterSet ParameterSet}s except the
      * one in the {@link LearningClassifierExpression#rounds} field, if any.
     **/
    private Learner.Parameters[] getParameterCombinations() {
      Class[] paramTypes = new Class[lce.parameterSets.size()];
      Object[][] arguments = new Object[paramTypes.length][];
      int[] lengths = new int[paramTypes.length];
      int totalCombinations = 1;

      Iterator iterator = lce.parameterSets.iterator();
      for (int i = 0; i < paramTypes.length; i++) {
        ParameterSet ps = (ParameterSet) iterator.next();
        paramTypes[i] = ps.type.typeClass();
        arguments[i] = ps.toStringArray();
        lengths[i] = arguments[i].length;
        totalCombinations *= lengths[i];
      }

      for (int i = 0; i < arguments.length; i++) {
        Class t = paramTypes[i];
        if (t.isPrimitive()) {
          if (t.getName().equals("int"))
            for (int j = 0; j < lengths[i]; ++j)
              arguments[i][j] = new Integer((String) arguments[i][j]);
          else if (t.getName().equals("long"))
            for (int j = 0; j < lengths[i]; ++j)
              arguments[i][j] = new Long((String) arguments[i][j]);
          else if (t.getName().equals("short"))
            for (int j = 0; j < lengths[i]; ++j)
              arguments[i][j] = new Short((String) arguments[i][j]);
          else if (t.getName().equals("double"))
            for (int j = 0; j < lengths[i]; ++j)
              arguments[i][j] = new Double((String) arguments[i][j]);
          else if (t.getName().equals("float"))
            for (int j = 0; j < lengths[i]; ++j)
              arguments[i][j] = new Float((String) arguments[i][j]);
          else if (t.getName().equals("boolean"))
            for (int j = 0; j < lengths[i]; ++j)
              arguments[i][j] = new Boolean((String) arguments[i][j]);
        }
      }

      Constructor c = null;
      try { c = parametersClass.getConstructor(paramTypes); }
      catch (Exception e) {
        System.err.println(
            "LBJ ERROR: Can't find a parameter tuning constructor for "
            + getName() + ".Parameters.");
        e.printStackTrace();
        System.exit(1);
      }

      Learner.Parameters[] result = new Learner.Parameters[totalCombinations];
      int[] I = new int[paramTypes.length];
      Object[] a = new Object[paramTypes.length];
      int i = 0;

      do {
        for (int j = 0; j < a.length; ++j) a[j] = arguments[j][I[j]];
        try { result[i++] = (Learner.Parameters) c.newInstance(a); }
        catch (Exception e) {
          System.err.println(
              "LBJ ERROR: Can't instantiate " + getName() + ".Parameters:");
          e.printStackTrace();
          System.exit(1);
        }
      } while (increment(I, lengths));

      return result;
    }


    /** <!-- tune() -->
      * Determines the best parameters to use when training the learner,
      * under the assumption that {@link LBJ2.IR.ParameterSet}s were present.
      * Here, "best" means the parameters that did the best out of some small
      * set of particular parameter settings.
     **/
    private Learner.Parameters tune() {
      Learner.Parameters[] parameterCombinations =
        getParameterCombinations();
      int[] rounds = null;
      if (lce.rounds != null) {
        if (lce.rounds instanceof ParameterSet)
          rounds = ((ParameterSet) lce.rounds).toSortedIntArray();
        else
          rounds =
            new int[]{ Integer.parseInt(((Constant) lce.rounds).value) };
      }
      else rounds = new int[]{ 1 };

      if (lce.K != null) {
        int k = Integer.parseInt(lce.K.value);
        double alpha = Double.parseDouble(lce.alpha.value);
        return
          trainer.tune(parameterCombinations, rounds, k, lce.splitPolicy,
                       alpha, testingMetric);
      }

      return
        trainer.tune(parameterCombinations, rounds, testParser,
                     testingMetric);
    }


    /** Performs the training and then generates the new code. */
    public void run() {
      boolean tuningParameters =
        lce.parameterSets.size() > 0
        || lce.rounds != null && lce.rounds instanceof ParameterSet;

      if (!lce.onlyCodeGeneration) {
        // If there's a "from" clause, train.
        try {
          if (lce.parser != null) {
            System.out.println("Training " + getName());
            if (preExtract) {
              preExtractAndPrune();
              System.gc();
            }
            else learner.saveLexicon();
            int trainingRounds = 1;

            if (tuningParameters) {
              String parametersPath = getName();
              if (Main.classDirectory != null)
                parametersPath =
                  Main.classDirectory + File.separator + parametersPath;
              parametersPath += ".p";

              Learner.Parameters bestParameters = tune();
              trainingRounds = bestParameters.rounds;
              Learner.writeParameters(bestParameters, parametersPath);
              System.out.println("  " + getName()
                                 + ": Training on entire training set");
            }
            else {
              if (lce.rounds != null)
                trainingRounds =
                  Integer.parseInt(((Constant) lce.rounds).value);

              if (lce.K != null) {
                int[] rounds = { trainingRounds };
                int k = Integer.parseInt(lce.K.value);
                double alpha = Double.parseDouble(lce.alpha.value);
                trainer.crossValidation(rounds, k, lce.splitPolicy, alpha,
                                        testingMetric, true);
                System.out.println("  " + getName()
                                   + ": Training on entire training set");
              }
            }

            trainer.train(lce.startingRound, trainingRounds);

            if (testParser != null) {
              System.out.println("Testing " + getName());
              new Accuracy(true).test(learner, learner.getLabeler(),
                                      testParser);
            }

            System.out.println("Writing " + getName());
          }
          else learner.saveLexicon(); // Writes .lex even if lexicon is empty.

          learner.save(); // Doesn't write .lex if lexicon is empty.
        }
        catch (Exception e) {
          System.err.println(
              "LBJ ERROR: Exception while training " + getName() + ":");
          e.printStackTrace();
          fatalError = true;
          return;
        }

        // Set learner's static instance field to the newly learned instance.
        Field field = null;
        try { field = learnerClass.getField("instance"); }
        catch (Exception e) {
          System.err.println("Can't access " + learnerClass
                             + "'s 'instance' field: " + e);
          System.exit(1);
        }

        try { field.set(null, learner); }
        catch (Exception e) {
          System.err.println("Can't set " + learnerClass
                             + "'s 'instance' field: " + e);
          System.exit(1);
        }
      }
      else System.out.println("Generating code for " + lce.name);

      // Write the new code.
      PrintStream out = TranslateToJava.open(lce);
      if (out == null) return;

      out.println(TranslateToJava.disclaimer);
      out.print("// ");
      TranslateToJava.compressAndPrint(lce.shallow(), out);
      out.println("\n");

      ast.symbolTable.generateHeader(out);

      if (lce.cacheIn != null) {
        String f = lce.cacheIn.toString();
        boolean cachedInMap = f.equals(ClassifierAssignment.mapCache);
        if (cachedInMap) out.println("import java.util.WeakHashMap;");
      }

      out.println("\n");
      if (lce.comment != null) out.println(lce.comment);

      out.println("\n\npublic class " + getName() + " extends "
                  + lce.learnerName);
      out.println("{");
      out.println("  private static java.net.URL _lcFilePath;");
      out.println("  private static java.net.URL _lexFilePath;");
      if (tuningParameters)
        out.println("  private static java.net.URL parametersPath;");
      out.println();

      out.println("  static");
      out.println("  {");
      out.println("    _lcFilePath = " + getName() + ".class.getResource(\""
                  + getName() + ".lc\");\n");

      out.println("    if (_lcFilePath == null)");
      out.println("    {");
      out.println("      System.err.println(\"ERROR: Can't locate "
                  + getName() + ".lc in the class path.\");");
      out.println("      System.exit(1);");
      out.println("    }\n");

      out.println("    _lexFilePath = " + getName() + ".class.getResource(\""
                  + getName() + ".lex\");\n");

      out.println("    if (_lexFilePath == null)");
      out.println("    {");
      out.println("      System.err.println(\"ERROR: Can't locate "
                  + getName() + ".lex in the class path.\");");
      out.println("      System.exit(1);");
      out.println("    }");

      if (tuningParameters) {
        out.println(
            "\n    parametersPath = " + getName() + ".class.getResource(\""
            + getName() + ".p\");\n");

        out.println("    if (parametersPath == null)");
        out.println("    {");
        out.println("      System.err.println(\"ERROR: Can't locate "
                    + getName() + ".p in the class path.\");");
        out.println("      System.exit(1);");
        out.println("    }");
      }
      out.println("  }\n");

      out.println("  private static void loadInstance()");
      out.println("  {");
      out.println("    if (instance == null)");
      out.println("    {");
      out.println("      instance = (" + getName()
                  + ") Learner.readLearner(_lcFilePath);");
      out.println("      instance.readLexiconOnDemand(_lexFilePath);");
      out.println("    }");
      out.println("  }\n");

      if (tuningParameters) {
        out.println("  private static " + lce.learnerName
                    + ".Parameters bestParameters;\n");

        out.println("  public static " + lce.learnerName
                    + ".Parameters getBestParameters()");
        out.println("  {");
        out.println("    if (bestParameters == null)");
        out.println("      bestParameters = (" + lce.learnerName
                    + ".Parameters) Learner.readParameters(parametersPath);");
        out.println("    return bestParameters;");
        out.println("  }\n");
      }

      if (exFilePath != null
          && lce.featuresStatus != RevisionAnalysis.UNAFFECTED
          && new File(exFilePath).exists())
        out.println(
            "  public static Parser getParser() { return new "
            + "LBJ2.parse.ArrayFileParser(\""
            + new File(exFilePath).getAbsolutePath()
            + "\"); }");
      else
        out.println("  public static Parser getParser() { return "
                    + lce.parser + "; }");

      if (testExFilePath != null
          && lce.featuresStatus != RevisionAnalysis.UNAFFECTED
          && new File(testExFilePath).exists())
        out.println(
            "  public static Parser getTestParser() { return new "
            + "LBJ2.parse.ArrayFileParser(\""
            + new File(testExFilePath).getAbsolutePath() + "\"); }");
      else
        out.println("  public static Parser getTestParser() { return "
                    + lce.testParser + "; }\n");

      TranslateToJava.generateLearnerBody(out, lce);

      if (lce.parameterSets.size() > 0) {
        out.println();
        out.println("  public static class Parameters extends "
                    + lce.learnerName + ".Parameters");
        out.println("  {");
        out.println(
            "    public Parameters() { super(getBestParameters()); }");
        out.println("  }");
      }

      out.println("}\n");
      out.close();

      executeReadyThreads(getName());
    }
  }
}

