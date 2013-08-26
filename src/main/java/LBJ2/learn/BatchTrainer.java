package LBJ2.learn;

import java.lang.reflect.Field;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import LBJ2.learn.Learner;
import LBJ2.parse.FoldSeparator;
import LBJ2.parse.FoldParser;
import LBJ2.parse.Parser;
import LBJ2.parse.ArrayFileParser;
import LBJ2.util.ExceptionlessInputStream;
import LBJ2.util.ExceptionlessOutputStream;
import LBJ2.util.TableFormat;
import LBJ2.util.Sort;
import LBJ2.util.StudentT;


/**
  * Use this class to batch train a {@link Learner}.
  *
  * @author Nick Rizzolo
 **/
public class BatchTrainer
{
  /** <!-- writeExample(ExceptionlessOutputStream,int[],double[],int[],double[]) -->
    * Writes an example vector to the specified stream, with all features
    * being written in the order they appear in the vector.
    *
    * @param out            The output stream.
    * @param featureIndexes The lexicon indexes of the features.
    * @param featureValues  The values or "strengths" of the features.
    * @param labelIndexes   The lexicon indexes of the labels.
    * @param labelValues    The values or "strengths" of the labels.
   **/
  public static void writeExample(ExceptionlessOutputStream out,
                                  int[] featureIndexes,
                                  double[] featureValues, int[] labelIndexes,
                                  double[] labelValues) {
    writeExample(out, featureIndexes, featureValues, labelIndexes,
                 labelValues, featureIndexes.length, null);
  }


  /** <!-- writeExample(ExceptionlessOutputStream,int[],double[],int[],double[],int) -->
    * Writes an example vector to the specified stream, with all features
    * being written in the order they appear in the vector.
    *
    * @param out            The output stream.
    * @param featureIndexes The lexicon indexes of the features.
    * @param featureValues  The values or "strengths" of the features.
    * @param labelIndexes   The lexicon indexes of the labels.
    * @param labelValues    The values or "strengths" of the labels.
    * @param unpruned       The number of features in the vector that aren't
    *                       pruned.
   **/
  public static void writeExample(ExceptionlessOutputStream out,
                                  int[] featureIndexes,
                                  double[] featureValues, int[] labelIndexes,
                                  double[] labelValues, int unpruned) {
    writeExample(out, featureIndexes, featureValues, labelIndexes,
                 labelValues, unpruned, null);
  }


  /** <!-- writeExample(ExceptionlessOutputStream,int[],double[],int[],double[],Lexicon) -->
    * Writes an example vector contained in an object array to the underlying
    * output stream, with features sorted according to their representations
    * in the given lexicon if present, or in the order they appear in the
    * vector otherwise.
    *
    * @param out            The output stream.
    * @param featureIndexes The lexicon indexes of the features.
    * @param featureValues  The values or "strengths" of the features.
    * @param labelIndexes   The lexicon indexes of the labels.
    * @param labelValues    The values or "strengths" of the labels.
    * @param lex            A lexicon.
   **/
  public static void writeExample(ExceptionlessOutputStream out,
                                  int[] featureIndexes,
                                  double[] featureValues, int[] labelIndexes,
                                  double[] labelValues, Lexicon lex) {
    writeExample(out, featureIndexes, featureValues, labelIndexes,
                 labelValues, featureIndexes.length, lex);
  }


  /** <!-- writeExample(ExceptionlessOutputStream,int[],double[],int[],double[],int,Lexicon) -->
    * Writes an example vector contained in an object array to the underlying
    * output stream, with features sorted according to their representations
    * in the given lexicon if present, or in the order they appear in the
    * vector otherwise.
    *
    * @param out            The output stream.
    * @param featureIndexes The lexicon indexes of the features.
    * @param featureValues  The values or "strengths" of the features.
    * @param labelIndexes   The lexicon indexes of the labels.
    * @param labelValues    The values or "strengths" of the labels.
    * @param unpruned       The number of features in the vector that aren't
    *                       pruned.
    * @param lexicon        A lexicon.
   **/
  public static void writeExample(ExceptionlessOutputStream out,
                                  final int[] featureIndexes,
                                  double[] featureValues, int[] labelIndexes,
                                  double[] labelValues, int unpruned,
                                  final Lexicon lexicon) {
    int[] I = null;
    if (lexicon != null) {
      I = new int[featureIndexes.length];
      for (int i = 0; i < I.length; ++i) I[i] = i;
      Sort.sort(I, 0, unpruned,
        new Sort.IntComparator() {
          public int compare(int i1, int i2) {
            return lexicon.lookupKey(featureIndexes[i1])
                   .compareTo(lexicon.lookupKey(featureIndexes[i2]));
          }
        });
    }

    out.writeInt(labelIndexes.length);
    for (int i = 0; i < labelIndexes.length; ++i) {
      out.writeInt(labelIndexes[i]);
      out.writeDouble(labelValues[i]);
    }

    out.writeInt(unpruned);
    out.writeInt(featureIndexes.length - unpruned);

    if (lexicon == null) {
      for (int i = 0; i < featureIndexes.length; ++i) {
        out.writeInt(featureIndexes[i]);
        out.writeDouble(featureValues[i]);
      }
    }
    else {
      for (int i = 0; i < featureIndexes.length; ++i) {
        out.writeInt(featureIndexes[I[i]]);
        out.writeDouble(featureValues[I[i]]);
      }
    }
  }


  // Instance member variables.
  /** The learning classifier being trained. */
  protected Learner learner;
  /** The parser from which training data for {@link #learner} is received. */
  protected Parser parser;
  /**
    * The number of training examples in between status messages printed to
    * <code>STDOUT</code>, or 0 to suppress these messages.
   **/
  protected int progressOutput;
  /** Spacing for making status messages prettier. */
  protected String messageIndent;
  /** {@link #learner}'s class. */
  protected Class learnerClass;
  /** {@link #learner}'s <code>isTraining</code> field. */
  protected Field fieldIsTraining;
  /** The number of examples extracted during pre-extraction. */
  protected int examples;
  /** The number of features extracted during pre-extraction. */
  protected int lexiconSize;


  // Constructors.
  /** <!-- <init>(Learner,String) -->
    * Creates a new trainer that doesn't produce status messages.
    *
    * @param l  The learner to be trained.
    * @param p  The path to an example file.
   **/
  public BatchTrainer(Learner l, String p) { this(l, p, true); }

