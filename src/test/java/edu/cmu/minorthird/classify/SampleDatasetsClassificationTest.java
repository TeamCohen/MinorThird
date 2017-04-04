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
public class SampleDatasetsClassificationTest extends AbstractClassificationChecks
{
  Logger log = Logger.getLogger(this.getClass());

  /**
   * Standard test class constructior for SampleDatasetsClassificationTest
   * @param name Name of the test
   */
  public SampleDatasetsClassificationTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for SampleDatasetsClassificationTest
   */
  public SampleDatasetsClassificationTest()
  {
    super("SampleDatasetsClassificationTest");
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
   * Base test for SampleDatasetsClassificationTest
   */
  public void test()
  {
    Dataset train = SampleDatasets.toyBayesTrain();
    Dataset test = SampleDatasets.toyBayesTest();

    log.debug("train: \n" + train);
    log.debug("test: \n" + test);
    double[] expectedStats = new double[] {0d, 0d, 0d, 0d, 0d, 0d, 0d,
                                           1d, 1d,
                                           10.117528032481275,
                                           1d, 1d, 1d, 1d};

    super.checkClassify(DEFAULT_LEARNER, train, test, expectedStats);
  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(SampleDatasetsClassificationTest.class);
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
