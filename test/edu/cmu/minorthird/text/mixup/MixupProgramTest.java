package edu.cmu.minorthird.text.mixup;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.IOUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class MixupProgramTest extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());
  private File simpleFile;
  private File testData;

  private File dependFile;
  private File dependExplict;

  private File dependJava;

  private MonotonicTextLabels labels;
  private TextBase textBase;

  /**
   * Standard test class constructior for MixupProgramTest
   * @param name Name of the test
   */
  public MixupProgramTest(String name)
  {
    super(name);
    simpleFile = new File("lib/mixup/date.mixup");
    testData = new File("data/seminar-subset/cmu.andrew.official.career-center-1495_0");

    dependFile = new File("lib/mixup/names2.mixup");
    dependExplict = new File("lib/mixup/names.mixup");

//    dependJava = new File("lib/mixup/getNames.mixup");
  }

  /**
   * Convinence constructior for MixupProgramTest
   */
  public MixupProgramTest()
  {
    this("MixupProgramTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    try
    {
      Logger.getRootLogger().removeAllAppenders();
      org.apache.log4j.BasicConfigurator.configure();
      log.setLevel(Level.DEBUG);

      //load text base and get fresh labels
      this.textBase = new BasicTextBase();
      TextBaseLoader loader = new TextBaseLoader();
		  log.debug("testData: " + testData);
      loader.loadTaggedFile(this.testData, null, textBase);
      labels = new BasicTextLabels(textBase);

    }
    catch (IOException e)
    {
      log.error(e, e);  //To change body of catch statement use Options | File Templates.
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
   * Base test for MixupProgramTest
   */
  public MixupProgram fromFile(File f) throws Mixup.ParseException, IOException, FileNotFoundException
  {
    return new MixupProgram(f);
  }

  public MixupProgram fromString(File f) throws IOException, Mixup.ParseException
  {
	  return new MixupProgram(new String(IOUtil.readFile(f)));
  }

  public void runCode(File code, File outFile)
  {
    try
    {
      MixupProgram program = fromFile(code);
      program.eval(labels, textBase);

      setUp();
      program = fromString(code);
      program.eval(labels, textBase);

	    new TextLabelsLoader().saveTypesAsOps(labels, outFile);
    }
    catch (Exception e)
    {
      log.error(e, e);
      fail();
    }

  }

  public void testSimple()
  {
    log.debug("test Simple");
    runCode(simpleFile, new File("simpleTest.out"));
  }

  public void testCodeWithDependencies()
  {
    log.debug("Depend File");
    runCode(dependFile, new File("depend.out"));
  }

  public void testCodeWithExplicitDependency()
  {
    log.debug("Depend Explicit");
    runCode(dependExplict, new File("dependExplicit.out"));
  }

//  public void testCodeWithJavaDependency()
//  {
//    log.debug("Depend Java");
//    runCode(dependJava, new File("dependJava.out"));
//  }


  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(MixupProgramTest.class);
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