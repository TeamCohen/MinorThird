package edu.cmu.minorthird.text.gui;

import org.apache.log4j.Logger;

import javax.swing.text.*;

/** A StyledDocument that holds a single span.
 *
 * @author William Cohen
 */

public class SpanDocument extends DefaultStyledDocument
{
//    private edu.cmu.minorthird.text.Span span;
  private edu.cmu.minorthird.text.Span contextSpan;
  private int beginContextSpanInDocument, leftContextChars, rightContextChars;
  private edu.cmu.minorthird.text.Span leftBoundary, rightBoundary;
  private static Logger log = Logger.getLogger(SpanDocument.class);
//    private int contextWidth = 0;

  public SpanDocument()
  {
    ;
  }

  public SpanDocument(edu.cmu.minorthird.text.Span span)
  {
    this(span, 5);
  }

  public SpanDocument(edu.cmu.minorthird.text.Span span, int contextWidth)
  {

//        this.span = span;
//        this.contextWidth = contextWidth;
//    log.debug(span.asString());
    int conLo = Math.max(span.documentSpanStartIndex() - contextWidth, 0);
    int conHi = Math.min(span.documentSpanStartIndex() + span.size() + contextWidth, span.documentSpan().size());
    contextSpan = span.documentSpan().subSpan(conLo, conHi - conLo);

    beginContextSpanInDocument = contextSpan.getTextToken(0).getLo();
    leftContextChars = span.getTextToken(0).getLo() - beginContextSpanInDocument;
    rightContextChars =
        contextSpan.getTextToken(contextSpan.size() - 1).getHi() - span.getTextToken(span.size() - 1).getHi();

    leftBoundary = contextSpan.getLeftBoundary();
    rightBoundary = contextSpan.getRightBoundary();

    try
    {
      super.insertString(0, contextSpan.asString(), SimpleAttributeSet.EMPTY);
      resetHighlights();
    }
    catch (BadLocationException e)
    {
      throw new IllegalStateException("inserting: " + e);
    }
  }

  /** Overrides default insertString, since insertions are not allowed. */
  public void insertString(int off, String s, AttributeSet attribs) throws BadLocationException
  {
    ; // do nothing
  }

  /** Overrides default insertString, since insertions are not allowed. */
  public void remove(int off, int len) throws BadLocationException
  {
    ; // do nothing
  }

  /** Clear all highlights. */
  public void resetHighlights()
  {
    setCharacterAttributes(0, getLength(), SimpleAttributeSet.EMPTY, true);
    setCharacterAttributes(0, leftContextChars, HiliteColors.gray, true);
    setCharacterAttributes(getLength() - rightContextChars, rightContextChars, HiliteColors.gray, true);
  }

	/** Convert a character index in the text being displayed to a char index in the
	 * actual document which the display is a part of
	 */
	public int toLogicalCharIndex(int charIndex)
	{
		return charIndex + beginContextSpanInDocument;
	}

  /** Highlight a subspan of the current span. */
  public void highlight(edu.cmu.minorthird.text.Span subspan, AttributeSet attributeSet)
  {
    if (!subspan.getDocumentId().equals(contextSpan.getDocumentId())) return;
    if (subspan.size() == 0) return;
    if (subspan.getLeftBoundary().compareTo(leftBoundary) < 0) return;
    if (subspan.getRightBoundary().compareTo(rightBoundary) > 0) return;
    int beginSubspanInDocument = subspan.getTextToken(0).getLo();
    int subspanLength = subspan.getTextToken(subspan.size() - 1).getHi() - beginSubspanInDocument;
    setCharacterAttributes(beginSubspanInDocument - beginContextSpanInDocument, subspanLength, attributeSet, true);
  }
}