  /** <!-- <init>(Learner,String,int) -->
    * Creates a new trainer that produces status messages.
    *
    * @param l  The learner to be trained.
    * @param p  The path to an example file.
    * @param o  The number of examples in between status messages on STDOUT.
   **/
  public BatchTrainer(Learner l, String p, int o) { this(l, p, true, o); }

  /** <!-- <init>(Learner,String,int,String) -->
    * Creates a new trainer that produces status messages with the specified
    * indentation spacing for status messages.
    *
    * @param l  The learner to be trained.
    * @param p  The path to an example file.
    * @param o  The number of examples in between status messages on STDOUT.
    * @param i  The indentation spacing for status messages.
   **/
  public BatchTrainer(Learner l, String p, int o, String i) {
    this(l, p, true, o, i);
  }

  /** <!-- <init>(Learner,String,boolean) -->
    * Creates a new trainer that doesn't produce status messages.
    *
    * @param l  The learner to be trained.
    * @param p  The path to an example file.
    * @param z  Whether or not the example file is compressed.
   **/
  public BatchTrainer(Learner l, String p, boolean z) {
    this(l, new ArrayFileParser(p, z));
  }

  /** <!-- <init>(Learner,String,boolean,int) -->
    * Creates a new trainer that produces status messages.
    *
    * @param l  The learner to be trained.
    * @param p  The path to an example file.
    * @param z  Whether or not the example file is compressed.
    * @param o  The number of examples in between status messages on STDOUT.
   **/
  public BatchTrainer(Learner l, String p, boolean z, int o) {
    this(l, new ArrayFileParser(p, z), o);
  }

  /** <!-- <init>(Learner,String,boolean,int,String) -->
    * Creates a new trainer that produces status messages with the specified
    * indentation spacing for status messages.
    *
    * @param l  The learner to be trained.
    * @param p  The path to an example file.
    * @param z  Whether or not the example file is compressed.
    * @param o  The number of examples in between status messages on STDOUT.
    * @param i  The indentation spacing for status messages.
   **/
  public BatchTrainer(Learner l, String p, boolean z, int o, String i) {
    this(l, new ArrayFileParser(p, z), o, i);
  }

  /** <!-- <init>(Learner,Parser) -->
    * Creates a new trainer that doesn't produce status messages.
    *
    * @param l  The learner to be trained.
    * @param p  The parser from which training data is received.
   **/
  public BatchTrainer(Learner l, Parser p) { this(l, p, 0); }

  /** <!-- <init>(Learner,Parser,int) -->
    * Creates a new trainer that produces status messages.
    *
    * @param l  The learner to be trained.
    * @param p  The parser from which training data is received.
    * @param o  The number of examples in between status messages on STDOUT.
   **/
  public BatchTrainer(Learner l, Parser p, int o) { this(l, p, o, ""); }

  /** <!-- <init>(Learner,Parser,int,String) -->
    * Creates a new trainer that produces status messages with the specified
    * indentation spacing for status messages.
    *
    * @param l  The learner to be trained.
    * @param p  The parser from which training data is received.
    * @param o  The number of examples in between status messages on STDOUT.
    * @param i  The indentation spacing for status messages.
   **/
  public BatchTrainer(Learner l, Parser p, int o, String i) {
    learner = l;
    parser = p;
    progressOutput = o;
    messageIndent = i;

    learnerClass = learner.getClass();
    try { fieldIsTraining = learnerClass.getField("isTraining"); }
    catch (Exception e) {
      System.err.println("Can't access " + learnerClass
                         + "'s 'isTraining' field: " + e);
      System.exit(1);
    }
  }


  /** Returns the value of {@link #progressOutput}. */
  public int getProgressOutput() { return progressOutput; }
  /** Returns the value of {@link #parser}. */
  public Parser getParser() { return parser; }


  /** <!-- setIsTraining(boolean) -->
    * Sets the static <code>isTraining</code> flag inside {@link #learner}'s
    * runtime class to the specified value.  This probably doesn't need to
    * be tinkered with after pre-extraction, since it can only affect the
    * code that does the extraction.
    *
    * @param b  The new value for the flag.
   **/
  protected void setIsTraining(boolean b) {
    try { fieldIsTraining.setBoolean(null, b); }
    catch (Exception e) {
      System.err.println("Can't set " + learnerClass
                         + "'s 'isTraining' field: " + e);
      System.exit(1);
    }
  }


  /** <!-- getIsTraining() -->
    * Returns the value of the static <code>isTraining</code> flag inside
    * {@link #learner}'s runtime class.
   **/
  protected boolean getIsTraining() {
    try { return fieldIsTraining.getBoolean(null); }
    catch (Exception e) {
      System.err.println("Can't get " + learnerClass
                         + "'s 'isTraining' field: " + e);
      System.exit(1);
    }
    return false;
  }


  /** <!-- preExtract(String) -->
    * Performs labeled feature vector pre-extraction into the specified file
    * (or memory), replacing {@link #parser} with one that reads from that
    * file (or memory).  After pre-extraction, the lexicon is written to disk.
    * It is assumed that {@link #learner} already knows where to write the
    * lexicon.  If it doesn't, call {@link Learner#setLexiconLocation(String)}
    * or {@link Learner#setLexiconLocation(java.net.URL)} on that object
    * before calling this method.
    *
    * <p> Calling this method is equivalent to calling
    * {@link #preExtract(String,boolean)} with the second argument
    * <code>true</code>.
    *
    * @param exampleFile  The full path to a file into which examples will be
    *                     written, or <code>null</code> to extract into
    *                     memory.
    * @return The resulting lexicon.
   **/
  public Lexicon preExtract(String exampleFile) {
    return preExtract(exampleFile, true);
  }


  /** <!-- preExtract(String,boolean) -->
    * Performs labeled feature vector pre-extraction into the specified file
    * (or memory), replacing {@link #parser} with one that reads from that
    * file (or memory).  After pre-extraction, the lexicon is written to disk.
    * It is assumed that {@link #learner} already knows where to write the
    * lexicon.  If it doesn't, call {@link Learner#setLexiconLocation(String)}
    * or {@link Learner#setLexiconLocation(java.net.URL)} on that object
    * before calling this method.
    *
    * @param exampleFile  The full path to a file into which examples will be
    *                     written, or <code>null</code> to extract into
    *                     memory.
    * @param zip          Whether or not to compress the extracted examples.
    * @return The resulting lexicon.
   **/
  public Lexicon preExtract(String exampleFile, boolean zip) {
    Learner preExtractLearner =
      preExtract(exampleFile, zip, Lexicon.CountPolicy.none);
    preExtractLearner.saveLexicon();
    return preExtractLearner.getLexicon();
  }


