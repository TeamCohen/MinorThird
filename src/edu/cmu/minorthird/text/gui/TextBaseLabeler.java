package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/** Label top-level "document" spans in a TextBase.
 */

public class TextBaseLabeler extends TrackedTextBaseComponent
{
//    private SpanLabeler spanLabeler;

    public TextBaseLabeler(
            TextBase base,
            TextLabels viewLabels, // seen in viewer
            MutableTextLabels editLabels, // changed in editor
            StatusMessage statusMsg)
    {
        super(base, viewLabels, editLabels, statusMsg);
        viewer = new TextBaseViewer(base, viewLabels, statusMsg);
        viewerTracker = new SpanLabeler(viewLabels, editLabels, viewer.getDocumentList(), viewer.getSpanPainter(), statusMsg);
        ((SpanLabeler) viewerTracker).addViewer(viewer);
        viewer.getDocumentList().addListSelectionListener(viewerTracker);
        initializeLayout();
    }

    /** Pop up a frame for editing the labels. */
    public static void label(MutableTextLabels labels, File file)
    {
        JFrame frame = new JFrame("TextBaseLabeler");
        TextBase base = labels.getTextBase();

        StatusMessage statusMsg = new StatusMessage();
        TextBaseLabeler labeler = new TextBaseLabeler(base, labels, labels, statusMsg);
        if (file != null) labeler.setSaveAs(file);
        JComponent main = new StatusMessagePanel(labeler, statusMsg);
        frame.getContentPane().add(main, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        try
        {

            TextBase base;
            MonotonicTextLabels guessLabels = null;
            MutableTextLabels truthLabels = null;
            File labelsFile = null;

            if (args.length == 0)
            {
                base = SampleTextBases.getTextBase();
                guessLabels = SampleTextBases.getTruthLabels();
                truthLabels = SampleTextBases.getTruthLabels();
            }
            else
            {
                base = new BasicTextBase();
                File f = new File(args[0]);
                if (f.isDirectory())
                {
                    guessLabels = truthLabels = TextBaseLoader.loadDirOfTaggedFiles(f);
                  base = guessLabels.getTextBase();
                }
                else
                {
                  base = TextBaseLoader.loadDocPerLine(f, false);
//                    baseLoader.loadLines(base, f);
                }
                if (args.length >= 2)
                {
                    labelsFile = new File(args[1]);
                    if (labelsFile.exists())
                    {
                        guessLabels = truthLabels = new TextLabelsLoader().loadSerialized(labelsFile, base);
                    }
                }
                if (guessLabels == null)
                {
                    guessLabels = truthLabels = new BasicTextLabels(base);
                }
            }

            TextBaseLabeler labeler = new TextBaseLabeler(base, guessLabels, truthLabels, new StatusMessage());
            labeler.setSaveAs(labelsFile);
            labeler.initializeLayout();

            labeler.buildFrame();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
