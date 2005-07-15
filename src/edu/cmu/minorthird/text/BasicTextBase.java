package edu.cmu.minorthird.text;

import edu.cmu.minorthird.util.gui.*;

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

    public void loadDocument(Document document, TextToken[] tokenArray) {
	document.setTokens(tokenArray);
	documentMap.put(document.getId(), document);
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
	    //s.setCharOffset(getOffset(documentId));
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

    /**Retokenize the textBase creating psuedotokens for a certain spanType */
    public MonotonicTextLabels createPseudotokens(MonotonicTextLabels labels, String spanType) {
	BasicTextBase tb = new BasicTextBase();
	Span.Looper looper = labels.getTextBase().documentSpanIterator();
	ArrayList pseudotokenList = new ArrayList();
	while(looper.hasNext()) {
	    Span docSpan = looper.nextSpan();
	    String docId = docSpan.getDocumentId();
	    Document doc = labels.getTextBase().getDocument(docId);
	    String docString = doc.getText();
	    Span.Looper typeIterator = labels.instanceIterator(spanType, docId);
	    ArrayList docSplits = new ArrayList(); //List of string split at the pseudotokens
	    int docPos = 0;
	    while(typeIterator.hasNext()) {
		Span typeSpan = typeIterator.nextSpan();
		if(docPos != typeSpan.getTextToken(0).getLo()) {
		    Span before = docSpan.charIndexSubSpan(docPos, typeSpan.getTextToken(0).getLo());		
		    docSplits.add(new Pseudotoken(before, null));
		}
		docSplits.add(new Pseudotoken(typeSpan, spanType));		
		docPos = typeSpan.getTextToken(typeSpan.size() - 1).getHi();
	    }
	    Span after = docSpan.charIndexSubSpan(docPos, docSpan.getTextToken(docSpan.size() - 1).getHi());
	    docSplits.add(new Pseudotoken(after, null));
	    ArrayList tokenList = new ArrayList();
	    
	    int numToks = 0;
	    for(int i=0; i<docSplits.size(); i++) {
		Pseudotoken tok = (Pseudotoken)docSplits.get(i);
		if(tok.tokenValue == null) { //this split is not a pseudotoken
		    TextToken[] tokens = tokenizer.splitIntoTokens(doc, tok.text.asString(), tok.text.getTextToken(0).getLo());
		    for(int j=0; j<tokens.length; j++) {
			tokenList.add(tokens[j]);
		    }
		} else { //Split is a pseudotoken
		    TextToken ptoken = new TextToken(doc, tok.text.getTextToken(0).getLo(), 
						     tok.text.asString().length());
		    //System.out.println("Pseudo: " + ptoken.asString());
		    tokenList.add(ptoken);
		    pseudotokenList.add(ptoken);
		}
		numToks = tokenList.size()-1;
	    }
	    /*for(int x=0; x<tokenList.size(); x++) {
		TextToken token = (TextToken)tokenList.get(x);
		System.out.println(token.asString());
		}*/
	    TextToken[] tokenArray = (TextToken[])tokenList.toArray(new TextToken[0]);
	    tb.loadDocument(doc, tokenArray);	    
	}	
	MonotonicTextLabels newLabels = new BasicTextLabels(tb);	
	 
	for(int i=0; i<pseudotokenList.size(); i++) {
	    TextToken pseudotok = (TextToken)pseudotokenList.get(i);
	    newLabels.setProperty((Token)pseudotok, "pseudotoken", spanType);
	}

	newLabels = tb.importLabels(newLabels, labels);

	return newLabels;
    }

    public static class IllegalArgumentException extends Exception {
	public IllegalArgumentException(String s) { super(s); }
    }

    /** Import Labels from a TextBase with the same documents (such as a retokenized textBase */
    public MonotonicTextLabels importLabels(MonotonicTextLabels origLabels, TextLabels parentLabels){
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
    public MonotonicTextLabels importLabels(TextLabels parentLabels){
	//	if(!(parentLabels instanceof BasicTextLabels)) throw new IllegalArgumentException("Labels must be an instance of BasicTextLabels");
	MonotonicTextLabels childLabels = new BasicTextLabels(this);
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
    public TextLabels importLabels(MonotonicTextLabels origLabels, TextLabels parentLabels, String type, String newName){
	//	if(!(parentLabels instanceof BasicTextLabels)) throw new IllegalArgumentException("Labels must be an instance of BasicTextLabels");
	MonotonicTextLabels childLabels = origLabels;
	Span.Looper docIterator = documentSpanIterator();
	Set types = parentLabels.getTypes();     
	while(docIterator.hasNext()) {
	    Span docSpan = docIterator.nextSpan();
	    String docID = docSpan.getDocumentId();
	    Set spansWithType = parentLabels.getTypeSet(type, docID);
	    Iterator spanIterator = spansWithType.iterator();
	    while(spanIterator.hasNext()) {
		Span s = (Span)spanIterator.next();
		Span approxSpan = docSpan.charIndexSubSpan(s.getTextToken(0).getLo(), s.getTextToken(s.size() - 1).getHi());
		if(docSpan.contains(approxSpan)) {
		    childLabels.addToType(approxSpan, newName);
		}
	    }
	}	
	return childLabels;
    }

    private class Pseudotoken {
	public Span text;
	public String tokenValue;
	public Pseudotoken(Span text, String tokValue) {
	    this.text = text;
	    this.tokenValue = tokValue;
	}
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
