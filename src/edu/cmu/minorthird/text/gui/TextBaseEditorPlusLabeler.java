package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/** Label top-level "document" spans in a TextBase.
 */

public class TextBaseEditorPlusLabeler extends TrackedTextBaseComponent
{
//    private SpanLabeler spanLabeler;

	ViewerTracker viewerTracker2;
    public TextBaseEditorPlusLabeler(
            TextBase base,
            TextLabels viewLabels, // seen in viewer
            MutableTextLabels editLabels, // changed in editor
            StatusMessage statusMsg)
    {
        super(base, viewLabels, editLabels, statusMsg);
        viewer = new TextBaseViewer(base, viewLabels, statusMsg);
        viewerTracker = 
					new SpanLabeler(viewLabels, editLabels, viewer.getDocumentList(), viewer.getSpanPainter(), statusMsg);
        ((SpanLabeler) viewerTracker).addViewer(viewer);
        viewerTracker2 = 
					new SpanEditor(viewLabels, editLabels, viewer.getDocumentList(), viewer.getSpanPainter(), statusMsg);
        //((SpanLabeler) viewerTracker).addViewer(viewer);
        viewer.getDocumentList().addListSelectionListener(viewerTracker);
        viewer.getDocumentList().addListSelectionListener(viewerTracker2);
        viewer.getTruthBox().addActionListener(
					new TextBaseEditor.EditTypeAction(viewer.getGuessBox(), viewer.getTruthBox(),(SpanEditor)viewerTracker2));
        viewer.getGuessBox().addActionListener(
					new TextBaseEditor.EditTypeAction(viewer.getGuessBox(), viewer.getTruthBox(),(SpanEditor)viewerTracker2));
        initializeTwoTrackerLayout();
    }
	  private void initializeTwoTrackerLayout()
	  {
        setPreferredSize(new Dimension(800, 600));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;

        viewer.setMinimumSize(new Dimension(200, 200));
				JTabbedPane tabbed = new JTabbedPane();
        viewerTracker.setMinimumSize(new Dimension(200, 50));
        viewerTracker2.setMinimumSize(new Dimension(200, 50));
				tabbed.add("Label parts",viewerTracker2);
				tabbed.add("Label whole",viewerTracker);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, viewer, tabbed);
        splitPane.setDividerLocation(400);
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(splitPane, gbc);
		}
    /** add a 'save' button */
    public void setSaveAs(File file)
    {
        viewerTracker.setSaveAs(file);
				viewerTracker2.setSaveAs(file);
    }


    /** Pop up a frame for editing the labels. */
    public static TextBaseEditorPlusLabeler editAndLabel(MutableTextLabels labels, File file)
    {
        JFrame frame = new JFrame("TextBaseEditorPlusLabeler");
        TextBase base = labels.getTextBase();

        StatusMessage statusMsg = new StatusMessage();
        TextBaseEditorPlusLabeler labeler = new TextBaseEditorPlusLabeler(base, labels, labels, statusMsg);
        if (file != null) labeler.setSaveAs(file);
        JComponent main = new StatusMessagePanel(labeler, statusMsg);
        frame.getContentPane().add(main, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
				return labeler;
    }

    public static void main(String[] args)
    {
			try {
				MutableTextLabels guessLabels = SampleTextBases.getTruthLabels();
				editAndLabel(guessLabels, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
}
