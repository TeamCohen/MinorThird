package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import java.awt.event.ActionEvent;
import java.io.File;

/** Tracks what's selected in the documentList of a TextBaseViewer.
 *
 * @author William Cohen
 */

abstract public class ViewerTracker extends JComponent implements ListSelectionListener
{
    static protected boolean viewEntireDocument = true;
    static protected final String DUMMY_ID = "**8dummy id***";

    // links to outside
    protected StatusMessage statusMsg;
    protected JList documentList;
    protected SpanPainter spanPainter;

    // internal state
    protected JTextPane editorPane;
    protected JScrollPane editorHolder;
    protected SpanDocument editedDoc;
    protected Span documentSpan;
    protected TextLabels viewLabels;
    protected MutableTextLabels editLabels;
    protected int contextWidth = 0;
    protected File saveAsFile = null;

    protected JButton upButton, downButton, saveButton;
    protected JSlider contextWidthSlider;

    /**
     * @param viewLabels a superset of editLabels which may include some additional read-only information
     * @param editLabels the labeling being modified (if there is one)
     * @param documentList the document Span being edited is associated with
     * the selected entry of the documentList.
     * @param spanPainter used to repaint documentList elements
     * @param statusMsg a JLabel used for status messages.
     */
    public ViewerTracker(
            TextLabels viewLabels,
            MutableTextLabels editLabels,
            JList documentList,
            SpanPainter spanPainter,
            StatusMessage statusMsg)
    {
        synchronized (documentList)
        {
            if (documentList.getModel().getSize() == 0)
            {
                throw new IllegalArgumentException("can't edit from empty list");
            }

            this.viewLabels = viewLabels;
            this.editLabels = editLabels;
            this.documentList = documentList;
            this.spanPainter = spanPainter;
            this.statusMsg = statusMsg;
            this.editorHolder = new JScrollPane();
            saveButton = new JButton(new SaveLabelsAction("Save"));
            upButton = new JButton(new MoveDocumentCursor("Up", -1));
            downButton = new JButton(new MoveDocumentCursor("Down", +1));
            contextWidthSlider = new ContextWidthSlider();
        }
    }

    protected Span nullSpan()
    {
        TextBase b = new BasicTextBase();
        b.loadDocument(DUMMY_ID, "[" + documentList.getModel().getSize() + " spans being viewed.\n"
                + " Select one to make it appear here.]");
        return b.documentSpanIterator().nextSpan();
    }


    /** If set, the viewer will show the entire document a span is in. */
    public void setViewEntireDocument(boolean flag)
    {
        viewEntireDocument = flag;
    }

    /** Activates the 'save' button, and indicates where to save. */
    public void setSaveAs(File file)
    {
        this.saveAsFile = file;
        System.out.println("saveAsFile -> " + saveAsFile);
        saveButton.setEnabled(saveAsFile != null);
    }

    /** change the text labels*/
    public void updateViewLabels(TextLabels newLabels)
    { this.viewLabels = newLabels; }

    /** implement ListSelectionListener, so can use this to listen to the documentList. */
    public void valueChanged(ListSelectionEvent e)
    {
        synchronized (documentList)
        {
            Span s = (Span) documentList.getSelectedValue();
            if (s != null)
                loadSpan(s);
            else
                loadSpan(nullSpan());
        }
    }

    /** Declare how much context to show on either size of the span. */
    protected void setContextWidth(int contextWidth)
    {
        synchronized (documentList)
        {
            this.contextWidth = contextWidth;
            Span s = (Span) documentList.getSelectedValue();
            if (s != null)
                loadSpan(s);
            else
                loadSpan(nullSpan());
        }
    }

    protected void loadSpan(Span span)
    {
        documentSpan = viewEntireDocument ? span.documentSpan() : span;
        editedDoc = new SpanDocument(documentSpan, contextWidth);
        editorPane = new JTextPane(editedDoc);
        Keymap keymap = editorPane.getKeymap(JTextComponent.DEFAULT_KEYMAP);
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("UP"), upButton.getAction());
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("control P"), upButton.getAction());
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("DOWN"), downButton.getAction());
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("control N"), downButton.getAction());
        editorPane.setKeymap(keymap);
        editorHolder.getViewport().setView(editorPane);
        editorHolder.repaint();
        statusMsg.display("editing " + documentSpan.getDocumentId()+" "+documentSpan);
        editorPane.requestFocus();
        // any subclass-dependent processing
        loadSpanHook();
    }

    /** Overload this to do something special when a new document is loaded. */
    protected void loadSpanHook()
    {
        ;
    }

    /** Useful routine */
    protected void importDocumentListMarkup(String documentId)
    {
        if (!DUMMY_ID.equals(documentSpan.getDocumentId()))
        {
            // import all current markup
            AttributeSet[] colors = new AttributeSet[SpanDifference.MAX_STATUS + 1];
            colors[SpanDifference.FALSE_POS] = spanPainter.fpColor();
            colors[SpanDifference.FALSE_NEG] = spanPainter.fnColor();
            colors[SpanDifference.TRUE_POS] = spanPainter.tpColor();
            colors[SpanDifference.UNKNOWN_POS] = spanPainter.mpColor();
            for (SpanDifference.Looper i = spanPainter.differenceIterator(documentId); i.hasNext();)
            {
                Span dspan = i.nextSpan();
                int status = i.getStatus();
                editedDoc.highlight(dspan, colors[status]);
            }
        }
    }

    protected class SaveLabelsAction extends AbstractAction
    {
        public SaveLabelsAction(String s)
        {
            super(s);
        }

        public void actionPerformed(ActionEvent event)
        {
            try
            {
                System.out.println("saving to file=" + saveAsFile);
                new TextLabelsLoader().saveTypesAsOps(editLabels, saveAsFile);
                statusMsg.display("Saved in " + saveAsFile.getName());
            }
            catch (Exception e)
            {
                statusMsg.display("Error: " + e);
            }
        }
    }

    protected class ContextWidthSlider extends JSlider
    {
        public ContextWidthSlider()
        {
            super(0, 10, 0);
            addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    ContextWidthSlider slider = (ContextWidthSlider) e.getSource();
                    if (!slider.getValueIsAdjusting())
                    {
                        int value = slider.getValue();
                        setContextWidth(value);
                    }
                }
            });
        }
    }

    /** Move through list of spans */
    protected class MoveDocumentCursor extends AbstractAction
    {
        private int delta;

        public MoveDocumentCursor(String msg, int delta)
        {
            super(msg);
            this.delta = delta;
        }

        public void actionPerformed(ActionEvent event)
        {
            synchronized (documentList)
            {
                int currentCursor = documentList.getSelectedIndex();
                // if nothing's selected, pretend it was the first thing
                if (currentCursor < 0) currentCursor = 0;
                int nextCursor = currentCursor + delta;
                if (nextCursor < documentList.getModel().getSize() && nextCursor >= 0)
                {
                    documentList.setSelectedIndex(nextCursor);
                }
            }
        }
    }

}

