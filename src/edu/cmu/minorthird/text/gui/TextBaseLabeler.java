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
            TextEnv viewEnv, // seen in viewer
            MutableTextEnv editEnv, // changed in editor
            StatusMessage statusMsg)
    {
        super(base, viewEnv, editEnv, statusMsg);
        viewer = new TextBaseViewer(base, viewEnv, statusMsg);
        viewerTracker = new SpanLabeler(viewEnv, editEnv, viewer.getDocumentList(), viewer.getSpanPainter(), statusMsg);
        ((SpanLabeler) viewerTracker).addViewer(viewer);
        viewer.getDocumentList().addListSelectionListener(viewerTracker);
        initializeLayout();
    }

    /** Pop up a frame for editing the environment. */
    public static void label(MutableTextEnv env, File file)
    {
        JFrame frame = new JFrame("TextBaseLabeler");
        TextBase base = env.getTextBase();

        StatusMessage statusMsg = new StatusMessage();
        TextBaseLabeler labeler = new TextBaseLabeler(base, env, env, statusMsg);
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
            MonotonicTextEnv guessEnv = null;
            MutableTextEnv truthEnv = null;
            File envFile = null;

            if (args.length == 0)
            {
                base = SampleTextBases.getTextBase();
                guessEnv = SampleTextBases.getTruthEnv();
                truthEnv = SampleTextBases.getTruthEnv();
                //env = edu.cmu.minorthird.text.ann.TestExtractionProblem.getEnv();
                //base = env.getTextBase();
            }
            else
            {
                TextBaseLoader baseLoader = new TextBaseLoader();
                baseLoader.setFirstWordIsDocumentId(true);
                base = new BasicTextBase();
                File f = new File(args[0]);
                if (f.isDirectory())
                {
                    baseLoader.loadTaggedFiles(base, f);
                    guessEnv = truthEnv = baseLoader.getFileMarkup();
                }
                else
                {
                    baseLoader.loadLines(base, f);
                }
                if (args.length >= 2)
                {
                    envFile = new File(args[1]);
                    if (envFile.exists())
                    {
                        guessEnv = truthEnv = new TextEnvLoader().loadSerialized(envFile, base);
                    }
                }
                if (guessEnv == null)
                {
                    guessEnv = truthEnv = new BasicTextEnv(base);
                }
            }

            TextBaseLabeler labeler = new TextBaseLabeler(base, guessEnv, truthEnv, new StatusMessage());
            labeler.setSaveAs(envFile);
            labeler.initializeLayout();

            labeler.buildFrame();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
