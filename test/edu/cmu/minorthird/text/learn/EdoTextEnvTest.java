package edu.cmu.minorthird.text.learn;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;

import edu.cmu.minorthird.text.learn.ClassifyTest;
import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.TextBaseLoader;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class EdoTextEnvTest extends ClassifyTest
{

  /**
   * Standard test class constructior for EdoTextEnvTest
   * @param name Name of the test
   */
  public EdoTextEnvTest(String name, String document)
  {
    super(name);
    this.documentId = document;
  }

  /**
   * Convinence constructior for EdoTextEnvTest
   */
  public EdoTextEnvTest()
  {
    super("EdoTextEnvTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
    org.apache.log4j.BasicConfigurator.configure();

  }

  public void spanTest()
  {
    try
    {
      dataFile = "examples/testData.dir";
      envFile = "examples/testData.env";
      labelString = "rr";
//    documentId = "ph1.text";

      base = new BasicTextBase();
      TextBaseLoader loader = new TextBaseLoader();
      File dir = new File(dataFile);
      loader.loadTaggedFiles(base, dir);

// check
      super.checkSpans();

    }
    catch (Exception e)
    {
      log.fatal(e, e);
      fail();
    }
  }

  /**
   * clean up to run after each test
   */
  protected void tearDown()
  {
    //TODO clean up resources if needed
  }

  /**
   * Creates a TestSuite of spanTests
   * @return TestSuite
   */
  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTest(new EdoTextEnvTest("spanTest", "ph1.text"));
    suite.addTest(new EdoTextEnvTest("spanTest", "rr1.text"));
    return suite;
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