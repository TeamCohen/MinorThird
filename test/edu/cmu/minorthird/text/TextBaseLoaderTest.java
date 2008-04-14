package edu.cmu.minorthird.text;

import java.io.File;
import java.util.Iterator;

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
public class TextBaseLoaderTest extends TestCase
{

	Logger log = Logger.getLogger(this.getClass());
	protected final String testCaseDir="test/edu/cmu/minorthird/text/testcases";


	/**
	 * Standard test class constructior for TextBaseLoaderTest
	 * @param name Name of the test
	 */
	public TextBaseLoaderTest(String name) {
		super(name);
	}

	/** Convinence constructior for TextBaseLoaderTest */
	public TextBaseLoaderTest() {
		super("TextBaseLoaderTest");
	}

	/** setUp to run before each test */
	protected void setUp() {
		Logger.getRootLogger().removeAllAppenders();
		org.apache.log4j.BasicConfigurator.configure();
		//TODO add initializations if needed
	}

	/** clean up to run after each test */
	protected void tearDown() {
		//TODO clean up resources if needed
	}

	//
	// The Tests
	//

	/**
	 * Begin by testing the basic loading of a data file functionality.  It tests both using the default
	 * tokenizer or specifying a custom tokenizer.
	 */
	public void testLoadDataFile() {
		try {
			//
			// Try the basic test of just loading a file with good data (ie no blamk lines, etc) using the standard tokenizer
			//
			TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_LINE, TextBaseLoader.USE_XML);
			TextBase base = loader.load(new File(testCaseDir+"/DocPerLineTestData.base"));
			MutableTextLabels labels = loader.getLabels();

			// Check that the proper number of documents were loaded from the file.
			assertEquals(7, base.size());

			// Check that all the docs were loaded correctly
			Span docSpan = base.documentSpan("DocPerLineTestData.base@line:1");
			assertNotNull(docSpan);
			assertEquals(19, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData.base@line:2");
			assertNotNull(docSpan);
			assertEquals(12, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData.base@line:3");
			assertNotNull(docSpan);
			assertEquals(12, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData.base@line:4");
			assertNotNull(docSpan);
			assertEquals(19, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData.base@line:5");
			assertNotNull(docSpan);
			assertEquals(11, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData.base@line:6");
			assertNotNull(docSpan);
			assertEquals(17, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData.base@line:7");
			assertNotNull(docSpan);
			assertEquals(6, docSpan.size());

			// Lastly make sure that all embedded types were loaded
			this.checkType(labels, "stime", "DocPerLineTestData.base@line:1", "4:00", 1);
			this.checkType(labels, "location", "DocPerLineTestData.base@line:1", "Adamson Wing, Baker Hall", 1);
			this.checkType(labels, "speaker", "DocPerLineTestData.base@line:2", "George W. Cobb", 1);
			this.checkType(labels, "title", "DocPerLineTestData.base@line:3", "Title: Three Ways to Gum up a Statistics Course", 1);
			this.checkType(labels, "sentence", "DocPerLineTestData.base@line:4", "My talk will be in two parts", 1);
			this.checkType(labels, "comment", "DocPerLineTestData.base@line:5", "comments and observations", 1);
			this.checkType(labels, "country", "DocPerLineTestData.base@line:6", "US", 1);


			//
			// Next repeat these tests with a dataset that has blank lines in it to make sure these are skipped
			//
			loader = new TextBaseLoader(TextBaseLoader.DOC_PER_LINE, TextBaseLoader.USE_XML);
			base = loader.load(new File(testCaseDir+"/DocPerLineTestData_WithBlanks.base"));
			labels = loader.getLabels();

			// Check that the proper number of documents were loaded from the file.
			//WARNING: THIS IS A KNOWN BUG THAT BLANK LINES ARE INCLUDED.  LEAVE THE TEST FAILING 
			//         TO REMIND US TO FIX THE BUG!!!
			assertEquals(7, base.size());

			// Check that all the docs were loaded correctly
			docSpan = base.documentSpan("DocPerLineTestData_WithBlanks.base@line:4");
			assertNotNull(docSpan);
			assertEquals(19, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData_WithBlanks.base@line:5");
			assertNotNull(docSpan);
			assertEquals(12, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData_WithBlanks.base@line:6");
			assertNotNull(docSpan);
			assertEquals(12, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData_WithBlanks.base@line:7");
			assertNotNull(docSpan);
			assertEquals(19, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData_WithBlanks.base@line:11");
			assertNotNull(docSpan);
			assertEquals(11, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData_WithBlanks.base@line:12");
			assertNotNull(docSpan);
			assertEquals(17, docSpan.size());
			docSpan = base.documentSpan("DocPerLineTestData_WithBlanks.base@line:13");
			assertNotNull(docSpan);
			assertEquals(6, docSpan.size());

			// Lastly make sure that all embedded types were loaded
			this.checkType(labels, "stime", "DocPerLineTestData_WithBlanks.base@line:4", "4:00", 1);
			this.checkType(labels, "location", "DocPerLineTestData_WithBlanks.base@line:4", "Adamson Wing, Baker Hall", 1);
			this.checkType(labels, "speaker", "DocPerLineTestData_WithBlanks.base@line:5", "George W. Cobb", 1);
			this.checkType(labels, "title", "DocPerLineTestData_WithBlanks.base@line:6", "Title: Three Ways to Gum up a Statistics Course", 1);
			this.checkType(labels, "sentence", "DocPerLineTestData_WithBlanks.base@line:7", "My talk will be in two parts", 1);
			this.checkType(labels, "comment", "DocPerLineTestData_WithBlanks.base@line:11", "comments and observations", 1);
			this.checkType(labels, "country", "DocPerLineTestData_WithBlanks.base@line:12", "US", 1);
		}
		catch (Exception e) {
			log.fatal(e.getMessage(), e);
			fail("testLoadDataFile failed because an exception occurred: " + e.getMessage());
		}
	}


	public void testLoadDirectoryOfFiles() {
		try {
			//First test loading a directory of files that have embedded labels
			TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE, TextBaseLoader.USE_XML);
			TextBase base = loader.load(new File(testCaseDir+"/SeminarAnnouncements"));
			MutableTextLabels labels = loader.getLabels();

			// Check that the proper number of documents were loaded from the directory.
			assertEquals(15, base.size());

			// Check that all the docs were loaded correctly
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2450_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2457_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2477_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2513_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2516_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2527_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2611_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2627_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2633_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2674_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2680_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2737_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2752_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2811_0"));
			assertNotNull(base.documentSpan("cmu.andrew.official.cmu-news-2912_1"));

			// Check the the correct number of instances of each label were loaded
			assertEquals(3, this.getNumLabels(labels, "etime"));
			assertEquals(15, this.getNumLabels(labels, "location"));
			assertEquals(18, this.getNumLabels(labels, "paragraph"));
			assertEquals(49, this.getNumLabels(labels, "sentence"));
			assertEquals(12, this.getNumLabels(labels, "speaker"));
			assertEquals(14, this.getNumLabels(labels, "stime"));

			// Check a number of the embedded type to make sure they were loaded correctly
			this.checkType(labels, "etime", "cmu.andrew.official.cmu-news-2611_0", "5:00 P.M.", 1);
			this.checkType(labels, "etime", "cmu.andrew.official.cmu-news-2457_0", "1:00pm", 1);
			this.checkType(labels, "etime", "cmu.andrew.official.cmu-news-2674_0", "1:30 p.m.", 1);
			this.checkType(labels, "location", "cmu.andrew.official.cmu-news-2527_0", "Baker Hall 235A", 1);
			this.checkType(labels, "speaker", "cmu.andrew.official.cmu-news-2611_0", "FARRO F. RADJY, PH.D.", 1);
			this.checkType(labels, "stime", "cmu.andrew.official.cmu-news-2752_0", "12:00 pm", 1);
		}
		catch(Exception e) {
			log.fatal(e.getMessage(), e);
			fail("testLoadDirectoryOfFiles failed because an exception occurred: " + e.getMessage());
		}
	}

	public void testLoadWordPerLineFile() {
		try {
			TextBaseLoader loader = new TextBaseLoader();
			TextBase base = loader.loadWordPerLineFile(new File(testCaseDir+"/eng.base"));
			MutableTextLabels labels = loader.getLabels();

			// Check that the proper number of documents were loaded from the file.
			assertEquals(216, base.size());

			// Check a couple of the docs to make sure that the text was loaded correctly
			String msg1 = "BOXING - JOHNSON WINS UNANIMOUS POIUNTS VERDICT . DUBLIN 1996-08-31 American Tom Johnson successfully defended his IBF featherweight title when he earned a unanimous points decision over Venezuela 's Ramon Guzman on Saturday . ";
			String msg2 = "SOCCER - RESULT IN SPANISH FIRST DIVISION . MADRID 1996-08-31 Result of game played in the Spanish first division on Saturday : Deportivo Coruna 1 Real Madrid 1 ";
			String msg3 = "SOCCER - ARMENIA AND PORTUGAL DRAW 0-0 IN WORLD CUP QUALIFIER . YEREVAN 1996-08-31 Armenia and Portugal drew 0-0 in a World Cup soccer European group 9 qualifier on Saturday . Attendance : 5,000 ";
			String msg4 = "SOCCER - AUSTRIA DRAW 0-0 WITH SCOTLAND IN WORLD CUP QUALIFIER . VIENNA 1996-08-31 Austria and Scotland drew 0-0 in a World Cup soccer European group four qualifier on Saturday . Attendance : 29,500 ";
			String msg5 = "BASKETBALL - INTERNATIONAL TOURNAMENT RESULT . BELGRADE 1996-08-30 Result in an international basketball tournament on Friday : Red Star ( Yugoslavia ) beat Dinamo ( Russia ) 92-90 ( halftime 47-47 ) ";
			String msg6 = "RUGBY LEAGUE - WIGAN BEAT BRADFORD 42-36 IN SEMIFINAL . WIGAN , England 1996-08-31 Result of English rugby league premiership semifinal played on Saturday : Wigan 42 Bradford Bulls 36 ";

			assertEquals(msg1, base.getDocument("eng.base-155").getText());
			assertEquals(msg2, base.getDocument("eng.base-160").getText());
			assertEquals(msg3, base.getDocument("eng.base-136").getText());
			assertEquals(msg4, base.getDocument("eng.base-162").getText());
			assertEquals(msg5, base.getDocument("eng.base-5").getText());
			assertEquals(msg6, base.getDocument("eng.base-102").getText());

			// Some of the fields in this format get translated to span types.  Check that these
			// get created (and in the correct amounts).
			assertEquals(4, this.getNumLabels(labels, "B-MISC"));
			assertEquals(2094, this.getNumLabels(labels, "I-LOC"));
			assertEquals(1264, this.getNumLabels(labels, "I-MISC"));
			assertEquals(2092, this.getNumLabels(labels, "I-ORG"));
			assertEquals(3149, this.getNumLabels(labels, "I-PER"));

		}
		catch (Exception e) {
			log.fatal(e.getMessage(), e);
			fail("testLoadWordPerLineFile failed because an exception occurred: " + e.getMessage());
		}
	}


	//
	// Helper methods 
	//

	// returns the number of times the given type appears in the doc
	private int getNumLabels(TextLabels labels, String type) {
		int i = 0;
		for (Iterator<Span> l = labels.instanceIterator(type); l.hasNext(); ) {
			log.debug(l.next().asString());
			i++;
		}

		return i;
	}

	// Asserts that there is an instance of the specified type, that this instance has the specified 
	// value and that it appears (with that value) the specified number of times
	private void checkType(TextLabels labels, String type, String doc, String value, int num) {
		int i = 0;
		for (Iterator<Span> l = labels.instanceIterator(type, doc); l.hasNext(); i++) {
			Span s = l.next();
			assertEquals(value, s.asString());
		}
		assertEquals(num, i);
	}

	/**
	 * Creates a TestSuite from all testXXX methods
	 * @return TestSuite
	 */
	public static Test suite()
	{
		return new TestSuite(TextBaseLoaderTest.class);
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
