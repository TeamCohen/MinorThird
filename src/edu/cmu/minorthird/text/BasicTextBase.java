package edu.cmu.minorthird.text;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Maintains information about what's in a set of documents.
 * Specifically, this contains a set of character sequences (TextToken's)
 * from some sort of set of containing documents - typically found by
 * tokenization.
 *
 * @author William Cohen
 * @author Cameron Williams
 */

public class BasicTextBase implements TextBase, Serializable
{
    static private Logger log = Logger.getLogger(BasicTextBase.class);

    static private final long serialVersionUID = 1;
    private static final int CURRENT_VERSION_NUMBER = 1;

    //private final String defaultRegexPattern = "\\s*(\\w+|\\W)\\s*";
    //private Tokenizer tokenizer = new Tokenizer(Tokenizer.SPLIT,"\n");
    private Tokenizer tokenizer = new Tokenizer();

    private Map documentMap = new HashMap();

    // map documentId to name of 'group' of documents it belongs to
    private Map documentGroupMap = new TreeMap();

    // map token value to index
    private Map indexMap = new TreeMap();
    // next allocatable index
    private int nextIndex = 0;

    public BasicTextBase() { ; }

    public void loadDocument(String documentId, String documentString, String regexPattern)
    { loadRegex(documentId, documentString, regexPattern); }

    public void loadDocument(String documentId, String documentString, Tokenizer tok)
    {
	this.tokenizer = tok;
	String regexPattern = tokenizer.regexPattern;
	loadRegex(documentId, documentString, regexPattern); 
    }

    public void loadDocument(String documentId, String documentString)
    {
	String regexPattern = tokenizer.regexPattern;
	loadRegex(documentId, documentString, regexPattern); 
    }

    public void loadDocument(String documentId, String documentString, int charOffset)
    {
	String regexPattern = tokenizer.regexPattern;
	loadRegex(documentId, documentString, charOffset, regexPattern); 
    }

    /** Load all substrings of a string that match group 1 of a given regex pattern.
     */
    protected void loadRegex(String documentId, String string, String regexPattern)
    {
	//create the document and add the tokens to that document
	
	tokenizer.regexPattern = regexPattern;
	Document document = new Document(documentId, string);
	
	TextToken[] tokenArray = tokenizer.splitIntoTokens(document, string);
	
	document.setTokens(tokenArray);

	documentMap.put(documentId, document);
    }

    /** Load all substrings of a string that match group 1 of a given regex pattern.
     */
    protected void loadRegex(String documentId, String string, int charOffset, String regexPattern)
    {
	//create the document and add the tokens to that document
	
	tokenizer.regexPattern = regexPattern;
	Document document = new Document(documentId, string, charOffset);
	
	TextToken[] tokenArray = tokenizer.splitIntoTokens(document, string);
	
	document.setTokens(tokenArray);

	documentMap.put(documentId, document);
    }

    public Document getDocument(String docID) {
	Document doc = (Document)documentMap.get(docID);
	return doc;
    }

    /** Tokenize a string. */
    public String[] splitIntoTokens(String string)
    {
	String[] stringArray = tokenizer.splitIntoTokens(string);
	return stringArray;
	
    }

    /** The number of documents in the tesst base. */
    public int size()
    { return documentMap.size(); }

    /** An iterator over all documents.
     */
    public Span.Looper documentSpanIterator()
    {	
	return new MyDocumentSpanLooper();	
    }

    /** Find the document span for the given id */
    public Span documentSpan(String documentId)
    {
	TextToken[] textTokens = getTokenArray(documentId);
	if (textTokens==null) return null;
	else return new BasicSpan(documentId,textTokens,0,textTokens.length,(String)documentGroupMap.get(documentId));
    }

    public void setDocumentGroupId(String documentId,String documentGroupId)
    {	documentGroupMap.put(documentId,documentGroupId); }

    private class MyDocumentSpanLooper implements Span.Looper
    {
	private Iterator k = documentMap.keySet().iterator();
	public MyDocumentSpanLooper() {;}
	public void remove() { throw new UnsupportedOperationException("not implemented"); }
	public boolean hasNext() { return k.hasNext(); }
	public Span nextSpan() { return (Span)next(); }
	public Object next() {
	    String documentId = (String)k.next();
	    TextToken[] textTokens = getTokenArray(documentId);
	    Span s = new BasicSpan(documentId,textTokens,0,textTokens.length,(String)documentGroupMap.get(documentId));
	    int offset = getOffset(documentId);
	    //System.out.println("The Offset is: " + offset);
	    s.setCharOffset(getOffset(documentId));
	    return s;
	}
	public int estimatedSize() { return documentMap.keySet().size(); }
    }

    private int getOffset(String documentId) {
	Document document = (Document)documentMap.get(documentId);
	if (document!=null)
	    return document.charOffset;
	else
	    return -1;
    }
    
    private TextToken[] getTokenArray(String documentId)
    {
	Document document = (Document)documentMap.get(documentId);
	if (document!=null)
	    return document.getTokens();
	else
	    return null;
    }

    /** Separate the tokens in a text Base differently */
    public TextBase retokenize(Tokenizer tok)
    {
	TextBase tb = new BasicTextBase();
	Object[] docs = documentMap.values().toArray();
	for(int i=0; i<docs.length; i++) {
	    tb.loadDocument(((Document)docs[i]).getId(),((Document)docs[i]).getText(),tok);
	}
	return tb;
    }

    public static class IllegalArgumentException extends Exception {
	public IllegalArgumentException(String s) { super(s); }
    }

    /** Import Labels from a TextBase with the same documents (such as a retokenized textBase */
    public TextLabels importLabels(MonotonicTextLabels origLabels, TextLabels parentLabels){
	//	if(!(parentLabels instanceof BasicTextLabels)) throw new IllegalArgumentException("Labels must be an instance of BasicTextLabels");
	MonotonicTextLabels childLabels = origLabels;
	Span.Looper docIterator = documentSpanIterator();
	Set types = parentLabels.getTypes();     
	while(docIterator.hasNext()) {
	    Span docSpan = docIterator.nextSpan();
	    String docID = docSpan.getDocumentId();
	    Iterator typeIterator = types.iterator();
	    while(typeIterator.hasNext()) {
		String type = (String)typeIterator.next();
		// bug: can't cast to BasicTextLabels!
		Set spansWithType = parentLabels.getTypeSet(type, docID);
		Iterator spanIterator = spansWithType.iterator();
		while(spanIterator.hasNext()) {
		    Span s = (Span)spanIterator.next();
		    Span approxSpan = docSpan.charIndexSubSpan(s.getTextToken(0).getLo(), s.getTextToken(s.size() - 1).getHi());
		    if(docSpan.contains(approxSpan)) {
			childLabels.addToType(approxSpan, type);
		    }
		}
	    }
	}
	return childLabels;
    }

    /** Import Labels from a TextBase with the same documents (such as a retokenized textBase */
    public TextLabels importLabels(TextLabels parentLabels){
	//	if(!(parentLabels instanceof BasicTextLabels)) throw new IllegalArgumentException("Labels must be an instance of BasicTextLabels");
	MutableTextLabels childLabels = new BasicTextLabels(this);
	Span.Looper docIterator = documentSpanIterator();
	Set types = parentLabels.getTypes();     
	while(docIterator.hasNext()) {
	    Span docSpan = docIterator.nextSpan();
	    String docID = docSpan.getDocumentId();
	    Iterator typeIterator = types.iterator();
	    while(typeIterator.hasNext()) {
		String type = (String)typeIterator.next();
		// bug: can't cast to BasicTextLabels!
		Set spansWithType = parentLabels.getTypeSet(type, docID);
		Iterator spanIterator = spansWithType.iterator();
		while(spanIterator.hasNext()) {
		    Span s = (Span)spanIterator.next();
		    Span approxSpan = docSpan.charIndexSubSpan(s.getTextToken(0).getLo(), s.getTextToken(s.size() - 1).getHi());
		    if(docSpan.contains(approxSpan)) {
			childLabels.addToType(approxSpan, type);
		    }
		}
	    }
	}
	return childLabels;
    }

    /** Import Labels of type  from a TextBase with the same documents (such as a retokenized textBase */
    public TextLabels importLabels(TextLabels parentLabels, String type){
	//	if(!(parentLabels instanceof BasicTextLabels)) throw new IllegalArgumentException("Labels must be an instance of BasicTextLabels");
	MutableTextLabels childLabels = new BasicTextLabels(this);
	Span.Looper docIterator = documentSpanIterator();
	Set types = parentLabels.getTypes();     
	while(docIterator.hasNext()) {
	    Span docSpan = docIterator.nextSpan();
	    String docID = docSpan.getDocumentId();
	    Set spansWithType = parentLabels.getTypeSet(type, docID);
	    Iterator spanIterator = spansWithType.iterator();
	    while(spanIterator.hasNext()) {
		Span s = (Span)spanIterator.next();
		Span approxSpan = docSpan.charIndexSubSpan(s.getLoChar(), s.getHiChar());
		if(docSpan.contains(approxSpan)) {
		    childLabels.addToType(approxSpan, type);
		}
	    }
	}	
	return childLabels;
    }

    //
    // test routine
    //

    static public void main(String[] args) {
	TextBase b = new BasicTextBase();
	for (int i=0; i<args.length; i++) {
	    b.loadDocument("arg_"+i, args[i]);
	}
	for (Iterator i=b.documentSpanIterator(); i.hasNext(); ) {
	    System.out.println(i.next());
	}
    }
}
