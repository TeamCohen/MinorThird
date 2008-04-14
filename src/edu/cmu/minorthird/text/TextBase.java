/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import java.util.Iterator;

/** Maintains information about what's in a set of documents.
 * Specifically, this contains a set of character sequences (TextToken's)
 * from some sort of set of containing documents - typically found by
 * tokenization.
 *
 * @author William Cohen
 * @author Quinten Mercer
 */

public interface TextBase {

    /** Returns the {@link edu.cmu.minorthird.text.Tokenizer} used on the documents in this text base. */
    Tokenizer getTokenizer();
    
    /** Returns the number of documents contained in this TextBase. */
    public int size();

    /** Returns the {@link Document} with the given ID */
    public Document getDocument(String docID);

    /** Returns an iterator over the documents in this TextBase. */
    public Iterator<Span> documentSpanIterator();
    
    /** Looks up the document Span for the given documentId.  Returns the Span or 
        null if a document with documentId was not found in this TextBase. */
    public Span documentSpan(String documentId);
}