  /** <!-- preExtract(String,Lexicon.CountPolicy) -->
    * Performs labeled feature vector pre-extraction into the specified file
    * (or memory), replacing {@link #parser} with one that reads from that
    * file (or memory).  If <code>exampleFile</code> already exists, this
    * method writes the examples to a temporary file, then copies the contents
    * to the existing file after pre-extraction completes.  This is done in
    * case the parser providing the examples to this method is reading the
    * existing file.
    *
    * <p> Note that this method does <i>not</i> write the feature lexicon it
    * produces to disk.  Calling this method is equivalent to calling
    * {@link #preExtract(String,boolean,Lexicon.CountPolicy)} with the second
    * argument <code>true</code>.
    *
    * @param exampleFile  The full path to a file into which examples will be
    *                     written, or <code>null</code> to extract into
    *                     memory.
    * @param countPolicy  The feature counting policy for the learner's
    *                     feature lexicon.
    * @return A new learning classifier containing the lexicon built during
    *         pre-extraction.
   **/
  public Learner preExtract(String exampleFile,
                            Lexicon.CountPolicy countPolicy) {
    return preExtract(exampleFile, true, countPolicy);
  }


  /** <!-- preExtract(String,boolean,Lexicon.CountPolicy) -->
    * Performs labeled feature vector pre-extraction into the specified file
    * (or memory), replacing {@link #parser} with one that reads from that
    * file (or memory).  If <code>exampleFile</code> already exists, this
    * method writes the examples to a temporary file, then copies the contents
    * to the existing file after pre-extraction completes.  This is done in
    * case the parser providing the examples to this method is reading the
    * existing file.
    *
    * <p> Note that this method does <i>not</i> write the feature lexicon it
    * produces to disk.
    *
    * @param exampleFile  The full path to a file into which examples will be
    *                     written, or <code>null</code> to extract into
    *                     memory.
    * @param zip          Whether or not to compress the extracted examples.
    * @param countPolicy  The feature counting policy for the learner's
    *                     feature lexicon.
    * @return A new learning classifier containing the lexicon built during
    *         pre-extraction.
   **/
  public Learner preExtract(String exampleFile, boolean zip,
                            Lexicon.CountPolicy countPolicy) {
    Learner preExtractLearner = learner.emptyClone();
    preExtractLearner.setLabelLexicon(learner.getLabelLexicon());
    Lexicon lexicon = learner.getLexicon();
    preExtractLearner.setLexicon(lexicon);
    preExtractLearner.countFeatures(countPolicy);
    learner.setLexicon(null);
    setIsTraining(true);
    examples = 0;

    // Establish an output stream for writing examples.
    ExceptionlessOutputStream eos = null;
    ByteArrayOutputStream baos = null;
    File fExampleFile = null;
    File fTempFile = null;
    boolean copy = false;

    if (exampleFile != null) {
      fExampleFile = new File(exampleFile);
      if (fExampleFile.exists()) {
        int lastSlash = exampleFile.lastIndexOf(File.separatorChar);

        try {
          if (lastSlash == -1) fTempFile = File.createTempFile("LBJ", null);
          else
            fTempFile =
              File.createTempFile(
                  "LBJ", null, new File(exampleFile.substring(0, lastSlash)));
        }
        catch (Exception e) {
          System.err.println(
              "LBJ ERROR: BatchTrainer.preExtract: Can't create temporary "
              + "file: " + e);
          System.exit(1);
        }

        fTempFile.deleteOnExit();
        copy = true;
      }
      else fTempFile = fExampleFile;

      try {
        if (zip)
          eos =
            ExceptionlessOutputStream.openCompressedStream(
                fTempFile.toURI().toURL());
        else
          eos =
            ExceptionlessOutputStream.openBufferedStream(
                fTempFile.toURI().toURL());
      }
      catch (Exception e) {
        System.err.println(
            "LBJ ERROR: BatchTrainer.preExtract: Can't convert file name '"
            + fTempFile + "' to URL: " + e);
        System.exit(1);
      }
    }
    else {
      baos = new ByteArrayOutputStream(1 << 18);
      if (zip) {
        ZipOutputStream zos = new ZipOutputStream(baos);
        try {
          zos.putNextEntry(
              new ZipEntry(ExceptionlessInputStream.zipEntryName));
        }
        catch (Exception e) {
          System.err.println("ERROR: Can't create in-memory zip data:");
          e.printStackTrace();
          System.exit(1);
        }
        eos = new ExceptionlessOutputStream(new BufferedOutputStream(zos));
      }
      else eos = new ExceptionlessOutputStream(baos);
    }

    // Write examples to the output stream.
    boolean alreadyExtracted = parser instanceof ArrayFileParser;
    if (alreadyExtracted) ((ArrayFileParser) parser).setIncludePruned(true);

    for (Object example = parser.next(); example != null;
         example = parser.next()) {
      if (progressOutput > 0 && examples % progressOutput == 0)
        System.out.println(
            "  " + learner.name + ", pre-extract: " + messageIndent + examples
            + " examples at " + new Date());

      if (example == FoldSeparator.separator) eos.writeInt(-1);
      else {
        ++examples;
        Object[] exampleArray =
          alreadyExtracted ? (Object[]) example
                           : preExtractLearner.getExampleArray(example);

        int[] featureIndexes = (int[]) exampleArray[0];
        double[] featureValues = (double[]) exampleArray[1];
        int[] labelIndexes = (int[]) exampleArray[2];
        double[] labelValues = (double[]) exampleArray[3];

        if (alreadyExtracted && countPolicy != Lexicon.CountPolicy.none) {
          int labelIndex =
            countPolicy == Lexicon.CountPolicy.perClass
            ? labelIndexes[0] : -1;
          for (int i = 0; i < featureIndexes.length; ++i)
            lexicon.lookup(lexicon.lookupKey(featureIndexes[i]), true,
                           labelIndex);
        }

        writeExample(eos, featureIndexes, featureValues, labelIndexes,
                     labelValues, lexicon);
      }
    }

    if (progressOutput > 0)
      System.out.println(
          "  " + learner.name + ", pre-extract: " + messageIndent + examples
          + " examples at " + new Date());

    parser.close();
    eos.close();

    if (copy) {
      try {
        FileChannel in = (new FileInputStream(fTempFile)).getChannel();
        FileChannel out = (new FileOutputStream(fExampleFile)).getChannel();
        in.transferTo(0, fTempFile.length(), out);
        in.close();
        out.close();
      }
      catch (Exception e) {
        System.err.println("LBJ ERROR: Can't copy example file:");
        e.printStackTrace();
        System.exit(1);
      }
    }

    setIsTraining(false);
    lexiconSize = preExtractLearner.getLexicon().size();

    // Set up a new parser to read the pre-extracted examples.
    if (fTempFile != null)
      parser = new ArrayFileParser(fTempFile.getPath(), zip);
    else parser = new ArrayFileParser(baos.toByteArray(), zip);

    learner.setLabelLexicon(preExtractLearner.getLabelLexicon());
    return preExtractLearner;
  }


