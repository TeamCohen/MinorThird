package edu.cmu.minorthird.text.util;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import org.apache.log4j.Logger;
import edu.cmu.minorthird.text.TextBaseLoaderTest;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.util.Globals;

import java.io.File;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class SimpleTextLoaderTest extends TextBaseLoaderTest
{
  Logger log = Logger.getLogger(this.getClass());

  /**
   * Standard test class constructior for SimpleTextLoaderTest
   * @param name Name of the test
   */
  public SimpleTextLoaderTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for SimpleTextLoaderTest
   */
  public SimpleTextLoaderTest()
  {
    super("SimpleTextLoaderTest");
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
   * Base test for SimpleTextLoaderTest
   */
  public void testDir()
  {
    //load seminar stuff
    File dataLocation = new File(Globals.DATA_DIR + "tblTest");
    TextLabels labels = SimpleTextLoader.load(dataLocation, false);
    super.checkSeminarSample(labels);
  }

  public void testLines()
  {
    File dataLocation = new File(Globals.DATA_DIR + "webmasterCommands.base");
    TextLabels labels = SimpleTextLoader.load(dataLocation, false);
    super.checkWebMasterLines(labels.getTextBase());

    labels = SimpleTextLoader.load(dataLocation, true);
    super.checkWebMasterLines(labels.getTextBase());
    checkWebMasterLabels(labels);
  }

  protected void checkWebMasterLabels(TextLabels labels)
  {
    assertEquals(1, getNumLables(labels, "addToDatabaseCommand", "msg01"));
    assertEquals(0, getNumLables(labels, "addToDatabaseCommand", "msg02"));
    assertEquals(1, getNumLables(labels, "changeExistingTupleCommand", "msg03"));
  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
//    return new TestSuite(SimpleTextLoaderTest.class);
    TestSuite s = new TestSuite();
    s.addTest(new SimpleTextLoaderTest("testDir"));
    s.addTest(new SimpleTextLoaderTest("testLines"));
    return s;
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