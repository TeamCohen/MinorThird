
package edu.cmu.minorthird.text;

import java.util.Iterator;


public abstract class AbstractTextBase implements TextBase {

    protected Tokenizer tokenizer;

    public AbstractTextBase(Tokenizer t) { tokenizer = t; }

    //
    // TextBase interface methods implemented
    //
    @Override
		public Tokenizer getTokenizer() { return tokenizer; }

    //
    // TextBase methods left abstract
    //
    @Override
		abstract public int size();
    @Override
		abstract public Document getDocument(String docID);
    @Override
		abstract public Iterator<Span> documentSpanIterator();
    @Override
		abstract public Span documentSpan(String documentId);    
}
