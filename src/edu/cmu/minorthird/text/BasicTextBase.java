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
*/

public class BasicTextBase implements TextBase, Serializable
{
  static private Logger log = Logger.getLogger(BasicTextBase.class);

  static private final long serialVersionUID = 1;
	private static final int CURRENT_VERSION_NUMBER = 1;

	//private final String defaultRegexPattern = "\\s*(\\w+|\\W)\\s*";
	private final String defaultRegexPattern = "\\s*([0-9]+|[a-zA-Z]+|\\W)\\s*";

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

	public void loadDocument(String documentId, String documentString)
	{ loadRegex(documentId, documentString, defaultRegexPattern); }

	/** Load all substrings of a string that match group 1 of a given regex pattern.
	 */
	protected void loadRegex(String documentId, String string, String regexPattern)
	{
		//create the document and add the tokens to that document

		Pattern pattern = Pattern.compile(regexPattern);
		Matcher matcher = pattern.matcher(string);

    Document document = new Document(documentId, string);

		List tokenList = new ArrayList();
		while (matcher.find())  {
            //index isn't used
//			int index = valueIndex( string.substring(matcher.start(1), matcher.end(1)) );
			tokenList.add( new TextToken(document, matcher.start(1), matcher.end(1)-matcher.start(1)) );
		}
		if (tokenList.size()==0) log.warn("empty document with id "+documentId);

		TextToken[] tokenArray = (TextToken[])tokenList.toArray(new TextToken[0]);
    document.setTokens(tokenArray);

		documentMap.put(documentId, document);
	}

	/** Tokenize a string. */
	public String[] splitIntoTokens(String string)
	{
		ArrayList list = new ArrayList();
		Pattern pattern = Pattern.compile(defaultRegexPattern);
		Matcher matcher = pattern.matcher(string);
		while (matcher.find()) {
			list.add( matcher.group(1) );
		}
		return (String[]) list.toArray( new String[list.size()] );
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
			return new BasicSpan(documentId,textTokens,0,textTokens.length,(String)documentGroupMap.get(documentId));
		}
		public int estimatedSize() { return documentMap.keySet().size(); }
	}

	private TextToken[] getTokenArray(String documentId)
	{
		Document document = (Document)documentMap.get(documentId);
		if (document!=null)
			return document.getTokens();
		else
			return null;
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
