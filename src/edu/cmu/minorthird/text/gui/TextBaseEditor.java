package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/** Interactively edit the contents of a TextBase and MutableTextEnv.
 *
 * @author William Cohen
 */

public class TextBaseEditor extends TrackedTextBaseComponent
{
    private SpanEditor spanEditor;

    public SpanEditor getSpanEditor()
    {
        return spanEditor;
    }

    protected TextBaseEditor(String[] args)
    {
        super();
        log.debug("construct");
        try
        {
            setEnvironment(args);
        }
        catch (IOException e)
        {
            log.fatal(e, e);
        }
    }

    public TextBaseEditor(
            TextBase base,
            TextEnv viewEnv, // seen in viewer
            MutableTextEnv editEnv, // changed in editor
            StatusMessage statusMsg,
            boolean readOnly)
    {
//        super(base, viewEnv, editEnv, statusMsg);
        init(base, viewEnv, statusMsg, editEnv, readOnly);

    }

    private void init(TextBase base, TextEnv viewEnv, StatusMessage statusMsg, MutableTextEnv editEnv, boolean readOnly)
    {
        super.init(base, viewEnv,  editEnv, statusMsg);
        viewer = new TextBaseViewer(base, viewEnv, statusMsg);

        createSpanEditor(viewEnv, editEnv, statusMsg);
        spanEditor = (SpanEditor) viewerTracker;

        viewer.getTruthBox().addActionListener(
                new EditTypeAction(viewer.getGuessBox(), viewer.getTruthBox(), spanEditor));
        viewer.getGuessBox().addActionListener(
                new EditTypeAction(viewer.getGuessBox(), viewer.getTruthBox(), spanEditor));
        viewer.getDocumentList().addListSelectionListener(spanEditor);
        spanEditor.setReadOnly(readOnly);
        initializeLayout();
    }

    protected void createSpanEditor(TextEnv viewEnv, MutableTextEnv editEnv, StatusMessage statusMsg)
    {
        viewerTracker = new SpanEditor(viewEnv, editEnv, viewer.getDocumentList(), viewer.getSpanPainter(), statusMsg);
    }

    /** Change the type of span being edited. */
	  public static class EditTypeAction extends AbstractAction
    {
        private JComboBox guessBox, truthBox;
        private SpanEditor spanEditor;

        public EditTypeAction(JComboBox guessBox, JComboBox truthBox, SpanEditor spanEditor)
        {
            this.guessBox = guessBox;
            this.truthBox = truthBox;
            this.spanEditor = spanEditor;
        }

        public void actionPerformed(ActionEvent event)
        {
            String truthType = (String) truthBox.getSelectedItem();
            String guessType = (String) guessBox.getSelectedItem();
            if (!TextBaseViewer.NULL_TRUTH_ENTRY.equals(truthType))
                spanEditor.setTypesBeingEdited(guessType, truthType);
            else
                spanEditor.setTypesBeingEdited(guessType, guessType);
        }
    }

    /** Pop up a frame for editing the environment. */
    public static TextBaseEditor edit(MutableTextEnv env, File file)
    {
//        JFrame frame = new JFrame("TextBaseEditor");
        TextBase base = env.getTextBase();

        StatusMessage statusMsg = new StatusMessage();
        TextBaseEditor editor = new TextBaseEditor(base, env, env, statusMsg, false);
        if (file != null) editor.setSaveAs(file);
        editor.initializeLayout();
        editor.buildFrame();

				return editor;
    }

    public static void main(String[] args)
    {
        // parse options
        try
        {
					  TextBaseEditor editor = new TextBaseEditor(args);
					  editor.initializeLayout();
					  editor.buildFrame();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (Error e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void setEnvironment(String[] args) throws IOException
    {
        boolean readOnly = checkReadOnly(args);

        TextBase base = null;
        MutableTextEnv env = null;
        File saveFile = null;

        if (args.length == 0)
        {
            base = SampleTextBases.getTextBase();
            env = SampleTextBases.getTruthEnv();
            log.info("Sample Text Bases");
            //env = edu.cmu.minorthird.text.ann.TestExtractionProblem.getEnv();
            //base = env.getTextBase();
        }
        else
        {
            log.debug("load from " + args[0]);
            env = (MutableTextEnv) new FancyLoader().loadTextEnv(args[0]);
            base = env.getTextBase();
            if (args.length > 1)
            {
                saveFile = new File(args[1]);
                if (saveFile.exists()) env = new TextEnvLoader().loadOps(base, saveFile);
                log.info("load text bases");
            }
         }
        init(base, env,  new StatusMessage(), env, readOnly);
        this.setSaveAs(saveFile);

    }

    private static boolean checkReadOnly(String[] args)
    {
        boolean readOnly = false;
//        int argp = 0;
        for (int argp = 0; argp < args.length; argp++)
        {
            if ("-readOnly".equals(args[argp]))
            {
                readOnly = true;
                argp++;
            }
/*            else
            {
                System.out.println("illegal option " + args[argp]);
                argp++;
            }
*/
        }
        return readOnly;
    }
}
