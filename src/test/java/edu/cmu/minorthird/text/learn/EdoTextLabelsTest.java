package edu.cmu.minorthird.text.learn;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.cmu.minorthird.text.TextBaseLoader;
import edu.cmu.minorthird.util.Globals;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class EdoTextLabelsTest extends ClassifyTest{

	/**
	 * Standard test class constructior for EdoTextLabelsTest
	 * @param name Name of the test
	 */
	public EdoTextLabelsTest(String name,String document){
		super(name);
		this.documentId=document;
	}

	/**
	 * Convinence constructior for EdoTextLabelsTest
	 */
	public EdoTextLabelsTest(){
		super("EdoTextLabelsTest");
	}

	/**
	 * setUp to run before each test
	 */
	protected void setUp(){
		org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
		org.apache.log4j.BasicConfigurator.configure();

	}

	public void spanTest(){
		try{
			dataFile=Globals.DATA_DIR+"bayes-testData";
			labelsFile=Globals.DATA_DIR+"bayes-testData.labels";
			labelString="rr";
			documentId="ph1.txt";

			TextBaseLoader loader=new TextBaseLoader();
			base=loader.load(new File(dataFile));

// check
			super.checkSpans();

		}catch(Exception e){
			log.fatal(e,e);
			fail();
		}
	}

	/**
	 * clean up to run after each test
	 */
	protected void tearDown(){
		//TODO clean up resources if needed
	}

	/**
	 * Creates a TestSuite of spanTests
	 * @return TestSuite
	 */
	public static Test suite(){
		TestSuite suite=new TestSuite();
		suite.addTest(new EdoTextLabelsTest("spanTest","ph1.text"));
		suite.addTest(new EdoTextLabelsTest("spanTest","rr1.text"));
		return suite;
	}

	/**
	 * Run the full suite of tests with text output
	 * @param args - unused
	 */
	public static void main(String args[]){
		junit.textui.TestRunner.run(suite());
	}
}