package edu.cmu.minorthird.text;

import edu.cmu.minorthird.util.Globals;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;

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
      MutableTextLabels labels = new BasicTextLabels();
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