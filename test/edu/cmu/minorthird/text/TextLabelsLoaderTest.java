package edu.cmu.minorthird.text;

import edu.cmu.minorthird.util.Globals;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class TextLabelsLoaderTest extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());
  private String dataFile = Globals.DATA_DIR + "webmasterCommands.base";
  private String labelsFile = Globals.DATA_DIR + "webmasterCommands.labels";
  private MutableTextLabels labels;

  /**
   * Standard test class constructior for TextLabelsLoaderTest
   * @param name Name of the test
   */
  public TextLabelsLoaderTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for TextLabelsLoaderTest
   */
  public TextLabelsLoaderTest()
  {
    super("TextLabelsLoaderTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    Logger.getRootLogger().removeAllAppenders();
    org.apache.log4j.BasicConfigurator.configure();
    //TODO add initializations if needed
    Logger.getRootLogger().setLevel(Level.DEBUG);
  }

  /**
   * clean up to run after each test
   */
  protected void tearDown()
  {
    //TODO clean up resources if needed
  }


  public void testClosureOutput()
  {
    try
    {
      labelsFile = Globals.DATA_DIR + "webmasterCommands.closeDocs.labels";
      testImportOps(); //loads up the labels object

      File outFile = new File(Globals.DATA_DIR + "webmaster.closeDocs.testOut");
      TextLabelsLoader saver = new TextLabelsLoader();
      saver.setClosurePolicy(TextLabelsLoader.CLOSE_TYPES_IN_LABELED_DOCS);
      saver.saveTypesAsOps(labels, outFile);
      BufferedReader in = new BufferedReader(new FileReader(outFile));
      String line = "";
      while (in.ready())
      { line = in.readLine(); }
      assertEquals("setClosure CLOSE_TYPES_IN_LABELED_DOCS", line);
    }
    catch (Exception e)
    {
      log.error(e, e);
      fail();
    }
  }

  public void testClosurePolicies()
  {
    testImportOps(); //loads up the labels object
    Span.Looper it = labels.closureIterator("addToDatabaseCommand");
    assertEquals(40, it.estimatedSize());

    labelsFile = Globals.DATA_DIR + "webmasterCommands.closeAll.labels";
    testImportOps(); //loads up the labels object
    it = labels.closureIterator("addToDatabaseCommand");
    assertEquals(40, it.estimatedSize());

    labelsFile = Globals.DATA_DIR + "webmasterCommands.closeDocs.labels";
    testImportOps(); //loads up the labels object
    it = labels.closureIterator("addToDatabaseCommand");
    assertEquals(19, it.estimatedSize());

    labelsFile = Globals.DATA_DIR + "webmasterCommands.closeNone.labels";
    testImportOps(); //loads up the labels object
    it = labels.closureIterator("addToDatabaseCommand");
    assertEquals(0, it.estimatedSize());
  }

  /**
   * Base test for TextLabelsLoaderTest
   */
  public void testImportOps()
  {
    try
    {
      TextBase base = TextBaseLoader.loadDocPerLine(new File(dataFile), false);

      TextLabelsLoader loader = new TextLabelsLoader();
      File labelFile = new File(this.labelsFile);
      labels = new BasicTextLabels();
      labels.setTextBase(base);
      loader.importOps(labels, base, labelFile);

    }
    catch (Exception e)
    {
      log.error(e, e);
      fail();
    }
  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(TextLabelsLoaderTest.class);
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