package edu.cmu.minorthird.classify;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class BasicFeatureIndexTest extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());

  /**
   * Standard test class constructior for BasicFeatureIndexTest
   * @param name Name of the test
   */
  public BasicFeatureIndexTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for BasicFeatureIndexTest
   */
  public BasicFeatureIndexTest()
  {
    super("BasicFeatureIndexTest");
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
   * Base test for BasicFeatureIndexTest
   */
  public void testBasicFeatureIndexTest()
  {
    log.debug(new BasicFeatureIndex(SampleDatasets.sampleData("bayes",false)));

  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(BasicFeatureIndexTest.class);
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