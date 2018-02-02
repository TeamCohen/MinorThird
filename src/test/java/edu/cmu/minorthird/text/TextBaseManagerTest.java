package edu.cmu.minorthird.text;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;
import org.apache.log4j.Logger;

/**
 *
 * @author Quinten Mercer
 */
public class TextBaseManagerTest extends TestCase
{
    Logger log = Logger.getLogger(this.getClass());

    /** Sample documents to use for the test. */
    public static String[] sampleDocs = new String[]{ "Mary had a little lamb.  Its fleece was white as snow.",
                                                      "Eeny, meeny, miny, moe.  Catch a tiger by the toe.",
                                                      "Row, row, row your boat gently down the stream.  Merrily, merrily, merrily, life is but a dream.",
                                                      "Mary, Mary quite contrary, how does your garden grow?"};

    /**
     * Standard test class constructior for TextBaseTests
     * @param name Name of the test
     */
    public TextBaseManagerTest(String name) { super(name); }

    /** Convinence constructior for TextBaseTests */
    public TextBaseManagerTest() { super("TextBaseManagerTest"); }

    /** Set up steps to run before each test */
    protected void setUp() {
        Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.BasicConfigurator.configure();
        //TODO add initializations if needed
    }

    /** clean up steps to run after each test */
    protected void tearDown() {
        //TODO clean up resources if needed
    }


    //
    // the Tests
    //