  /** <!-- fillInSizes() -->
    * This method sets the {@link #examples} and {@link #lexiconSize}
    * variables by querying {@link #parser} and {@link #learner} respectively.
    * It sets {@link #examples} to 0 if {@link #parser} is not an
    * {@link LBJ2.parse.ArrayFileParser} and {@link #lexiconSize} to 0 if
    * {@link #learner} doesn't either have the lexicon loaded or know where to
    * find it.
   **/
  public void fillInSizes() {
    if (parser instanceof ArrayFileParser) {
      ArrayFileParser afp = (ArrayFileParser) parser;
      examples = afp.getNumExamples();
    }
    else examples = 0;
    lexiconSize = learner.getPrunedLexiconSize();
  }


  /** <!-- pruneDataset(String,Lexicon.PruningPolicy,Learner) -->
    * Prunes the data returned by {@link #parser} according to the given
    * policy, under the assumption that feature counts have already been
    * compiled in the given learner's lexicon.  The pruned data is written to
    * the given file (or memory), and at the end of the method,
    * {@link #parser} is replaced with a new parser that reads from that file
    * (or memory).  The pruned lexicon is also written to disk.
    *
    * <p> If <code>exampleFile</code> already exists, this method writes the
    * examples to a temporary file, then copies the contents to the existing
    * file after pruning completes.  This is done in case the parser providing
    * the examples to this method is reading the existing file.
    *
    * <p> When calling this method, it must be the case that {@link #parser}
    * is a {@link LBJ2.parse.ArrayFileParser}.  This condition is easy to
    * satisfy, since the
    * {@link #preExtract(String,boolean,Lexicon.CountPolicy)} method will
    * usually be called prior to this method to count the features in the
    * dataset, and this method also replaces {@link #parser} with a
    * {@link LBJ2.parse.ArrayFileParser}.
    *
    * <p> It is assumed that <code>preExtractLearner</code> already knows
    * where to write the lexicon.  If it doesn't, call
    * {@link Learner#setLexiconLocation(String)} or
    * {@link Learner#setLexiconLocation(java.net.URL)} on that object before
    * calling this method.
    *
    * <p> Calling this method is equivalent to calling
    * {@link #pruneDataset(String,boolean,Lexicon.PruningPolicy,Learner)} with
    * the second argument <code>true</code>.
    *
    * @param exampleFile        The full path to a file into which examples
    *                           will be written, or <code>null</code> to
    *                           extract into memory.
    * @param policy             The type of feature pruning.
    * @param preExtractLearner  A learner whose lexicon contains all the
    *                           necessary feature count information.
   **/
  public void pruneDataset(String exampleFile, Lexicon.PruningPolicy policy,
                           Learner preExtractLearner) {
    pruneDataset(exampleFile, true, policy, preExtractLearner);
  }


