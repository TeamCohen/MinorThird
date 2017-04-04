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

public class BasicDatasetTest extends TestCase{
	
	Logger log=Logger.getLogger(this.getClass());

	/**
	 * Standard test class constructior for BasicDatasetTest
	 * @param name Name of the test
	 */
	public BasicDatasetTest(String name){
		super(name);
	}

	/**
	 * Convinence constructior for BasicDatasetTest
	 */
	public BasicDatasetTest(){
		super("BasicDatasetTest");
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

	/**
	 * Base test for BasicDatasetTest
	 */
	public void testBasicDatasetTest(){

		try{
			BasicDataset data=(BasicDataset)SampleDatasets.sampleData("toy",false);
			assertTrue(data.toGUI()!=null);
			log.debug(data.getSchema());
		}catch(Exception e){
			log.error(e,e);
		}

	}

	/**
	 * Creates a TestSuite from all testXXX methods
	 * @return TestSuite
	 */
	public static Test suite(){
		return new TestSuite(BasicDatasetTest.class);
	}

	/**
	 * Run the full suite of tests with text output
	 * @param args - unused
	 */
	public static void main(String args[]){
		junit.textui.TestRunner.run(suite());
	}
}
