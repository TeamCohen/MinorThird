package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/** Interactively edit the contents of a TextBase and MutableTextLabels.
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
            setLabels(args);
        }
        catch (IOException e)
        {
            log.fatal(e, e);
        }
    }

    public TextBaseEditor(
            TextBase base,
            TextLabels viewLabels, // seen in viewer
            MutableTextLabels editLabels, // changed in editor
            StatusMessage statusMsg,
            boolean readOnly)
    {
//        super(base, viewLabels, editLabels, statusMsg);
        init(base, viewLabels, statusMsg, editLabels, readOnly);

    }

    private void init(TextBase base, TextLabels viewLabels, StatusMessage statusMsg, MutableTextLabels editLabels, boolean readOnly)
    {
        super.init(base, viewLabels,  editLabels, statusMsg);
        viewer = new TextBaseViewer(base, viewLabels, statusMsg);

        createSpanEditor(viewLabels, editLabels, statusMsg);
        spanEditor = (SpanEditor) viewerTracker;

        viewer.getTruthBox().addActionListener(
                new EditTypeAction(viewer.getGuessBox(), viewer.getTruthBox(), spanEditor));
        viewer.getGuessBox().addActionListener(
                new EditTypeAction(viewer.getGuessBox(), viewer.getTruthBox(), spanEditor));
        viewer.getDocumentList().addListSelectionListener(spanEditor);
        spanEditor.setReadOnly(readOnly);
        initializeLayout();
    }

    protected void createSpanEditor(TextLabels viewLabels, MutableTextLabels editLabels, StatusMessage statusMsg)
    {
        viewerTracker = new SpanEditor(viewLabels, editLabels, viewer.getDocumentList(), viewer.getSpanPainter(), statusMsg);
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

    /** Pop up a frame for editing the labels. */
    public static TextBaseEditor edit(MutableTextLabels labels, File file)
    {
        TextBase base = labels.getTextBase();

        StatusMessage statusMsg = new StatusMessage();
        TextBaseEditor editor = new TextBaseEditor(base, labels, labels, statusMsg, false);
        if (file != null) editor.setSaveAs(file);
        editor.initializeLayout();
        editor.buildFrame();

				return editor;
    }

    private void setLabels(String[] args) throws IOException
    {
        boolean readOnly = checkReadOnly(args);

        TextBase base = null;
        MutableTextLabels labels = null;
        File saveFile = null;

        if (args.length == 0)
        {
            base = SampleTextBases.getTextBase();
            labels = SampleTextBases.getTruthLabels();
            log.info("Sample Text Bases");
            //labels = edu.cmu.minorthird.text.ann.TestExtractionProblem.getLabels();
            //base = labels.getTextBase();
        }
        else
        {
            log.debug("load from " + args[0]);
            labels = (MutableTextLabels) new FancyLoader().loadTextLabels(args[0]);
            base = labels.getTextBase();
            if (args.length > 1)
            {
                saveFile = new File(args[1]);
                if (saveFile.exists()) labels = new TextLabelsLoader().loadOps(base, saveFile);
                log.info("load text bases");
            }
         }
        init(base, labels,  new StatusMessage(), labels, readOnly);
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
        }
        return readOnly;
    }

    /**
       Entry point that runs a gui to examine labels and
       change them.  
       @param args first argument is labels file and second is save file
     **/
	public static void main(String[] args)
	{
		try {
			MutableTextLabels labels = 
				(MutableTextLabels)FancyLoader.loadTextLabels(args[0]);
			File saveFile = new File(args[1]);
			TextBaseEditor.edit(labels, saveFile);
		} catch (Exception e) {
			System.out.println("usage repositoryKey outputFile");
		}
	} 
}
