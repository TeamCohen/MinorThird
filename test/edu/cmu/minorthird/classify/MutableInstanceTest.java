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
public class MutableInstanceTest extends InstanceTestBase
{
  Logger log = Logger.getLogger(this.getClass());
  private MutableInstance mutInstance;

  /**
   * Standard test class constructior for MutableInstanceTest
   * @param name Name of the test
   */
  public MutableInstanceTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for MutableInstanceTest
   */
  public MutableInstanceTest()
  {
    super("MutableInstanceTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    Logger.getRootLogger().removeAllAppenders();
    org.apache.log4j.BasicConfigurator.configure();
    //TODO add initializations if needed
    mutInstance = new MutableInstance("source for test");
    instance = mutInstance;
  }

  /**
   * clean up to run after each test
   */
  protected void tearDown()
  {
    //TODO clean up resources if needed
  }

  public void testConstructor()
  {
    instance = new MutableInstance("test source", "sub pop");
    assertEquals("test source", instance.getSource());
    assertEquals("sub pop", instance.getSubpopulationId());
  }

  public void testBinaryFeatures()
  {
    mutInstance.addBinary(hello);
    mutInstance.addBinary(world);
    mutInstance.addBinary(new Feature("token eq hello"));
    mutInstance.addBinary(new Feature("token eq world"));
    mutInstance.addBinary(new Feature("token eq purple"));
    mutInstance.addBinary(new Feature("token eq croutons"));
    mutInstance.addBinary(new Feature("token eq zzfencepost"));

    super.testBinaryFeatures();
  }

  public void testNumericFeatures()
  {
    mutInstance.addNumeric(hello, 1);
    mutInstance.addNumeric(world, 5);
    mutInstance.addNumeric(new Feature("token eq hello"), 1);
    mutInstance.addNumeric(new Feature("token eq world"), 10);
    mutInstance.addNumeric(new Feature("token eq purple"), 0.5);
    mutInstance.addNumeric(new Feature("token eq max"), Double.MAX_VALUE);
    mutInstance.addNumeric(new Feature("token eq nan"), Double.NaN);

    super.testNumericFeatures();

  }

  public void testInstanceComparison()
  {
    super.testMixedFeatures();
//    MutableInstance old = mutInstance;

    setUp();
    addNewFeatures();
//    super.testBinaryFeatures();
//    super.testNumericFeatures();

    //check that the new and old are equivalent
  }

  private void addNewFeatures()
  {
  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(MutableInstanceTest.class);
  }

  /**
   * Run the full suite of tests with text output
   * @param args - unused
   */
  public static void main(String args[])
  {
    junit.textui.TestRunner.run(suite());
  }

  public MutableInstance getMutInstance()
  {
    return mutInstance;
  }


}