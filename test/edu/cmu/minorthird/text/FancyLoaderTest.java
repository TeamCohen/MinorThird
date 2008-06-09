package edu.cmu.minorthird.text;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import org.apache.log4j.Logger;
import edu.cmu.minorthird.util.Globals;

/**
 * @author ksteppe
 */
public class FancyLoaderTest extends TestCase{

	Logger log=Logger.getLogger(this.getClass());

	/**
	 * Standard test class constructior for FancyLoaderTest
	 * @param name Name of the test
	 */
	public FancyLoaderTest(String name){
		super(name);
	}

	/**
	 * Convinence constructior for FancyLoaderTest
	 */
	public FancyLoaderTest(){
		super("FancyLoaderTest");
	}

	/**
	 * setUp to run before each test
	 */
	protected void setUp(){
		Logger.getRootLogger().removeAllAppenders();
		org.apache.log4j.BasicConfigurator.configure();
		//TODO add initializations if needed
	}

	/**
	 * clean up to run after each test
	 */
	protected void tearDown(){
		//TODO clean up resources if needed
	}

	/** uses seminar-subset */
	public void testDirwithNoFile(){
		log.info("testDirwithNoFile");
		String script=Globals.DATA_DIR+"seminar-subset";
		FancyLoader.loadTextLabels(script);
	}

	public void testDirwithLabelFile(){
		log.info("testDirwithLabelFile");
		String script=Globals.DATA_DIR+"bayes-testData";
		FancyLoader.loadTextLabels(script);
	}

	public void testFilewithLabelFile(){
		log.info("testFilewithLabelFile");
		String script=Globals.DATA_DIR+"webmasterCommands";
		FancyLoader.loadTextLabels(script);
	}

	/**
	 * Base test for FancyLoaderTest
	 */
	public void testScript(){
		log.info("testScript");
		String script=Globals.DATA_DIR+"test.bsh";
		FancyLoader.loadTextLabels(script);
	}

	/**
	 * Creates a TestSuite from all testXXX methods
	 * @return TestSuite
	 */
	public static Test suite(){
		return new TestSuite(FancyLoaderTest.class);
	}

	/**
	 * Run the full suite of tests with text output
	 * @param args - unused
	 */
	public static void main(String args[]){
		junit.textui.TestRunner.run(suite());
	}
}