  /** <!-- pruneDataset(String,boolean,Lexicon.PruningPolicy,Learner) -->
    * Prunes the data returned by {@link #parser} according to the given
    * policy, under the assumption that feature counts have already been
    * compiled in the given learner's lexicon.  The pruned data is written to
    * the given file (or memory), and at the end of the method,
    * {@link #parser} is replaced with a new parser that reads from that file
    * (or memory).  The pruned lexicon is also written to disk.
    *
    * <p> If <code>exampleFile</code> already exists, this method writes the
    * examples to a temporary file, then copies the contents to the existing
    * file after pruning completes.  This is done in case the parser providing
    * the examples to this method is reading the existing file.
    *
    * <p> When calling this method, it must be the case that {@link #parser}
    * is an {@link LBJ2.parse.ArrayFileParser ArrayFileParser}.  This
    * condition is easy to satisfy, since the
    * {@link #preExtract(String,boolean,Lexicon.CountPolicy)} method will
    * usually be called prior to this method to count the features in the
    * dataset, and this method also replaces {@link #parser} with an
    * {@link LBJ2.parse.ArrayFileParser ArrayFileParser}.
    *
    * <p> It is assumed that <code>preExtractLearner</code> already knows
    * where to write the lexicon.  If it doesn't, call
    * {@link Learner#setLexiconLocation(String)} or
    * {@link Learner#setLexiconLocation(java.net.URL)} on that object before
    * calling this method.
    *
    * @param exampleFile        The full path to a file into which examples
    *                           will be written, or <code>null</code> to
    *                           extract into memory.
    * @param zip                Whether or not to compress the extracted
    *                           examples.
    * @param policy             The type of feature pruning.
    * @param preExtractLearner  A learner whose lexicon contains all the
    *                           necessary feature count information.
   **/
  public void pruneDataset(String exampleFile, boolean zip,
                           Lexicon.PruningPolicy policy,
                           Learner preExtractLearner) {
    Lexicon lexicon = preExtractLearner.getLexicon();

    if (!policy.isNone()
        && lexicon.getCountPolicy() == Lexicon.CountPolicy.none)
      throw new IllegalArgumentException(
          "LBJ ERROR: BatchTrainer.pruneDataset: Can't prune with policy '"
          + policy + "' if features haven't been counted.");
    if (!(parser instanceof ArrayFileParser))
      throw new IllegalArgumentException(
          "LBJ ERROR: BatchTrainer.pruneDataset can't be called unless "
          + "feature pre-extraction has already been performed.");
    ArrayFileParser afp = (ArrayFileParser) parser;
    afp.setIncludePruned(true);

    int[] swapMap = lexicon.prune(policy);

    // Establish an output stream for writing examples.
    ExceptionlessOutputStream eos = null;
    ByteArrayOutputStream baos = null;
    File fExampleFile = null;
    File fTempFile = null;
    boolean copy = false;

    if (exampleFile != null) {
      fExampleFile = new File(exampleFile);
      if (fExampleFile.exists()) {
        int lastSlash = exampleFile.lastIndexOf(File.separatorChar);

        try {
          if (lastSlash == -1) fTempFile = File.createTempFile("LBJ", null);
          else
            fTempFile =
              File.createTempFile(
                  "LBJ", null, new File(exampleFile.substring(0, lastSlash)));
        }
        catch (Exception e) {
          System.err.println(
              "LBJ ERROR: BatchTrainer.preExtract: Can't create temporary "
              + "file: " + e);
          System.exit(1);
        }

        fTempFile.deleteOnExit();
        copy = true;
      }
      else fTempFile = fExampleFile;

      try {
        if (zip)
          eos =
            ExceptionlessOutputStream.openCompressedStream(
                fTempFile.toURI().toURL());
        else
          eos =
            ExceptionlessOutputStream.openBufferedStream(
                fTempFile.toURI().toURL());
      }
      catch (Exception e) {
        System.err.println(
            "LBJ ERROR: BatchTrainer.preExtract: Can't convert file name '"
            + fTempFile + "' to URL: " + e);
        System.exit(1);
      }
    }
    else {
      baos = new ByteArrayOutputStream(1 << 18);
      if (zip) {
        ZipOutputStream zos = new ZipOutputStream(baos);
        try {
          zos.putNextEntry(
              new ZipEntry(ExceptionlessInputStream.zipEntryName));
        }
        catch (Exception e) {
          System.err.println("ERROR: Can't create in-memory zip data:");
          e.printStackTrace();
          System.exit(1);
        }
        eos = new ExceptionlessOutputStream(new BufferedOutputStream(zos));
      }
      else eos = new ExceptionlessOutputStream(baos);
    }

    // Write examples to the output stream.
    examples = 0;

    for (Object example = afp.next(); example != null; example = afp.next()) {
      if (progressOutput > 0 && examples % progressOutput == 0)
        System.out.println("  " + learner.name + ", pruning: " + examples
                           + " examples at " + new Date());

      if (example == FoldSeparator.separator) eos.writeInt(-1);
      else {
        ++examples;
        Object[] exampleArray = (Object[]) example;

        int[] featureIndexes = (int[]) exampleArray[0];
        double[] featureValues = (double[]) exampleArray[1];
        int[] labelIndexes = (int[]) exampleArray[2];
        double[] labelValues = (double[]) exampleArray[3];

        int unpruned = featureIndexes.length;
        if (swapMap != null) {
          // First, map the old feature indexes to the new ones.
          for (int i = 0; i < featureIndexes.length; ++i)
            featureIndexes[i] = swapMap[featureIndexes[i]];

          // Second, put the pruned features at the end of the example array.
          while (unpruned > 0
                 && lexicon.isPruned(featureIndexes[unpruned - 1],
                                     labelIndexes[0], policy))
            --unpruned;

          for (int i = unpruned - 2; i >= 0; --i)
            if (lexicon.isPruned(featureIndexes[i], labelIndexes[0], policy))
            {
              int t = featureIndexes[i];
              featureIndexes[i] = featureIndexes[--unpruned];
              featureIndexes[unpruned] = t;

              double d = featureValues[i];
              featureValues[i] = featureValues[unpruned];
              featureValues[unpruned] = d;
            }
        }

        writeExample(eos, featureIndexes, featureValues, labelIndexes,
                     labelValues, unpruned, lexicon);
      }
    }

    if (progressOutput > 0)
      System.out.println("  " + learner.name + ", pruning: " + examples
                         + " examples at " + new Date());

    parser.close();
    eos.close();

    if (copy) {
      try {
        FileChannel in = (new FileInputStream(fTempFile)).getChannel();
        FileChannel out = (new FileOutputStream(fExampleFile)).getChannel();
        in.transferTo(0, fTempFile.length(), out);
        in.close();
        out.close();
      }
      catch (Exception e) {
        System.err.println("LBJ ERROR: Can't copy example file:");
        e.printStackTrace();
        System.exit(1);
      }
    }

    lexiconSize = lexicon.getCutoff();
    preExtractLearner.saveLexicon();

    // Set up a new parser to read the pre-extracted and pruned examples.
    if (fTempFile != null)
      parser = new ArrayFileParser(fTempFile.getPath(), zip);
    else parser = new ArrayFileParser(baos.toByteArray(), zip);
  }


  /** <!-- interface DoneWithRound -->
    * Provides access to a hook into {@link #train(int)} so that additional
    * processing can be performed at the end of each round.  This processing
    * supplements the processing in {@link Learner#doneWithRound()} which is
    * already called from withink {@link #train(int)}.
   **/
  public static interface DoneWithRound
  {
    /**
      * The hook into {@link #train(int)} as described above.
      *
      * @param r  The 1-based number of the training round that just
      *           completed.
     **/
    public void doneWithRound(int r);
  }


  /** <!-- train(int) -->
    * Trains {@link #learner} for the specified number of rounds.  This
    * learning happens on top of any learning that {@link #learner} may have
    * already done.
    *
    * @param rounds The number of passes to make over the training data.
   **/
  public void train(int rounds) { train(1, rounds); }


  /** <!-- train(int,int) -->
    * Trains {@link #learner} for the specified number of rounds.  This
    * learning happens on top of any learning that {@link #learner} may have
    * already done.
    *
    * @param start  The 1-based number of the first training round.
    * @param rounds The total number of training rounds including those before
    *               <code>start</code>.
   **/
  public void train(int start, int rounds) {
    train(start, rounds,
          new DoneWithRound() { public void doneWithRound(int r) { } });
  }


  /** <!-- train(int,DoneWithRound) -->
    * Trains {@link #learner} for the specified number of rounds.  This
    * learning happens on top of any learning that {@link #learner} may have
    * already done.
    *
    * @param rounds The number of passes to make over the training data.
    * @param dwr    Performs post processing at the end of each round.
   **/
  public void train(int rounds, DoneWithRound dwr) {
    train(1, rounds, dwr);
  }


