package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.BasicSpanLooper;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/** A read-only TextBase.
 *
 *
 * @author Cameron Williams
 */

abstract public class ImmutableTextBase implements TextBase
{

    public static class UnknownDocumentException extends Exception {
	public UnknownDocumentException(String s) { super(s); }
    }

    public void loadDocument(String documentId, String documentString, String regexPattern) {
	throw new UnsupportedOperationException("SubTextBase is read-only");
    }

    public void loadDocument(String documentId, String documentString, Tokenizer tokenizer) {
	throw new UnsupportedOperationException("SubTextBase is read-only");
    }

    public void loadDocument(String documentId, String string){
	throw new UnsupportedOperationException("SubTextBase is read-only");
    }

    public void loadDocument(String documentId, String string, int offset){
	throw new UnsupportedOperationException("SubTextBase is read-only");
    }

    public Document getDocument(String documentId){
	throw new UnsupportedOperationException("You cannot retrieve a document from SubTextBase");
    }

    public String[] splitIntoTokens(String string) {
	throw new UnsupportedOperationException("SubTextBase is read-only");
    }

    abstract public int size();

    abstract public Span.Looper documentSpanIterator();

    abstract public Span documentSpan(String documentId);

    /** True if a span is contained by this TextBase */
    abstract public boolean contains(Span span);

    public void setDocumentGroupId(String documentId, String groupId) {
	throw new UnsupportedOperationException("SubTextBase is read-only");
    }

    abstract public TextBase retokenize(Tokenizer tok);

    abstract public TextLabels importLabels(TextLabels parentLabels);
}