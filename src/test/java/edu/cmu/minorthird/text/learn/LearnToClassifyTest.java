package edu.cmu.minorthird.text.learn;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.io.File;

import edu.cmu.minorthird.text.TextBaseLoader;
import edu.cmu.minorthird.util.Globals;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class LearnToClassifyTest extends ClassifyTest
{
  Logger log = Logger.getLogger(this.getClass());

  /**
   * Standard test class constructior for LearnToClassifyTest
   * @param name Name of the test
   */
  public LearnToClassifyTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for LearnToClassifyTest
   */
  public LearnToClassifyTest()
  {
    super("LearnToClassifyTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
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
   * Base test for LearnToClassifyTest
   */
  public void testSpans()
  {
    try
    {
      dataFile = Globals.DATA_DIR + "webmasterCommands.base";
      labelsFile = Globals.DATA_DIR + "webmasterCommands.labels";
      documentId = "msg05";
      labelString = "add";
      loadFileData();
      super.checkSpans();
    }
    catch (Exception e)
    {
      log.fatal(e, e);
      fail();
    }

  }

  private void loadFileData() throws Exception
  {
    File file = new File(dataFile);

//    base = new BasicTextBase();
//    TextBaseLoader loader = new TextBaseLoader();
//    loader.setFirstWordIsDocumentId(true);
//    loader.loadLines(base, file);

    TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_LINE);
    base = loader.load(file);

  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(LearnToClassifyTest.class);
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