package edu.cmu.minorthird.text.gui;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;

/**
 * Label top-level "document" spans in a TextBase.
 */

public class TextBaseLabeler extends TrackedTextBaseComponent{

	static final long serialVersionUID=200803014L;

// private SpanLabeler spanLabeler;

	public TextBaseLabeler(TextBase base,TextLabels viewLabels, // seen in viewer
			MutableTextLabels editLabels, // changed in editor
			StatusMessage statusMsg){
		super(base,viewLabels,editLabels,statusMsg);
		viewer=new TextBaseViewer(base,viewLabels,statusMsg);
		viewerTracker=
				new SpanLabeler(viewLabels,editLabels,viewer.getDocumentList(),viewer
						.getSpanPainter(),statusMsg);
		((SpanLabeler)viewerTracker).addViewer(viewer);
		viewer.getDocumentList().addListSelectionListener(viewerTracker);
		initializeLayout();
	}

	/** Pop up a frame for editing the labels. */
	public static void label(MutableTextLabels labels,File file){
		JFrame frame=new JFrame("TextBaseLabeler");
		TextBase base=labels.getTextBase();

		StatusMessage statusMsg=new StatusMessage();
		TextBaseLabeler labeler=new TextBaseLabeler(base,labels,labels,statusMsg);
		if(file!=null)
			labeler.setSaveAs(file);
		JComponent main=new StatusMessagePanel(labeler,statusMsg);
		frame.getContentPane().add(main,BorderLayout.CENTER);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args){
		if(args.length!=2){
			System.out.println("Usage: TextBaseLabeler <data> <labelfile>");
			return;
		}
		try{
			MutableTextLabels labels=
					(MutableTextLabels)FancyLoader.loadTextLabels(args[0]);
			label(labels,new File(args[1]));
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
