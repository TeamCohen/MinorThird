package edu.cmu.minorthird.text;


/** A series of of adjacent Token's from the same document.
 *
 * @author William Cohen
 */

public interface Span extends Comparable<Span>{

    /** Number of TextToken's in the span. */
    public int size();

    /** Access the i-th Token. */
    public Token getToken(int i);

    /** Access the i-th Token as a TextToken */
    public TextToken getTextToken(int i);

    /** Create a new Span containing TextToken's lo, lo+1, ..., lo+len. */ 
    public Span subSpan(int lo,int len);

    /** Create a new Span containing TextToken's which are as close as
     * possible to the character boundaries charLo, charHi of
     * thisspan. */
    public Span charIndexSubSpan(int charLo,int charHi);

    /** Create a the largest Span containing TextToken's which are
     * contained in the character boundaries charLo, charHi of this
     * span. */
    public Span charIndexProperSubSpan(int charLo,int charHi);

    /** Return the name of the document for these TextToken's. */
    public String getDocumentId();

    /** If this was a subspan, create a copy of its parent. */
    public Span documentSpan();

    /** Return the id of the group of documents this span belongs to */
    public String getDocumentGroupId();

    /** Return a string containing the entire content of the document
     * that this span lives in. 
     * <p>
     * <b>Note:</b> this differs subtly from documentSpan().asString(),
     * which returns the string between the beginning of the first
     * token and the end of the last token in the containing document. */
  
    public String getDocumentContents();

    /** Find the index of this span within its document.
     * In other words, a span can be copied with the code
     * <code>span.documentSpan().subSpan( span.documentSpanStartIndex(), span.length())</code>
     */
    public int documentSpanStartIndex();

    /** Find the string contained in a Span. */
    public String asString();
	
    /** A length-zero span for the left boundary */
    public Span getLeftBoundary();

    /** A length-zero span for the left boundary */
    public Span getRightBoundary();

    /** Check containment */
    public boolean contains(Span other);

    /** Check overlap */
    public boolean overlaps(Span other);

    /** Returns the low text token */
    public int getLoTextToken();

    /** Returns the Char index of where the span starts in the document */
    public int getLoChar();

    /** Returns the Char index of where the span ends in the document */
    public int getHiChar();

    public void setCharOffset(int charOffset);

    public int getCharOffset();

//    /** An iterator over Spans. */
//    public interface Looper extends Iterator {
//	public int estimatedSize();
//	public Span nextSpan();
//    }
    
}
