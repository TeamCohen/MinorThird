package edu.cmu.minorthird.text;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.io.File;
import java.io.IOException;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class TextLabelsLoaderTest extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());
  private String dataFile = "demos/sampleData/webmasterCommands.txt";
  private String labelsFile = "demos/sampleData/webmasterCommandTypes.labels";

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
      TextBase base = new BasicTextBase();
      TextBaseLoader baseLoader = new TextBaseLoader();
      File file = new File(dataFile);
      baseLoader.setFirstWordIsDocumentId(true);
      baseLoader.loadLines(base, file);

      TextLabelsLoader loader = new TextLabelsLoader();
      File labelFile = new File(this.labelsFile);
      MutableTextLabels labels = new BasicTextLabels();
      labels.setTextBase(base);
      loader.importOps(labels, base, labelFile);
    }
    catch (IOException e)
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