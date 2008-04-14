package edu.cmu.minorthird.text;

import java.util.Iterator;

/**
 *
 *
 * @author Quinten Mercer
 */

public abstract class MutableTextBase extends AbstractTextBase {

    public MutableTextBase(Tokenizer t) { super(t); }

    /** Creates a new document for the document contained in documentString and referenced by documentId. <br>
     *  Tokenizes documentString using the Tokenizer set for this TextBase.  <br>
     *  New document is stored in the TextBase.
     */
    public abstract void loadDocument(String documentId, String text);    

    /** Creates a new document for the document contained in documentString and referenced by documentId. <br>
     *  Also, sets the char offset to indicate that this document is a subdocument of another. <br>
     *  Tokenizes documentString using the Tokenizer set for this TextBase.  <br>
     *  New document is stored in the TextBase.
     */
    public abstract void loadDocument(String documentId, String documentString, int charOffset);

    /** Sets the group that a document belongs to */
    public abstract void setDocumentGroupId(String documentId,String documentGroupId);

    //
    // TextBase interface methods
    //
    public abstract int size();
    public abstract Document getDocument(String docID);
    public abstract Iterator<Span> documentSpanIterator();
    public abstract Span documentSpan(String documentId);
}
