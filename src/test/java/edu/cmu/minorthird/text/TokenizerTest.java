package edu.cmu.minorthird.text;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;
import org.apache.log4j.Logger;

/**
 *
 * @author Quinten Mercer
 */
public class TokenizerTest extends TestCase
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
    public TokenizerTest(String name) { super(name); }

    /** Convinence constructior for TextBaseTests */
    public TokenizerTest() { super("TokenizerTest"); }

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

    public void testRegexTokenizer() {

        System.out.println("Testing the RegexTokenizer");

        //
        // First test it with the default pattern
        // 
        RegexTokenizer tokenizer = new RegexTokenizer();
        
        // Test splitting each sample as a string
        String[] tokens1 = tokenizer.splitIntoTokens(sampleDocs[0]);
        assertEquals(13, tokens1.length);
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[1]);
        assertEquals(15, tokens1.length);
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[2]);
        assertEquals(24, tokens1.length);
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[3]);
        assertEquals(12, tokens1.length);

        // Then put each sample into a doc and test again
        Document doc = new Document("doc0", sampleDocs[0]);
        TextToken[] tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(13, tokens2.length);
        doc = new Document("doc1", sampleDocs[1]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(15, tokens2.length);
        doc = new Document("doc2", sampleDocs[2]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(24, tokens2.length);
        doc = new Document("doc3", sampleDocs[3]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(12, tokens2.length);

        //
        // Then test it with a custom pattern
        //
        String newPattern = "([^\\s]+)";
        tokenizer = new RegexTokenizer(newPattern);

        // Test splitting each sample as a string
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[0]);
        assertEquals(11, tokens1.length);
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[1]);
        assertEquals(10, tokens1.length);
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[2]);
        assertEquals(17, tokens1.length);
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[3]);
        assertEquals(9, tokens1.length);

        // Then put each sample into a doc and test again
        doc = new Document("doc0", sampleDocs[0]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(11, tokens2.length);
        doc = new Document("doc1", sampleDocs[1]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(10, tokens2.length);
        doc = new Document("doc2", sampleDocs[2]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(17, tokens2.length);
        doc = new Document("doc3", sampleDocs[3]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(9, tokens2.length);
    }

    public void testSpanTypeTokenizer() {

        System.out.println("Testing the SpanTypeTokenizer");

        // Create a text base to house the sample documents
        BasicTextBase textBase = new BasicTextBase();
        textBase.loadDocument("doc0", sampleDocs[0]);
        textBase.loadDocument("doc1", sampleDocs[1]);
        textBase.loadDocument("doc2", sampleDocs[2]);
        textBase.loadDocument("doc3", sampleDocs[3]);

        // Now create a labels set for this text base and add some annotations
        BasicTextLabels labels = new BasicTextLabels(textBase);
        labels.addToType(new BasicSpan("doc0", textBase.getDocument("doc0").getTokens(), 0, 6, "doc0"), "sentence");
        labels.addToType(new BasicSpan("doc0", textBase.getDocument("doc0").getTokens(), 6, 7, "doc0"), "sentence");
        labels.addToType(new BasicSpan("doc1", textBase.getDocument("doc1").getTokens(), 0, 8, "doc1"), "sentence");
        labels.addToType(new BasicSpan("doc2", textBase.getDocument("doc2").getTokens(), 12, 12, "doc2"), "sentence");
        labels.addToType(new BasicSpan("doc3", textBase.getDocument("doc3").getTokens(), 0, 12, "doc3"), "sentence");

        // Create a SpanTypeTokenizer to make each sentence a token.
        SpanTypeTokenizer spanTypeTokenizer = new SpanTypeTokenizer("sentence", labels);

        // Tokenize the sample strings checking to make sure that it uses the base tokenizer
        // since without being in the context of a Document, there is no way to reconcile 
        // this doc back to the parent labels set.
        String[] tokens1 = spanTypeTokenizer.splitIntoTokens(sampleDocs[0]);
        assertEquals(13, tokens1.length);
        tokens1 = spanTypeTokenizer.splitIntoTokens(sampleDocs[1]);
        assertEquals(15, tokens1.length);
        tokens1 = spanTypeTokenizer.splitIntoTokens(sampleDocs[2]);
        assertEquals(24, tokens1.length);
        tokens1 = spanTypeTokenizer.splitIntoTokens(sampleDocs[3]);
        assertEquals(12, tokens1.length);

        // Now put the sample strings into new docs with the same doc ids (a requirement of the
        // SpanTypeTokenizer class) and tokenize them with the new Tokenizer to make sure they
        // are tokenized into one token per sentence.
        Document newDoc0 = new Document("doc0", sampleDocs[0]);
        Document newDoc1 = new Document("doc1", sampleDocs[1]);
        Document newDoc2 = new Document("doc2", sampleDocs[2]);
        Document newDoc3 = new Document("doc3", sampleDocs[3]);
        TextToken[] tokens2 = spanTypeTokenizer.splitIntoTokens(newDoc0);
        assertEquals(2, tokens2.length);
        tokens2 = spanTypeTokenizer.splitIntoTokens(newDoc1);
        assertEquals(8, tokens2.length);
        tokens2 = spanTypeTokenizer.splitIntoTokens(newDoc2);
        assertEquals(13, tokens2.length);
        tokens2 = spanTypeTokenizer.splitIntoTokens(newDoc3);
        assertEquals(1, tokens2.length);
    }

    public void testSplitTokenizer() {

        System.out.println("Testing the SplitTokenizer");

        // Split tokens up by the '.' char.
        SplitTokenizer tokenizer = new SplitTokenizer("\\.");

        // Test splitting each sample as a string
        String[] tokens1 = tokenizer.splitIntoTokens(sampleDocs[0]);
        assertEquals(2, tokens1.length);
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[1]);
        assertEquals(2, tokens1.length);
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[2]);
        assertEquals(2, tokens1.length);
        tokens1 = tokenizer.splitIntoTokens(sampleDocs[3]);
        assertEquals(1, tokens1.length);

        // Then put each sample into a doc and test again
        Document doc = new Document("doc0", sampleDocs[0]);
        TextToken[] tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(2, tokens2.length);
        doc = new Document("doc1", sampleDocs[1]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(2, tokens2.length);
        doc = new Document("doc2", sampleDocs[2]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(2, tokens2.length);
        doc = new Document("doc3", sampleDocs[3]);
        tokens2 = tokenizer.splitIntoTokens(doc);
        assertEquals(1, tokens2.length);
    }

    /**
     * Creates a TestSuite from all testXXX methods
     * @return TestSuite
     */
    public static Test suite() { return new TestSuite(TokenizerTest.class); }

    /**
     * Run the full suite of tests with text output
     * @param args - unused
     */
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}