    public void testRetokenize() {

        // Load some sample docs into a textbase
        BasicTextBase parentTextBase = new BasicTextBase();
        parentTextBase.loadDocument("doc0", sampleDocs[0]);
        parentTextBase.loadDocument("doc1", sampleDocs[1]);
        parentTextBase.loadDocument("doc2", sampleDocs[2]);
        parentTextBase.loadDocument("doc3", sampleDocs[3]);

        // Now create a labels set for this text base and add some annotations
        BasicTextLabels labels = new BasicTextLabels(parentTextBase);
        BasicSpan span1 = new BasicSpan("doc0", parentTextBase.getDocument("doc0").getTokens(), 1, 4, "doc0"); 
        labels.addToType(span1, "predicate");
        BasicSpan span2 = new BasicSpan("doc0", parentTextBase.getDocument("doc0").getTokens(), 8, 4, "doc0"); 
        labels.addToType(span2, "predicate");
        BasicSpan span3 = new BasicSpan("doc1", parentTextBase.getDocument("doc1").getTokens(), 11, 3, "doc1"); 
        labels.addToType(span3, "predicate");
        BasicSpan span4 = new BasicSpan("doc2", parentTextBase.getDocument("doc2").getTokens(), 8, 3, "doc2"); 
        labels.addToType(span4, "predicate");
        BasicSpan span5 = new BasicSpan("doc2", parentTextBase.getDocument("doc2").getTokens(), 19, 4, "doc2"); 
        labels.addToType(span5, "predicate");
        BasicSpan span6 = new BasicSpan("doc3", parentTextBase.getDocument("doc3").getTokens(), 6, 5, "doc3"); 
        labels.addToType(span6, "predicate");

        // Create a TextBaseManager to manage the different levels
        TextBaseManager tbman = new TextBaseManager("root", parentTextBase);

        // create a new tokenizer
        RegexTokenizer newTokenizer = new RegexTokenizer("([^\\s]+)");

        // call retokenize with this new stuff.
        MutableTextBase newTextBase = tbman.retokenize(newTokenizer, "root", "newLevel");

        // Check that the TextBaseManager stored the correct new textbase under the correct level name
        TextBase tb = tbman.getTextBase("newLevel");
        assertEquals(newTextBase, tb);

        // Check that there are the correct number of documents in the new text base
        assertEquals(parentTextBase.size(), newTextBase.size());        

        // Check that the documents in the new text base have the correct number of tokens
        assertEquals(11, newTextBase.documentSpan("doc0").size());
        assertEquals(10, newTextBase.documentSpan("doc1").size());
        assertEquals(17, newTextBase.documentSpan("doc2").size());
        assertEquals(9, newTextBase.documentSpan("doc3").size());

        // check that the textbase has the appropriate tokenizer for new docs
        assertEquals(newTokenizer, newTextBase.getTokenizer());

        // Test mapping from the root level to the new level.  Since the tokenizer we used split 
        //   tokens based on whitespace, the mapped spans will NOT have the exact same characters 
        //   as the originals.  If there were non-white space characters (such as punctuation) that 
        //   are next to the first token of the span (imeediately to the left) or the last token 
        //   of the span (immediately to the right), then the tokens that contain these chars will 
        //   be included in the mapped span.  This is because the new tokenizer will treat all of 
        //   these chars as a single token and since Spans are based on tokens and not chars, these 
        //   extra chars get added.

        // Start by mapping actual Span instances
        Span mappedSpan = tbman.getMatchingSpan(span1, "root", "newLevel");
        assertEquals("had a little lamb.", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span2, "root", "newLevel");
        assertEquals("was white as snow.", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span3, "root", "newLevel");
        assertEquals("by the toe.", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span4, "root", "newLevel");
        assertEquals("down the stream.", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span5, "root", "newLevel");
        assertEquals("is but a dream.", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span6, "root", "newLevel");
        assertEquals("how does your garden grow?", mappedSpan.asString());

        // Now map some random char offsets to make sure that they get mapped to corresponding spans correctly
        mappedSpan = tbman.getMatchingSpan("root", "doc0", 13, 20, "newLevel");
        assertEquals("little lamb.  Its fleece", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan("root", "doc1", 7, 20, "newLevel");
        assertEquals("meeny, miny, moe.  Catch", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan("root", "doc2", 26, 27, "newLevel");
        assertEquals("gently down the stream.  Merrily,", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan("root", "doc3", 20, 24, "newLevel");
        assertEquals("contrary, how does your garden", mappedSpan.asString());

    }

    public void testFilter() {

        // Create a text base to house the sample docs
        BasicTextBase parentTextBase = new BasicTextBase();
        parentTextBase.loadDocument("doc0", sampleDocs[0]);
        parentTextBase.loadDocument("doc1", sampleDocs[1]);
        parentTextBase.loadDocument("doc2", sampleDocs[2]);
        parentTextBase.loadDocument("doc3", sampleDocs[3]);

        // Now create a labels set for this text base and add some annotations
        BasicTextLabels labels = new BasicTextLabels(parentTextBase);
        BasicSpan span1 = new BasicSpan("doc0", parentTextBase.getDocument("doc0").getTokens(), 1, 4, "doc0");
        labels.addToType(span1, "predicate");
        BasicSpan span2 = new BasicSpan("doc0", parentTextBase.getDocument("doc0").getTokens(), 8, 4, "doc0");
        labels.addToType(span2, "predicate");
        BasicSpan span3 = new BasicSpan("doc1", parentTextBase.getDocument("doc1").getTokens(), 11, 3, "doc1");
        labels.addToType(span3, "predicate");
        BasicSpan span4 = new BasicSpan("doc2", parentTextBase.getDocument("doc2").getTokens(), 8, 3, "doc2");
        labels.addToType(span4, "predicate");
        BasicSpan span5 = new BasicSpan("doc2", parentTextBase.getDocument("doc2").getTokens(), 19, 4, "doc2");
        labels.addToType(span5, "predicate");
        BasicSpan span6 = new BasicSpan("doc3", parentTextBase.getDocument("doc3").getTokens(), 6, 5, "doc3");
        labels.addToType(span6, "predicate");

        // Create a TextBaseManager to manage the different levels
        TextBaseManager tbman = new TextBaseManager("root", parentTextBase);

        // call filter with this new stuff.
        TextBase newTextBase = tbman.filter("root", labels, "newLevel", "predicate");

        // Check that the TextBaseManager stored the correct new textbase under the correct level name
        TextBase tb = tbman.getTextBase("newLevel");
        assertEquals(newTextBase, tb);

        // Check that there are the correct number of documents in the new text base.  In this case
        //   since we filtered on the "predicate" type there should be one doc in the new text base
        //   for each instance on this span type in the original text base.
        assertEquals(6, newTextBase.size());

        // Check that the documents in the new text base have the correct number of tokens
        assertEquals(4, newTextBase.documentSpan("childTB0-doc0").size());
        assertEquals(4, newTextBase.documentSpan("childTB1-doc0").size());
        assertEquals(3, newTextBase.documentSpan("childTB0-doc1").size());
        assertEquals(3, newTextBase.documentSpan("childTB0-doc2").size());
        assertEquals(4, newTextBase.documentSpan("childTB1-doc2").size());
        assertEquals(5, newTextBase.documentSpan("childTB0-doc3").size());

        // Test mapping from the root level to the new level.  Since the tokenizer we used split
        //   tokens was the same as the original text base, the mapped spans should have the EXACT
        //   same characters as the originals.

        // Start by mapping actual Span instances
        Span mappedSpan = tbman.getMatchingSpan(span1, "root", "newLevel");
        assertEquals("had a little lamb", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span2, "root", "newLevel");
        assertEquals("was white as snow", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span3, "root", "newLevel");
        assertEquals("by the toe", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span4, "root", "newLevel");
        assertEquals("down the stream", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span5, "root", "newLevel");
        assertEquals("is but a dream", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan(span6, "root", "newLevel");
        assertEquals("how does your garden grow", mappedSpan.asString());
        
        // Now map some random char offsets to make sure that they get mapped to corresponding spans correctly
        mappedSpan = tbman.getMatchingSpan("root", "doc0", 13, 8, "newLevel");
        assertEquals("little lamb", mappedSpan.asString());
        mappedSpan = tbman.getMatchingSpan("root", "doc3", 26, 18, "newLevel");
        assertEquals("how does your garden", mappedSpan.asString());

        // There should be no matching spans for these because the char sequences I've specified
        // were not part of an instance of the span type I filtered the text base on.
        mappedSpan = tbman.getMatchingSpan("root", "doc1", 7, 13, "newLevel");
        assertNull(mappedSpan);
        mappedSpan = tbman.getMatchingSpan("root", "doc2", 26, 18, "newLevel");
        assertNull(mappedSpan);
    }

    public void testMultiLevel() {
        // Load some sample docs into a textbase
        BasicTextBase parentTextBase = new BasicTextBase();
        parentTextBase.loadDocument("doc0", sampleDocs[0]);
        parentTextBase.loadDocument("doc1", sampleDocs[1]);
        parentTextBase.loadDocument("doc2", sampleDocs[2]);
        parentTextBase.loadDocument("doc3", sampleDocs[3]);

        // Now create a labels set for this text base and add some annotations
        BasicTextLabels labels = new BasicTextLabels(parentTextBase);
        BasicSpan span1 = new BasicSpan("doc0", parentTextBase.getDocument("doc0").getTokens(), 1, 4, "doc0");
        labels.addToType(span1, "predicate");
        BasicSpan span2 = new BasicSpan("doc0", parentTextBase.getDocument("doc0").getTokens(), 8, 4, "doc0");
        labels.addToType(span2, "predicate");
        BasicSpan span3 = new BasicSpan("doc1", parentTextBase.getDocument("doc1").getTokens(), 11, 3, "doc1");
        labels.addToType(span3, "predicate");
        BasicSpan span4 = new BasicSpan("doc2", parentTextBase.getDocument("doc2").getTokens(), 8, 3, "doc2");
        labels.addToType(span4, "predicate");
        BasicSpan span5 = new BasicSpan("doc2", parentTextBase.getDocument("doc2").getTokens(), 19, 4, "doc2");
        labels.addToType(span5, "predicate");
        BasicSpan span6 = new BasicSpan("doc3", parentTextBase.getDocument("doc3").getTokens(), 6, 5, "doc3");
        labels.addToType(span6, "predicate");

        // Create a TextBaseManager to manage the different levels
        TextBaseManager tbman = new TextBaseManager("root", parentTextBase);

        // create a new tokenizer
        RegexTokenizer newTokenizer = new RegexTokenizer("([^\\s]+)");
        // call retokenize with this new stuff.
        tbman.retokenize(newTokenizer, "root", "retok");
        // call filter with this new stuff.
        TextBase filteredTextBase = tbman.filter("retok", labels, "filtered", "predicate");


        // Check that there are the correct number of documents in the new text base.  In this case
        //   since we filtered on the "predicate" type there should be one doc in the new text base
        //   for each instance on this span type in the original text base.
        assertEquals(6, filteredTextBase.size());

        // Check that the documents in the new text base have the correct number of tokens
        assertEquals(4, filteredTextBase.documentSpan("childTB0-doc0").size());
        assertEquals(4, filteredTextBase.documentSpan("childTB1-doc0").size());
        assertEquals(3, filteredTextBase.documentSpan("childTB0-doc1").size());
        assertEquals(3, filteredTextBase.documentSpan("childTB0-doc2").size());
        assertEquals(4, filteredTextBase.documentSpan("childTB1-doc2").size());
        assertEquals(5, filteredTextBase.documentSpan("childTB0-doc3").size());

        // check that the docs have the appropriate tokenization
        assertEquals(4, filteredTextBase.getDocument("childTB0-doc0").getTokens().length);
        assertEquals(4, filteredTextBase.getDocument("childTB1-doc0").getTokens().length);
        assertEquals(3, filteredTextBase.getDocument("childTB0-doc1").getTokens().length);
        assertEquals(3, filteredTextBase.getDocument("childTB0-doc2").getTokens().length);
        assertEquals(4, filteredTextBase.getDocument("childTB1-doc2").getTokens().length);
        assertEquals(5, filteredTextBase.getDocument("childTB0-doc3").getTokens().length);

    }


    /**
     * Creates a TestSuite from all testXXX methods
     * @return TestSuite
     */
    public static Test suite() { return new TestSuite(TextBaseManagerTest.class); }

    /**
     * Run the full suite of tests with text output
     * @param args - unused
     */
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}
