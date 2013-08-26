package edu.cmu.minorthird.text.gui;

import javax.swing.text.AttributeSet;

/** Repaint a document.
 *
 * @author William Cohen
 */

/*package*/

interface SpanPainter
{
    /** Repaint the specified document.
     * Null means to repaint all documents. */
    public void paintDocument(String documentId);

    /** Find span differences associated with the document. */
    public edu.cmu.minorthird.text.SpanDifference.Looper differenceIterator(String documentId);

    /** Color for false positive spans. */
    public AttributeSet fpColor();

    /** Color for false negative spans. */
    public AttributeSet fnColor();

    /** Color for true positive spans. */
    public AttributeSet tpColor();

    /** Color for spans that might be positive */
    public AttributeSet mpColor();

}
