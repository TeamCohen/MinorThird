package edu.cmu.minorthird.text.mixup;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class DependenciesTest extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());

  /**
   * Standard test class constructior for DependenciesTest
   * @param name Name of the test
   */
  public DependenciesTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for DependenciesTest
   */
  public DependenciesTest()
  {
    super("DependenciesTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    Logger.getRootLogger().removeAllAppenders();
    org.apache.log4j.BasicConfigurator.configure();
    log.setLevel(Level.DEBUG);
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
   * Base test for DependenciesTest
   */
  public void testAGet()
  {
    try
    {
      log.debug("first call");
      Dependencies.getDependency("names");

      assertEquals("names.mixup", Dependencies.getDependency("names"));
      assertEquals("date.mixup", Dependencies.getDependency("date"));
      assertEquals("time.mixup", Dependencies.getDependency("time"));
      assertEquals("np.mixup", Dependencies.getDependency("npchunks"));

    }
    catch (Exception e)
    {
      log.error(e, e);
      fail();
    }
  }

  public void testBroken()
  {
    try
    {
      Dependencies.runDependency(null, "no such annotation", null);
      fail("should throw exception");
    }
    catch (Exception e)
    {
      assertTrue(e.getMessage().indexOf("error loading") > -1);
      log.error(e.getMessage());
    }
  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(DependenciesTest.class);
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
