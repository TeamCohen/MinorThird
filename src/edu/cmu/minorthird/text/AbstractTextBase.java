
package edu.cmu.minorthird.text;

import java.util.Iterator;


public abstract class AbstractTextBase implements TextBase {

    protected Tokenizer tokenizer;

    public AbstractTextBase(Tokenizer t) { tokenizer = t; }

    //
    // TextBase interface methods implemented
    //
    public Tokenizer getTokenizer() { return tokenizer; }

    //
    // TextBase methods left abstract
    //
    abstract public int size();
    abstract public Document getDocument(String docID);
    abstract public Iterator<Span> documentSpanIterator();
    abstract public Span documentSpan(String documentId);    
}
