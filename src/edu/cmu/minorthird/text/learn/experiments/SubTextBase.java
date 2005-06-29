package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.BasicSpanLooper;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/** A read-only TextBase which is a subset of another TextBase.
 *
 *
 * @author William Cohen
*/

public class SubTextBase implements TextBase
{
	private Set validDocumentSpans;
	private TextBase base;

	public static class UnknownDocumentException extends Exception {
		public UnknownDocumentException(String s) { super(s); }
	}

	public SubTextBase(TextBase base,Iterator documentSpanIterator) throws UnknownDocumentException {
		this.base = base;
		validDocumentSpans = new TreeSet();
		while (documentSpanIterator.hasNext()) {
			Span span = (Span)documentSpanIterator.next(); 
			if (base.documentSpan( span.getDocumentId() )==null) {
				throw new UnknownDocumentException("documentId not in textBase: "+span.getDocumentId());
			}
			validDocumentSpans.add( span );
		}
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
		return base.splitIntoTokens(string);
	}

	public int size() { return validDocumentSpans.size(); }

	public Span.Looper documentSpanIterator() {
		return new BasicSpanLooper( validDocumentSpans );
	}

	public Span documentSpan(String documentId) {
		Span span = base.documentSpan(documentId);
		return validDocumentSpans.contains(span) ? span : null;
	}

	/** True if a span is contained by this TextBase */
	public boolean contains(Span span) {
		return validDocumentSpans.contains( span.documentSpan() );
	}

	public void setDocumentGroupId(String documentId, String groupId) {
		throw new UnsupportedOperationException("SubTextBase is read-only");
	}

    public TextBase retokenize(Tokenizer tok)
    {
	TextBase tb = new BasicTextBase();
	tb = base.retokenize(new Tokenizer());
	return tb;
    }

    /**Import Labels from another TextBase - as long as the current TextBase is some subset of the original */
    public TextLabels importLabels(MonotonicTextLabels origLabels, TextLabels parentLabels) {
	TextLabels childLabels = base.importLabels(origLabels, parentLabels);
	return childLabels;
    }

    public TextLabels importLabels(TextLabels parentLabels) 
    {
	TextLabels childLabels = base.importLabels(parentLabels);
	return childLabels;
    }

    public TextLabels importLabels(TextLabels parentLabels, String type) 
    {
	TextLabels childLabels = base.importLabels(parentLabels, type);
	return childLabels;
    }
}
