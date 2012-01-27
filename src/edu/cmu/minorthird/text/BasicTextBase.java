package edu.cmu.minorthird.text;

import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * Maintains information about what's in a set of documents. Specifically, this
 * contains a set of character sequences (TextToken's) from some sort of set of
 * containing documents - typically found by tokenization.
 * 
 * @author William Cohen
 * @author Cameron Williams
 * @author Quinten Mercer
 */

public class BasicTextBase extends MutableTextBase implements Serializable{

	// Minorthird administrative stuff
	static Logger log=Logger.getLogger(BasicTextBase.class);

	static private final long serialVersionUID=20080202L;

	// Underlying document store.
	private SortedMap<String,Document> documentMap=new TreeMap<String,Document>();

	// map documentId to name of 'group' of documents it belongs to
	private SortedMap<String,String> documentGroupMap=
			new TreeMap<String,String>();

	/** Default constructor creates a new TextBase with the default Tokenizer. */
	public BasicTextBase(){
		super(new RegexTokenizer());
	}

	/**
	 * Constructor that specifies a custom Tokenizer to be used with this
	 * TextBase.
	 */
	public BasicTextBase(Tokenizer t){
		super(t);
	}

	//
	// Implementations of MutableTextBase abstract methods
	//
	/**
	 * Adds a document to this TextBase with documentId as its identifier and with
	 * text specified by documentString.
	 */
	@Override
	public void loadDocument(String documentId,String documentString){
		// create the document and add the tokens to that document
		Document document=new Document(documentId,documentString);
		TextToken[] tokenArray=getTokenizer().splitIntoTokens(document);
		document.setTokens(tokenArray);
		documentMap.put(documentId,document);
	}

	/**
	 * Adds a document to this TextBase with documentId as its identifier and with
	 * text specified by documentString. Also, this method sets the offset
	 * parameter in the new Document to the specified charOffset.
	 */
	@Override
	public void loadDocument(String documentId,String documentString,
			int charOffset){
		// create the document and add the tokens to that document
		Document document=new Document(documentId,documentString,charOffset);
		TextToken[] tokenArray=getTokenizer().splitIntoTokens(document);
		document.setTokens(tokenArray);
		documentMap.put(documentId,document);
	}

	/**
	 * Sets the document group id for the specified documentId to the specified
	 * document group id.
	 */
	@Override
	public void setDocumentGroupId(String documentId,String documentGroupId){
		documentGroupMap.put(documentId,documentGroupId);
	}

	/** Returns the number of documents currently in this TextBase. */
	@Override
	public int size(){
		return documentMap.size();
	}

	/**
	 * Returns the Document instance that corresponds to the specified documentId
	 * or null if no document exists with the specified documentId.
	 */
	@Override
	public Document getDocument(String documentId){
		return documentMap.get(documentId);
	}

	/**
	 * Returns a Span instance that encloses all of the tokens in the document
	 * specified by documentId. Note that this Span instance will NOT include any
	 * white space that comes before the first token or after the last token.
	 */
	@Override
	public Span documentSpan(String documentId){
		TextToken[] textTokens=getTokenArray(documentId);
		if(textTokens==null)
			return null;
		else
			return new BasicSpan(documentId,textTokens,0,textTokens.length,
					documentGroupMap.get(documentId));
	}

	/**
	 * Returns a Span.Looper instance that includes a document span for every
	 * document in this TextBase.
	 */
	@Override
	public Iterator<Span> documentSpanIterator(){
		return new MyDocumentSpanIterator();
	}

	/** Helper class that is used to iterate through document spans. */
	private class MyDocumentSpanIterator implements Iterator<Span>{

		private Iterator<String> k=documentMap.keySet().iterator();

		@Override
		public void remove(){
			throw new UnsupportedOperationException(
					"Cannot remove documents from a TextBase.");
		}

		@Override
		public boolean hasNext(){
			return k.hasNext();
		}

		@Override
		public Span next(){
			String documentId=k.next();
			TextToken[] textTokens=getTokenArray(documentId);
			Span s=new BasicSpan(documentId,textTokens,0,textTokens.length,documentGroupMap.get(documentId));
			s.setCharOffset(getOffset(documentId));
			return s;
		}

//		public int estimatedSize(){
//			return documentMap.keySet().size();
//		}
	}

	private int getOffset(String documentId){
		Document document=documentMap.get(documentId);
		if(document!=null)
			return document.charOffset;
		else
			return -1;
	}

	/**
	 * Helper method used internally to make getting at the token array for a
	 * specific document id easier.
	 */
	private TextToken[] getTokenArray(String documentId){
		Document document=documentMap.get(documentId);
		if(document!=null)
			return document.getTokens();
		return null;
	}

	//
	// basic test routine that loads each argument as a document, then iterates
	// through them printing them out.
	//    
	static public void main(String[] args){
		BasicTextBase b=new BasicTextBase();
		for(int i=0;i<args.length;i++){
			b.loadDocument("arg_"+i,args[i]);
		}
		for(Iterator<Span> i=b.documentSpanIterator();i.hasNext();){
			System.out.println(i.next());
		}
	}
}
