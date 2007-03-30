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
public class SubTextBase extends AbstractTextBase
{
    private Set validDocumentSpans;
    private TextBase base;

    public static class UnknownDocumentException extends Exception {
        public UnknownDocumentException(String s) { super(s); }
    }

    public SubTextBase(TextBase base,Iterator documentSpanIterator) throws UnknownDocumentException {
        super(base.getTokenizer());
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


    /** True if a span is contained by this TextBase */
    public boolean contains(Span span) {
        return validDocumentSpans.contains( span.documentSpan() );
    }

    //
    // Implementations of abstract methods from AbstractTextBase
    public Tokenizer getTokenizer() { return base.getTokenizer(); }

    public Document getDocument(String documentId){
        return base.getDocument(documentId);
    }

    public int size() { return validDocumentSpans.size(); }

    public Span.Looper documentSpanIterator() {
        return new BasicSpanLooper( validDocumentSpans );
    }
    
    public Span documentSpan(String documentId) {
        Span span = base.documentSpan(documentId);
        return validDocumentSpans.contains(span) ? span : null;
    }

}
