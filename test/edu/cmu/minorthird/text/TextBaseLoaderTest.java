package edu.cmu.minorthird.text;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.Globals;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Iterator;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class TextBaseLoaderTest extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());

  /**
   * Standard test class constructior for TextBaseLoaderTest
   * @param name Name of the test
   */
  public TextBaseLoaderTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for TextBaseLoaderTest
   */
  public TextBaseLoaderTest()
  {
    super("TextBaseLoaderTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    Logger.getRootLogger().removeAllAppenders();
    org.apache.log4j.BasicConfigurator.configure();
    //TODO add initializations if needed
  }

  /**
   * clean up to run after each test
   */
  protected void tearDown()
  {
    //TODO clean up resources if needed
  }

  /**
   * read the seminar-subset data into a labels
   * then takes the smaller version, and checks that labels are as expected
   */
  public void testSeminarSet()
  {
    try
    {
      log.info("----------------- SeminarSet -----------------");
      TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE);
      loader.setLabelsInFile(true);
      File dataLocation = new File(Globals.DATA_DIR + "seminar-subset");
      TextBase textBase = loader.load(dataLocation);
      TextLabels labels = TextBaseLoader.loadDirOfTaggedFiles(dataLocation);

      dataLocation = new File(Globals.DATA_DIR + "tblTest");
      labels = TextBaseLoader.loadDirOfTaggedFiles(dataLocation);
      log.info("labels: " + labels.toString());

      log.debug("types::: " + labels.getTypes());

      //now check that it's right
      textBase = labels.getTextBase();
      assertEquals(4, textBase.size());
      assertNotNull(textBase.documentSpan("cil-2.txt"));

      checkSeminarSample(labels);
    }
    catch (Exception e)
    {
      log.fatal(e, e);
      fail();
    }
    log.info("----------------- SeminarSet -----------------");
  }

  /** explicit labeling check of 4 docs from seminar set */
  protected void checkSeminarSample(TextLabels labels)
  {
    checkType(labels, "stime", "cil-2.txt", "4:00", 1);
    checkType(labels, "stime", "cil-5.txt", "12:00pm", 1);
    checkType(labels, "stime", "cil-22.txt", "4:30 pm", 1);
    checkType(labels, "stime", "cil-28.txt", "12:00pm", 1);
    checkType(labels, "location", "cil-2.txt", "Adamson Wing, Baker Hall", 1);
    checkType(labels, "location", "cil-5.txt", "3005 Hamburg Hall", 1);
    checkType(labels, "location", "cil-22.txt", "Wean 7500", 1);
    checkType(labels, "location", "cil-28.txt", "Student Center, Room 207", 1);
    checkType(labels, "speaker", "cil-2.txt", "George W. Cobb", 1);
    checkType(labels, "speaker", "cil-5.txt", "Karen Schriver", 1);
    checkType(labels, "speaker", "cil-22.txt", "Bruce Sherwood", 1);
    checkType(labels, "speaker", "cil-28.txt", "David Banks", 1);

    assertEquals(2, getNumLables(labels, "sentence", "cil-2.txt"));
    assertEquals(2, getNumLables(labels, "sentence", "cil-5.txt"));
    assertEquals(3, getNumLables(labels, "sentence", "cil-22.txt"));
    assertEquals(4, getNumLables(labels, "sentence", "cil-28.txt"));
  }

  /**
   * returns the number of times the given type appears in the doc
   */
  protected int getNumLables(TextLabels labels, String type, String doc)
  {
    int i = 0;
    for (Span.Looper l = labels.instanceIterator(type, doc); l.hasNext(); )
    {
      log.debug(l.nextSpan().asString());
      i++;
    }

    return i;
  }

  /**
   * asserts that the type has the specified value and that it appears (with that value!) the
   * specified number of times
   */
  protected void checkType(TextLabels labels, String type, String doc, String value, int num)
  {
    int i = 0;
    for (Span.Looper l = labels.instanceIterator(type, doc); l.hasNext(); i++)
    {
      Span s = l.nextSpan();
      log.debug("span type: " + type + " : " + s.asString());
      assertEquals(new String(value), s.asString());
    }
    assertEquals(num, i);
  }

  /**
   * loads xmlLines.base and checks that the labels are as expected
   */
  public void testLoadLabeledLines()
  {
    try
    {
      TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_LINE, TextBaseLoader.IN_FILE, true);
      TextBase textBase = loader.load(new File(Globals.DATA_DIR + "xmlLines.base"));
      TextLabels labels = loader.getLabels();

      assertEquals(7, textBase.size());
      assertNotNull(textBase.documentSpan("doc1"));

      checkType(labels, "stime", "doc1", "4:00", 1);
      checkType(labels, "location", "doc1", "Adamson Wing, Baker Hall", 1);
      checkType(labels, "speaker", "doc2", "George W. Cobb", 1);
      checkType(labels, "title", "doc3", "Title: Three Ways to Gum up a Statistics Course", 1);
      checkType(labels, "sentence", "doc4", "My talk will be in two parts", 1);
      checkType(labels, "comment", "doc5", "comments and observations", 1);
      checkType(labels, "country", "doc6", "US", 1);

      Iterator it = labels.getTypes().iterator();
      while (it.hasNext())
      {
        assertEquals(0, this.getNumLables(labels, it.next().toString(), "doc7"));
      }

    }
    catch (Exception e)
    {
      log.fatal(e, e);
      fail();
    }

  }

  /**
   * loads webmaster-noid.base and checks that msgs 1-3 have expected text
   */
  public void testLinesNoId()
  {
    try
    {
      TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_LINE, TextBaseLoader.NONE);
      File dataLocation = new File(Globals.DATA_DIR + "webmaster-noid.base");
      TextBase base = loader.load(dataLocation);

      Span.Looper l = base.documentSpanIterator();
      while (l.hasNext())
      {
        log.info("*" + l.nextSpan().getDocumentId() + "*");
      }

      String msg1 = "Please add the attached publication to the web site in the ``Publications\" folder. The authors are Anthony Tomasic, Louiqa Raschid and Patrick Valduriez. The title is ``Scaling Access to Heterogeneous Databases with DISCO\" and it appeared in the IEEE Transactions on Knowledge and Data Engineering, 1998.";
      String msg2 = "Please add the folder ``Publications\" to the web site.";
      String msg3 = "Please change the string ``VLDB\" to ``International Conference on Very Large Databases\" on the ``Publications\" page.";

      assertEquals(msg1, base.documentSpan("webmaster-noid.base@line:1").asString());
      assertEquals(msg2, base.documentSpan("webmaster-noid.base@line:2").asString());
      assertEquals(msg3, base.documentSpan("webmaster-noid.base@line:3").asString());
    }
    catch (Exception e)
    {
      log.fatal(e, e);
      fail();
    }
  }

  /**
   * loads webmasterCommands.base and checks that msgs 1-3 have expected text
   */
  public void testLines()
  {
    try
    {
      TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_LINE, TextBaseLoader.IN_FILE);
      File dataLocation = new File(Globals.DATA_DIR + "webmasterCommands.base");
      TextBase base = loader.load(dataLocation);

      base = TextBaseLoader.loadDocPerLine(dataLocation, false);

      checkWebMasterLines(base);
    }
    catch (Exception e)
    {
      log.fatal(e, e);
      fail();
    }
  }

  /** checks string representation of msges 1-3 of seminar set */
  protected void checkWebMasterLines(TextBase base)
  {
    String msg1 = "Please add the attached publication to the web site in the ``Publications\" folder. The authors are Anthony Tomasic, Louiqa Raschid and Patrick Valduriez. The title is ``Scaling Access to Heterogeneous Databases with DISCO\" and it appeared in the IEEE Transactions on Knowledge and Data Engineering, 1998.";
    String msg2 = "Please add the folder ``Publications\" to the web site.";
    String msg3 = "Please change the string ``VLDB\" to ``International Conference on Very Large Databases\" on the ``Publications\" page.";

    assertEquals(msg1, base.documentSpan("msg01").asString());
    assertEquals(msg2, base.documentSpan("msg02").asString());
    assertEquals(msg3, base.documentSpan("msg03").asString());
  }

  /**
   * Load from blankLines.base
   * checks that the extra lines are thrown out
   */
  public void testBlankLines()
  {
    try
    {
      TextBase base = TextBaseLoader.loadDocPerLine(new File(Globals.DATA_DIR + "blankLines.base"), false);
      //check that I can get a token from every document
      Span.Looper it = base.documentSpanIterator();
      while (it.hasNext())
      {
        assertNotNull(it.nextSpan().getTextToken(0));
      }
      //check that # of documents is 1
      assertEquals(1, base.size());
    }
    catch (Exception e)
    {
      log.error(e, e);
      fail();
    }

  }

  /**
   * Base test for TextBaseLoaderTest
   */
  public void testDirectories()
  {
    try
    {
      TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE, TextBaseLoader.FILE_NAME,
                                                  TextBaseLoader.NONE, TextBaseLoader.DIRECTORY_NAME, false, true);

      log.info("loader labels: " + loader.isLabelsInFile());
      File dir = new File(Globals.DATA_DIR + "20newgroups/20news-bydate-train");
      TextBase base = loader.load(dir);
  //    loader.loadLabeledDir(base, dir);
      log.debug("loaded training set");

      // set up the labels
      MutableTextLabels labels = loader.getLabels();


      log.debug("passed first assertion");
      log.debug("base size = " + base.size());
      // for verification/correction of the labels, if we care...
      // TextBaseLabeler.label( labels, new File("my-document-labels.env"));

      // set up a simple bag-of-words feature extractor
      edu.cmu.minorthird.text.learn.SpanFeatureExtractor fe = edu.cmu.minorthird.text.learn.SampleFE.BAG_OF_LC_WORDS; //SimpleFeatureExtractor();

      // create a binary dataset for the class 'delete'
      Dataset data = extractDataset(base, labels, fe);

      log.debug("extracted dataset");
      log.debug("data size = " + data.size());

      Example.Looper it = data.iterator();
      log.debug("got looper: " + it);

      }
    catch (Exception e)
    {
      log.error(e, e);  //To change body of catch statement use Options | File Templates.
      fail();
    }

  }

  private Dataset extractDataset(TextBase base, MutableTextLabels labels, edu.cmu.minorthird.text.learn.SpanFeatureExtractor fe)
  {
    Dataset data = new BasicDataset();
    int numSpans = 0;
    for (Span.Looper i = base.documentSpanIterator(); i.hasNext();)
    {
      log.debug("span: " + numSpans++);
      Span s = i.nextSpan();
      double label = labels.hasType(s, "delete") ? +1 : -1;
      data.add(new BinaryExample(fe.extractInstance(s), label));
    }
    return data;
  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(TextBaseLoaderTest.class);
  }

  /**
   * Run the full suite of tests with text output
   * @param args - unused
   */
  public static void main(String args[])
  {
    junit.textui.TestRunner.run(suite());
  }
}