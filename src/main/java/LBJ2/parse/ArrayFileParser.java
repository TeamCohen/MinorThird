package LBJ2.parse;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import LBJ2.classify.FeatureVector;
import LBJ2.learn.Learner;
import LBJ2.parse.FoldSeparator;
import LBJ2.util.ExceptionlessInputStream;


/**
  * This parser returns an array of arrays representing each example.  The
  * first array represents the integer keys of the example's features; the
  * second array holds the values of those features. The third array holds the
  * example's label(s), and the fourth array holds the values of those labels.
  * These arrays are read in through files, and the paths to these files are
  * passed in through the constructor.
  *
  * <p> When run as a stand-alone program, this class takes the names of
  * example, lexicon, and model files as input and prints all the feature
  * vectors in the dataset to <code>STDOUT</code>.
  *
  * @author Michael Paul
 **/
public class ArrayFileParser implements Parser
{
  /** Reader for file currently being parsed. */
  protected DataInputStream in;
  /** The name of the file to parse. */
  protected String exampleFileName;
  /** A single array from which all examples can be parsed. */
  protected byte[] exampleData;
  /** Whether or not the input stream is zipped. */
  protected boolean zipped;
  /** Whether the returned example arrays should include pruned features. */
  protected boolean includePruned = false;


  /**
    * Initializes the parser with a file name assuming the input stream is not
    * zipped.
    *
    * @param exampleFile  The name of the file containing the examples.
   **/
  public ArrayFileParser(String exampleFile) { this(exampleFile, true); }

  /**
    * Initializes the parser with a file name, specifying whether the data is
    * zipped.
    *
    * @param exampleFile  The name of the file containing the examples.
    * @param zip          Whether or not the input stream is zipped.
   **/
  public ArrayFileParser(String exampleFile, boolean zip) {
    exampleFileName = exampleFile;
    zipped = zip;
    reset();
  }

  /**
    * Initializes the parser with a data array assuming the input stream is
    * not zipped.
    *
    * @param data The examples can be parsed out of this array.
   **/
  public ArrayFileParser(byte[] data) { this(data, true); }

  /**
    * Initializes the parser with a data array, specifying whether the data is
    * zipped.
    *
    * @param data The examples can be parsed out of this array.
    * @param zip  Whether or not the input stream is zipped.
   **/
  public ArrayFileParser(byte[] data, boolean zip) {
    exampleData = data;
    zipped = zip;
    reset();
  }


  /** Setter for {@link #includePruned}. */
  public void setIncludePruned(boolean b) { includePruned = b; }


  /**
    * Returns the number of examples left in the example file.  This may be
    * slow to compute as it must read through the entire file and increment
    * the count.  {@link #reset()} is called after the examples are counted.
    *
    * @return The number of examples left in the example file.
   **/
  public int getNumExamples() {
    int result = 0;

    try {
      while (true) {
        int L = in.readInt();
        if (L == -1) continue;
        ++result;
        in.skipBytes(12 * L); // 4 for label index, 8 for its value
        L = in.readInt() + in.readInt();
        in.skipBytes(12 * L); // 4 for feature index, 8 for its value
      }
    }
    catch (EOFException eof) {
    }
    catch (Exception e) {
      System.err.println("Can't read from '" + exampleFileName + "':");
      e.printStackTrace();
      System.exit(1);
    }

    reset();

    return result;
  }


  /**
    * Returns either an <code>Object[]</code> or a {@link FoldSeparator}
    * deserialized out of the given file.
   **/
  public Object next() {
    Object[] result = new Object[4];

    try {
      int L = in.readInt();

      // A -1 means that there was a fold separator here
      if (L == -1) return FoldSeparator.separator;
      else {
        int[] exampleLabels = new int[L];
        double[] labelValues = new double[L];
        for (int i = 0; i < L; ++i) {
          exampleLabels[i] = in.readInt();
          labelValues[i] = in.readDouble();
        }

        int Fup = in.readInt(); // # unpruned
        int Fp = in.readInt();  // # pruned
        int F = (includePruned) ? (Fup+Fp) : Fup;

        int[] exampleFeatures = new int[F];
        double[] exampleValues = new double[F];

        for (int i = 0; i < Fup+Fp; ++i) {
          int ef = in.readInt();
          double ev = in.readDouble();

          if (i < F) {
            exampleFeatures[i] = ef;
            exampleValues[i] = ev;
          }
        }

        result[0] = exampleFeatures;
        result[1] = exampleValues;
        result[2] = exampleLabels;
        result[3] = labelValues;
      }
    }
    catch (EOFException eof) {
      result = null;
    }
    catch (Exception e) {
      System.err.println("Can't read from '" + exampleFileName + "':");
      e.printStackTrace();
      System.exit(1);
    }

    return result;
  }


  /** Resets the example file stream to the beginning. */
  public void reset() {
    close();

    try {
      if (exampleFileName != null) {
        if (zipped) {
          ZipFile zip = new ZipFile(exampleFileName);
          in =
            new DataInputStream(
              new BufferedInputStream(
                  zip.getInputStream(
                      zip.getEntry(ExceptionlessInputStream.zipEntryName))));
        }
        else
          in =
            new DataInputStream(
                new BufferedInputStream(
                    new FileInputStream(exampleFileName)));
      }
      else if (zipped) {
        ZipInputStream zip =
          new ZipInputStream(
              new ByteArrayInputStream(exampleData));
        zip.getNextEntry();
        in = new DataInputStream(new BufferedInputStream(zip));
      }
      else
        in =
          new DataInputStream(
              new ByteArrayInputStream(exampleData));
    }
    catch (Exception e) {
      System.err.println("Can't open '" + exampleFileName + "' for input:");
      e.printStackTrace();
      System.exit(1);
    }
  }


  /** Frees any resources this parser may be holding. */
  public void close() {
    if (in == null) return;
    try { in.close(); }
    catch (Exception e) {
      System.err.println("Can't close '" + exampleFileName + "':");
      e.printStackTrace();
      System.exit(1);
    }
  }


  public static void main(String[] args) {
    String exFileName = null;
    String lexFileName = null;
    String lcFileName = null;

    try {
      exFileName = args[0];
      lexFileName = args[1];
      lcFileName = args[2];
      if (args.length > 3) throw new Exception();
    }
    catch (Exception e) {
      System.err.println(
"usage: java LBJ2.parse.ArrayFileParser <example file> <lexicon file> <lc file>");
      System.exit(1);
    }

    ArrayFileParser parser = new ArrayFileParser(exFileName);
    Learner learner = Learner.readLearner(lcFileName);
    learner.readLexicon(lexFileName);

    for (Object e = parser.next(); e != null; e = parser.next()) {
      FeatureVector v =
        new FeatureVector((Object[]) e, learner.getLexicon(),
                          learner.getLabelLexicon());
      v.sort();
      System.out.println(v);
    }
  }
}

