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
public class FeatureTest extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());

  /**
   * Standard test class constructior for FeatureTest
   * @param name Name of the test
   */
  public FeatureTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for FeatureTest
   */
  public FeatureTest()
  {
    super("FeatureTest");
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

  public void testSimpleFactoryConstruct()
  {
      FeatureFactory factory = new FeatureFactory();
      Feature f = factory.getFeature("token eq hello");
      assertNotNull(f);
      assertTrue(factory.contains(f));
  }

  /**
   * Base test for FeatureTest
   */
  public void testFeatureTest()
  {
      FeatureFactory factory = new FeatureFactory();
      Feature f = factory.getFeature(new String[] {"token","eq", "hello"});
      assertNotNull(f);
      assertTrue(factory.contains(f));
  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(FeatureTest.class);
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
