package edu.cmu.minorthird.classify;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class CompactInstanceTest extends InstanceTestBase
{
  Logger log = Logger.getLogger(this.getClass());
  private MutableInstanceTest mutTest;

  /**
   * Standard test class constructior for CompactInstanceTest
   * @param name Name of the test
   */
  public CompactInstanceTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for CompactInstanceTest
   */
  public CompactInstanceTest()
  {
    super("CompactInstanceTest");
  }

  public void testBinaryFeatures()
  {
    mutTest.testBinaryFeatures();
    setInstance();
  }

  private void setInstance()
  {
    MutableInstance mutInstance = mutTest.getMutInstance();
    instance = new CompactInstance(mutInstance);
  }

  public void testNumericFeatures()
  {
    mutTest.testNumericFeatures();
    setInstance();
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    Logger.getRootLogger().removeAllAppenders();
    org.apache.log4j.BasicConfigurator.configure();
    //TODO add initializations if needed
    mutTest = new MutableInstanceTest();
    mutTest.setUp();
  }

  /**
   * clean up to run after each test
   */
  protected void tearDown()
  {
    //TODO clean up resources if needed
  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(CompactInstanceTest.class);
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