  /** <!-- train(int,int,DoneWithRound) -->
    * Trains {@link #learner} for the specified number of rounds.  This
    * learning happens on top of any learning that {@link #learner} may have
    * already done.
    *
    * @param start  The 1-based number of the first training round.
    * @param rounds The total number of training rounds including those before
    *               <code>start</code>.
    * @param dwr    Performs post processing at the end of each round.
   **/
  public void train(int start, int rounds, DoneWithRound dwr) {
    if (lexiconSize > 0) {
      // If the parser is a FoldParser, it means we're doing cross validation
      // in which we train on just part of the data.  So the examples variable
      // doesn't accurately reflect how many training examples we'll see in
      // this episode of training.
      learner.initialize(parser instanceof FoldParser ? 0 : examples,
                         lexiconSize);
    }
    else setIsTraining(true);

    for (int i = start; i <= rounds; ++i) {
      int examples = 0;

      for (Object example = parser.next(); example != null;
           example = parser.next()) {
        if (example == FoldSeparator.separator) continue;

        if (progressOutput > 0 && examples % progressOutput == 0) {
          System.out.print("  " + learner.name + ": " + messageIndent);
          if (rounds != 1) System.out.print("Round " + i + ", ");
          System.out.println(examples + " examples processed at "
                             + new Date());
        }

        learner.learn(example);
        ++examples;
      }

      if (progressOutput > 0) {
        System.out.print("  " + learner.name + ": " + messageIndent);
        if (rounds != 1) System.out.print("Round " + i + ", ");
        System.out.println(examples + " examples processed at " + new Date());
      }

      parser.reset();
      learner.doneWithRound();
      dwr.doneWithRound(i);
    }

    learner.doneLearning();
    if (lexiconSize == 0) setIsTraining(false);
  }


  /** <!-- crossValidation(int[],int,FoldParser.SplitPolicy,double,TestingMetric,boolean) -->
    * Performs cross validation, computing a confidence interval on the
    * performance of the learner after each of the specified rounds of
    * training.  This method assumes that {@link #learner} has not yet done
    * any learning.  The learner will again be empty in this sense when the
    * method exits, except that any label lexicon present before the method
    * was called will be restored.  The label lexicon needs to persist in this
    * way so that it can ultimately be written into the model file.
    *
    * @param rounds         An array of training rounds after which
    *                       performance of the learner should be evaluated on
    *                       the testing data.
    * @param k              The number of folds.
    * @param splitPolicy    The policy according to which the data is split
    *                       up.
    * @param alpha          The fraction of the distribution to leave outside
    *                       the confidence interval.  For example, <code>alpha
    *                       = .05</code> gives a 95% confidence interval.
    * @param metric         A metric with which to evaluate the learner on
    *                       testing data.
    * @param statusMessages If set <code>true</code> status messages will be
    *                       produced, even if {@link #progressOutput} is zero.
    * @return A 2D array <code>results</code> where <code>results[i][0]</code>
    *         is the average performance of the learner after
    *         <code>rounds[i]</code> rounds of training and
    *         <code>results[i][1]</code> is half the size of the corresponding
    *         confidence interval.
   **/
  public double[][] crossValidation(final int[] rounds,
                                    int k,
                                    FoldParser.SplitPolicy splitPolicy,
                                    double alpha,
                                    final TestingMetric metric,
                                    boolean statusMessages) {
    if (!(k > 1 || splitPolicy == FoldParser.SplitPolicy.manual))
      throw new IllegalArgumentException(
          "LBJ ERROR: BatchTrainer.crossValidation: if the data splitting "
          + "policy is not 'Manual', the number of folds must be greater "
          + "than 1.");
    if (splitPolicy == FoldParser.SplitPolicy.manual) k = -1;
    Arrays.sort(rounds);
    final int totalRounds = rounds[rounds.length - 1];

    // Status messages.
    if (statusMessages || progressOutput > 0) {
      System.out.print("  " + learner.name + ": " + messageIndent
                       + "Cross Validation: ");
      if (k != -1) System.out.print("k = " + k + ", ");
      System.out.print("Split = " + splitPolicy);
      if (totalRounds != 1) System.out.print(", Rounds = " + totalRounds);
      System.out.println();
    }

    // Instantiate a fold parser.
    final FoldParser foldParser;
    // If we pre-extracted, we know how many examples there are already;
    // otherwise FoldParser will have to compute it.
    if (examples > 0)
      foldParser = new FoldParser(parser, k, splitPolicy, 0, false, examples);
    else foldParser = new FoldParser(parser, k, splitPolicy, 0, false);
    parser = foldParser;

    if (splitPolicy == FoldParser.SplitPolicy.manual) k = foldParser.getK();

    final double[][] performances = new double[rounds.length][k];
    Lexicon labelLexicon = learner.getLabelLexicon();

    // Train and get testing performances for each fold.
    for (int i = 0; i < k; foldParser.setPivot(++i)) {
      if (statusMessages || progressOutput > 0)
        System.out.println(
            "  " + learner.name + ": " + messageIndent
            + "Training against subset " + i + " at " + new Date());
      final int fold = i;
      messageIndent += "  ";

      train(totalRounds,
            new DoneWithRound() {
              int r = 0;
              public void doneWithRound(int round) {
                if (round < totalRounds && rounds[r] == round)
                  performances[r++][fold] =
                    crossValidationTesting(foldParser, metric, true, false);
              }
            });

      performances[rounds.length - 1][i] =
        crossValidationTesting(foldParser, metric, false, statusMessages);
      messageIndent = messageIndent.substring(2);

      learner.forget();
      if (labelLexicon != null && labelLexicon.size() > 0
          && learner.getLabelLexicon().size() == 0)
        learner.setLabelLexicon(labelLexicon);
    }

    parser = foldParser.getParser();

    // Compute the confidence interval.
    double[][] results = new double[rounds.length][];
    boolean usingAccuracy = metric instanceof Accuracy;

    for (int r = 0; r < rounds.length; ++r) {
      results[r] = StudentT.confidenceInterval(performances[r], alpha);

      if (r == rounds.length - 1 && statusMessages || progressOutput > 0) {
        double mean = Math.round(results[r][0] * 100000) / 100000.0;
        double half = Math.round(results[r][1] * 100000) / 100000.0;

        System.out.print(
            "  " + learner.name + ":   " + messageIndent + (100 * (1 - alpha))
            + "% confidence interval after " + rounds[r] + " rounds: "
            + mean);
        if (usingAccuracy) System.out.print("%");
        System.out.print(" +/- " + half);
        if (usingAccuracy) System.out.print("%");
        System.out.println();
      }
    }

    return results;
  }


