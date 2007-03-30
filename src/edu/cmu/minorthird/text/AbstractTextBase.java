
package edu.cmu.minorthird.text;


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
    abstract public Span.Looper documentSpanIterator();
    abstract public Span documentSpan(String documentId);    
}
