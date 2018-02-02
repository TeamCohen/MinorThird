package edu.cmu.minorthird.text;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;
import org.apache.log4j.Logger;

/**
 * This set of tests checks out the basic functionality of the TextBase class
 * hierarchy.  It tests loading documents, querying for its properties, creating
 * document spans, document span loopers, etc.
 *
 * @author Quinten Mercer
 */
public class TextBaseTest extends TestCase
{
    Logger log = Logger.getLogger(this.getClass());
    
    /** Sample documents to use for the test. */
    public static String[] sampleDocs = new String[]{ "Mary had a little lamb.  Its fleece was white as snow.", 
                                                      "Eeny, meeny, miny, moe.  Catch a tiger by the toe.", 
                                                      "Row, row, row your boat gently down the stream.  Merrily, merrily, merrily, life is but a dream.", 
                                                      "Mary, Mary quite contrary, how does your garden grow?"};

    /**
     * Standard test class constructior for TextBaseTest
     * @param name Name of the test
     */
    public TextBaseTest(String name) { super(name); }
    
    /** Convinence constructior for TextBaseTest */
    public TextBaseTest() { super("TextBaseTest"); }
    
    /** Set up steps to run before each test */
    protected void setUp() {
        Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.BasicConfigurator.configure();
        //TODO add initializations if needed
    }
    
    /** Clean up steps to run after each test */
    protected void tearDown() {
        //TODO clean up resources if needed
    }


    //
    // Tests
    //

    public void testBasicTextBase() {

        // Create the TextBase instance to use for testing
        BasicTextBase tb = new BasicTextBase(new RegexTokenizer());

        // Add all the sample docs to the TextBase
        for (int i=0;i<TextBaseTest.sampleDocs.length;i++) {
            tb.loadDocument("doc_"+i, TextBaseTest.sampleDocs[i]);
        }

        // Check to make sure that the correct number of docs were loaded.
        assertEquals(TextBaseTest.sampleDocs.length, tb.size());

        // Check to make sure that the docs have the correct document ids
        assertNotNull(tb.getDocument("doc_0"));
        assertNotNull(tb.getDocument("doc_1"));
        assertNotNull(tb.getDocument("doc_2"));
        assertNotNull(tb.getDocument("doc_3"));
        
        // Check that the loaded docs have the correct text
        assertEquals(tb.getDocument("doc_0").getText(), TextBaseTest.sampleDocs[0]);
        assertEquals(tb.getDocument("doc_1").getText(), TextBaseTest.sampleDocs[1]);
        assertEquals(tb.getDocument("doc_2").getText(), TextBaseTest.sampleDocs[2]);
        assertEquals(tb.getDocument("doc_3").getText(), TextBaseTest.sampleDocs[3]);

        // Check that the documents have the correct number of tokens.
        assertEquals(tb.documentSpan("doc_0").size(), 13);
        assertEquals(tb.documentSpan("doc_1").size(), 15);
        assertEquals(tb.documentSpan("doc_2").size(), 24);
        assertEquals(tb.documentSpan("doc_3").size(), 12);        
    }


    /**
     * Creates a TestSuite from all testXXX methods
     * @return TestSuite
     */
    public static Test suite() { return new TestSuite(TextBaseTest.class); }

    /**
     * Run the full suite of tests with text output
     * @param args - unused
     */
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