  /** <!-- crossValidationTesting(FoldParser,TestingMetric,boolean,boolean) -->
    * Tests the learner as a subroutine inside cross validation.
    *
    * @param foldParser     The cross validation parser that splits up the
    *                       data.
    * @param metric         The metric used to evaluate the performance of the
    *                       learner.
    * @param clone          Whether or not the learner should be cloned (and
    *                       it should be cloned if more learning will take
    *                       place after making this call).
    * @param statusMessages If set <code>true</code> status messages will be
    *                       produced, even if {@link #progressOutput} is zero.
    * @return The result produced by the testing metric on the current cross
    *         validation fold expressed as a percentage (instead of a
    *         fraction) if the testing metric is {@link Accuracy}.
   **/
  protected double crossValidationTesting(FoldParser foldParser,
                                          TestingMetric metric,
                                          boolean clone,
                                          boolean statusMessages) {
    Parser originalParser = foldParser.getParser();
    foldParser.setFromPivot(true);

    Learner testLearner = learner;
    if (clone) {
      testLearner = (Learner) learner.clone();
      testLearner.doneLearning();
    }

    double result = 0;

    if (originalParser instanceof ArrayFileParser) {
      ArrayFileParser afp = (ArrayFileParser) originalParser;
      afp.setIncludePruned(true);
      result = metric.test(testLearner, null, foldParser);
      afp.setIncludePruned(false);
    }
    else {
      setIsTraining(false);
      result = metric.test(testLearner, testLearner.getLabeler(), foldParser);
      setIsTraining(true);
    }

    foldParser.reset();
    foldParser.setFromPivot(false);
    if (metric instanceof Accuracy) result *= 100;

    if (statusMessages || progressOutput > 0) {
      double printResult = Math.round(result * 100000) / 100000.0;
      System.out.print(
          "  " + learner.name + ": " + messageIndent + "Subset "
          + foldParser.getPivot() + " " + metric.getName() + ": "
          + printResult);
      if (metric instanceof Accuracy) System.out.print("%");
      System.out.println();
    }

    return result;
  }


  /** <!-- tune(Learner.Parameters[],int[],int,FoldParser.SplitPolicy,double,TestingMetric) -->
    * Tune learning algorithm parameters using cross validation.  Note that
    * this interface takes both an array of
    * {@link LBJ2.learn.Learner.Parameters} objects and an array of rounds.
    * As such, the value in the {@link LBJ2.learn.Learner.Parameters#rounds}
    * field is ignored during tuning.  It is also overwritten in each of the
    * {@link LBJ2.learn.Learner.Parameters} objects when the optimal number of
    * rounds is determined in terms of the other parameters in each object.
    * Finally, in addition to returning the parameters that got the best
    * performance, this method also sets {@link #learner} with those
    * parameters at the end of the method.
    *
    * <p> This method assumes that {@link #learner} has not yet done any
    * learning.  The learner will again be empty in this sense when the method
    * exits, except that any label lexicon present before the method was
    * called will be restored.  The label lexicon needs to persist in this way
    * so that it can ultimately be written into the model file.
    *
    * @param parameters   An array of parameter settings objects.
    * @param rounds       An array of training rounds after which performance
    *                     of the learner should be evaluated on the testing
    *                     data.
    * @param k            The number of folds.
    * @param splitPolicy  The policy according to which the data is split up.
    * @param alpha        The fraction of the distribution to leave outside
    *                     the confidence interval.  For example, <code>alpha =
    *                     .05</code> gives a 95% confidence interval.
    * @param metric       A metric with which to evaluate the learner.
    * @return The element of <code>parameters</code> that resulted in the best
    *         performance according to <code>metric</code>.
   **/
  public Learner.Parameters tune(Learner.Parameters[] parameters,
                                 int[] rounds,
                                 int k,
                                 FoldParser.SplitPolicy splitPolicy,
                                 double alpha,
                                 TestingMetric metric) {
    int best = -1;
    String[] parameterStrings = new String[parameters.length];
    double[][] scores = new double[parameters.length][];

    for (int i = 0; i < parameters.length; ++i) {
      parameterStrings[i] = parameters[i].nonDefaultString();

      // Status message.
      if (progressOutput > 0)
        System.out.println(
            "  " + learner.name + ": " + messageIndent + "Trying parameters ("
            + parameterStrings[i] + ")");

      learner.setParameters(parameters[i]);
      messageIndent += "  ";
      double[][] results =
        crossValidation(rounds, k, splitPolicy, alpha, metric, false);
      messageIndent = messageIndent.substring(2);

      // Update best scores, rounds, and parameters.
      int bestRounds = 0;
      if (best == -1 || results[0][0] > scores[best][0]) best = i;
      scores[i] = results[0];

      for (int j = 1; j < results.length; ++j)
        if (results[j][0] > scores[i][0]) {
          bestRounds = j;
          scores[i] = results[j];
          if (results[j][0] > scores[best][0]) best = i;
        }

      parameters[i].rounds = rounds[bestRounds];
    }

    if (progressOutput > 0) {
      // Print a table of results.
      double[][] data = new double[parameters.length][4];
      for (int i = 0; i < parameters.length; ++i) {
        data[i][0] = i + 1;
        data[i][1] = scores[i][0];
        data[i][2] = scores[i][1];
        data[i][3] = parameters[i].rounds;
      }

      String[] columnLabels = { "Set", metric.getName(), "+/-", "Rounds" };
      int[] sigDigits = { 0, 3, 3, 0 };
      String[] s =
        TableFormat.tableFormat(columnLabels, null, data, sigDigits,
                                new int[]{ 0 });

      System.out.println("  " + learner.name + ": " + messageIndent + "----");
      System.out.println(
          "  " + learner.name + ": " + messageIndent + "Parameter sets:");
      for (int i = 0; i < parameterStrings.length; ++i)
        System.out.println(
            "  " + learner.name + ": " + messageIndent + (i+1) + ": "
            + parameterStrings[i]);
      for (int i = 0; i < s.length; ++i)
        System.out.println("  " + learner.name + ": " + messageIndent + s[i]);
      System.out.println("  " + learner.name + ": " + messageIndent + "----");

      // Status message.
      double bestScore = Math.round(scores[best][0] * 100000) / 100000.0;
      System.out.println(
          "  " + learner.name + ": " + messageIndent + "Best "
          + metric.getName() + ": " + bestScore);
      System.out.print(
          "  " + learner.name + ":   " + messageIndent + "with ");

      if (parameterStrings[best].length() > 0) {
        System.out.println(parameterStrings[best]);
        System.out.print(
            "  " + learner.name + ":   " + messageIndent + "and ");
      }

      System.out.println(parameters[best].rounds + " rounds");
    }

    learner.setParameters(parameters[best]);
    return parameters[best];
  }


