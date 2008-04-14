package edu.cmu.minorthird.text;

import edu.cmu.minorthird.util.Globals;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class TextLabelsLoaderTest extends TestCase{

	Logger log=Logger.getLogger(this.getClass());

	private String dataFile=Globals.DATA_DIR+"webmasterCommands.base";

	private String labelsFile=Globals.DATA_DIR+"webmasterCommands.labels";

	private MutableTextLabels labels;

	/**
	 * Standard test class constructior for TextLabelsLoaderTest
	 * @param name Name of the test
	 */
	public TextLabelsLoaderTest(String name){
		super(name);
	}

	/**
	 * Convinence constructior for TextLabelsLoaderTest
	 */
	public TextLabelsLoaderTest(){
		super("TextLabelsLoaderTest");
	}

	/**
	 * setUp to run before each test
	 */
	protected void setUp(){
		Logger.getRootLogger().removeAllAppenders();
		org.apache.log4j.BasicConfigurator.configure();
		//TODO add initializations if needed
		Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	/**
	 * clean up to run after each test
	 */
	protected void tearDown(){
		//TODO clean up resources if needed
	}

	public void testClosureOutput(){
		try{
			labelsFile=Globals.DATA_DIR+"webmasterCommands.closeSome.labels";
			loadLabels(); //loads up the labels object

			File outFile=new File(Globals.DATA_DIR+"webmaster.closeDocs.testOut");
			TextLabelsLoader saver=new TextLabelsLoader();
			saver.setClosurePolicy(TextLabelsLoader.CLOSE_TYPES_IN_LABELED_DOCS);
			saver.saveTypesAsOps(labels,outFile);
			BufferedReader in=new BufferedReader(new FileReader(outFile));

			String line="";
			int closures=0;
			while(in.ready()){
				line=in.readLine();
				if(line.startsWith("closeType"))
					closures++;
			}

			assertEquals(16,closures);
		}catch(Exception e){
			log.error(e,e);
			fail();
		}
	}

	public void testClosureOperations(){
		log.info("------------- testClosureOperations -----------------");
		try{
			labelsFile=Globals.DATA_DIR+"webmasterCommands.closeSome.labels";
			loadLabels();

			int count;
			Iterator<Span> it=labels.closureIterator("addToDatabaseCommand");
			for(count=0;it.hasNext();count++);
			assertEquals(6,count);

			it=labels.closureIterator("changeExistingTupleCommand");
			for(count=0;it.hasNext();count++);
			assertEquals(4,count);

			it=labels.closureIterator("otherCommand");
			for(count=0;it.hasNext();count++);
			assertEquals(3,count);

		}catch(Exception e){
			log.error(e,e);
			fail(e.getMessage());
		}
		log.info("------------- testClosureOperations -----------------");
	}

	public void testClosurePolicies(){
		log.info("------------- testClosurePolicies -----------------");
		try{
			int count;
			loadLabels(TextLabelsLoader.CLOSE_ALL_TYPES); //loads up the labels object
			Iterator<Span> it=labels.closureIterator("addToDatabaseCommand");
			for(count=0;it.hasNext();count++);
			assertEquals(40,count);

			labelsFile=Globals.DATA_DIR+"webmasterCommands.closeAll.labels";
			loadLabels(); //loads up the labels object
			it=labels.closureIterator("addToDatabaseCommand");
			for(count=0;it.hasNext();count++);
			assertEquals(40,count);

			labelsFile=Globals.DATA_DIR+"webmasterCommands.closeDocs.labels";
			loadLabels(); //loads up the labels object
			it=labels.closureIterator("addToDatabaseCommand");
			for(count=0;it.hasNext();count++);
			assertEquals(19,count);

			labelsFile=Globals.DATA_DIR+"webmasterCommands.closeNone.labels";
			loadLabels(); //loads up the labels object
			it=labels.closureIterator("addToDatabaseCommand");
			for(count=0;it.hasNext();count++);
			assertEquals(0,count);
		}catch(Exception e){
			log.fatal(e,e);
			fail();
		}
		log.info("------------- testClosurePolicies -----------------");
	}

	/**
	 * Base test for TextLabelsLoaderTest
	 */
	public void testImportOps(){
		try{
			loadLabels(TextLabelsLoader.CLOSE_ALL_TYPES);
		}catch(Exception e){
			log.error(e,e);
			fail();
		}
	}

	private void loadLabels() throws Exception{
		loadLabels(-1);
	}

	/**
	 * convinence for loading the labels into the class level labels object
	 * @param closurePolicy TLL policy to load with
	 */
	private void loadLabels(int closurePolicy) throws Exception{
		TextBaseLoader tbloader=new TextBaseLoader(TextBaseLoader.DOC_PER_LINE);
		TextBase base=tbloader.load(new File(dataFile));
		TextLabelsLoader loader=new TextLabelsLoader();

		File labelFile=new File(this.labelsFile);
		if(closurePolicy>-1)
			loader.setClosurePolicy(closurePolicy);

		labels=new BasicTextLabels();
		labels.setTextBase(base);
		loader.importOps(labels,base,labelFile);
	}

	/**
	 * Creates a TestSuite from all testXXX methods
	 * @return TestSuite
	 */
	public static Test suite(){
		return new TestSuite(TextLabelsLoaderTest.class);
	}

	/**
	 * Run the full suite of tests with text output
	 * @param args - unused
	 */
	public static void main(String args[]){
		junit.textui.TestRunner.run(suite());
	}
}