  /** <!-- tune(Learner.Parameters[],int[],Parser,TestingMetric) -->
    * Tune learning algorithm parameters against a development set.  Note that
    * this interface takes both an array of
    * {@link LBJ2.learn.Learner.Parameters} objects and an array of rounds.
    * As such, the value in the {@link LBJ2.learn.Learner.Parameters#rounds}
    * field is ignored during tuning.  It is also overwritten in each of the
    * {@link LBJ2.learn.Learner.Parameters} objects when the optimal number of
    * rounds is determined in terms of the other parameters in each object.
    * Finally, in addition to returning the parameters that got the best
    * performance, this method also sets {@link #learner} with those
    * parameters at the end of the method.
    *
    * <p> This method assumes that {@link #learner} has not yet done any
    * learning.  The learner will again be empty in this sense when the method
    * exits, except that any label lexicon present before the method was
    * called will be restored.  The label lexicon needs to persist in this way
    * so that it can ultimately be written into the model file.
    *
    * @param parameters An array of parameter settings objects.
    * @param rounds     An array of training rounds after which performance of
    *                   the learner should be evaluated on the testing data.
    * @param devParser  A parser from which development set examples are
    *                   obtained.
    * @param metric     A metric with which to evaluate the learner.
    * @return The element of <code>parameters</code> that resulted in the best
    *         performance according to <code>metric</code>.
   **/
  public Learner.Parameters tune(Learner.Parameters[] parameters,
                                 final int[] rounds,
                                 final Parser devParser,
                                 final TestingMetric metric) {
    int best = -1;
    double[] scores = new double[parameters.length];
    String[] parameterStrings = new String[parameters.length];
    Arrays.sort(rounds);
    final int totalRounds = rounds[rounds.length - 1];
    Lexicon labelLexicon = learner.getLabelLexicon();

    for (int i = 0; i < parameters.length; ++i) {
      parameterStrings[i] = parameters[i].nonDefaultString();
      // Status message.
      if (progressOutput > 0)
        System.out.println(
            "  " + learner.name + ": " + messageIndent + "Trying parameters ("
            + parameterStrings[i] + ")");

      final double[] results = new double[rounds.length];
      learner.setParameters(parameters[i]);
      messageIndent += "  ";

      train(totalRounds,
            new DoneWithRound() {
              int r = 0;
              public void doneWithRound(int round) {
                if (round < totalRounds && rounds[r] == round)
                  results[r++] = testMidTraining(devParser, metric, true);
              }
            });

      results[rounds.length - 1] = testMidTraining(devParser, metric, false);
      messageIndent = messageIndent.substring(2);

      // Update best scores, rounds, and parameters.
      int bestRounds = 0;
      if (best == -1 || results[0] > scores[best]) best = i;
      scores[i] = results[0];

      for (int j = 1; j < results.length; ++j)
        if (results[j] > scores[i]) {
          bestRounds = j;
          scores[i] = results[j];
          if (results[j] > scores[best]) best = i;
        }

      parameters[i].rounds = rounds[bestRounds];

      learner.forget();
      if (labelLexicon != null && labelLexicon.size() > 0
          && learner.getLabelLexicon().size() == 0)
        learner.setLabelLexicon(labelLexicon);
    }

    if (progressOutput > 0) {
      // Print a table of results.
      double[][] data = new double[parameters.length][3];
      for (int i = 0; i < parameters.length; ++i) {
        data[i][0] = i + 1;
        data[i][1] = scores[i];
        data[i][2] = parameters[i].rounds;
      }

      String[] columnLabels = { "Set", metric.getName(), "Rounds" };
      int[] sigDigits = { 0, 3, 0 };
      String[] s =
        TableFormat.tableFormat(columnLabels, null, data, sigDigits,
                                new int[]{ 0 });

      System.out.println("  " + learner.name + ": " + messageIndent + "----");
      System.out.println(
          "  " + learner.name + ": " + messageIndent + "Parameter sets:");
      for (int i = 0; i < parameterStrings.length; ++i)
        System.out.println(
            "  " + learner.name + ": " + messageIndent + (i+1) + ": "
            + parameterStrings[i]);
      for (int i = 0; i < s.length; ++i)
        System.out.println("  " + learner.name + ": " + messageIndent + s[i]);
      System.out.println("  " + learner.name + ": " + messageIndent + "----");

      // Status message.
      double bestScore = Math.round(scores[best] * 100000) / 100000.0;
      System.out.println(
          "  " + learner.name + ": " + messageIndent + "Best "
          + metric.getName() + ": " + bestScore);
      System.out.print(
          "  " + learner.name + ":   " + messageIndent + "with ");

      if (parameterStrings[best].length() > 0) {
        System.out.println(parameterStrings[best]);
        System.out.print(
            "  " + learner.name + ":   " + messageIndent + "and ");
      }

      System.out.println(parameters[best].rounds + " rounds");
    }

    learner.setParameters(parameters[best]);
    return parameters[best];
  }


  /** <!-- testMidTraining(Parser,TestingMetric,boolean) -->
    * Tests {@link #learner} on the specified data while making provisions
    * under the assumption that this test happens in between rounds of
    * training.
    *
    * @param testParser A parser producing labeled testing examples.
    * @param metric     The metric used to evaluate the performance of the
    *                   learner.
    * @param clone      Whether or not the learner should be cloned (and it
    *                   should be cloned if more learning will take place
    *                   after making this call).
    * @return The result produced by the testing metric on the testing data
    *         expressed as a percentage (instead of a fraction) if the testing
    *         metric is {@link Accuracy}.
   **/
  protected double testMidTraining(Parser testParser,
                                   TestingMetric metric,
                                   boolean clone) {
    Learner testLearner = clone ? (Learner) learner.clone() : learner;
    testLearner.doneLearning();
    double result = 0;

    if (testParser instanceof ArrayFileParser) {
      ArrayFileParser afp = (ArrayFileParser) testParser;
      afp.setIncludePruned(true);
      result = metric.test(testLearner, null, testParser);
      afp.setIncludePruned(false);
    }
    else {
      setIsTraining(false);
      result = metric.test(testLearner, testLearner.getLabeler(), testParser);
      setIsTraining(true);
    }

    testParser.reset();
    if (metric instanceof Accuracy) result *= 100;

    if (progressOutput > 0) {
      double printResult = Math.round(result * 100000) / 100000.0;
      System.out.print(
          "  " + learner.name + ": " + messageIndent + metric.getName() + ": "
          + printResult);
      if (metric instanceof Accuracy) System.out.print("%");
      System.out.println();
    }

    return result;
  }
}

