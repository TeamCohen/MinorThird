package edu.cmu.minorthird.text.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.MutableTextLabels;

/**
 * A TextBaseViewer augmented with a ViewerTracker component.
 * 
 * @author William Cohen
 */

public class TrackedTextBaseComponent extends JComponent{

	static final long serialVersionUID=20080314L;

	protected Logger log;

	protected edu.cmu.minorthird.text.TextBase base;

	protected edu.cmu.minorthird.text.TextLabels viewLabels;

	protected MutableTextLabels editLabels;

	protected StatusMessage statusMsg;

	protected TextBaseViewer viewer;

	protected ViewerTracker viewerTracker;

	// after Kevin's refactoring of components in this package,
	// initLayout was sometimes called zero times, sometimes once.
	// after William's fixes, this went to once or twice.
	// this flag keeps initLayout code from happening more than once.
	private boolean laidOut=false;

	protected TrackedTextBaseComponent(){
		log=Logger.getLogger(this.getClass().getName());
	}

	public TrackedTextBaseComponent(edu.cmu.minorthird.text.TextBase base,
			edu.cmu.minorthird.text.TextLabels viewLabels,
			MutableTextLabels editLabels,StatusMessage statusMsg){
		init(base,viewLabels,editLabels,statusMsg);
	}

	protected void init(edu.cmu.minorthird.text.TextBase base,
			edu.cmu.minorthird.text.TextLabels viewLabels,
			MutableTextLabels editLabels,StatusMessage statusMsg){
		log=Logger.getLogger(this.getClass().getName());
		this.base=base;
		this.viewLabels=viewLabels;
		this.editLabels=editLabels;
		this.statusMsg=statusMsg;
	}

	public TextBaseViewer getViewer(){
		return viewer;
	}

	public ViewerTracker getViewerTracker(){
		return viewerTracker;
	}

	/**
	 * Layout stuff - assumes that viewer and viewerTracker are already created.
	 */
	protected void initializeLayout(){
		if(laidOut)
			return;

		setPreferredSize(new Dimension(800,600));
		setLayout(new GridBagLayout());
		GridBagConstraints gbc;

		viewer.setMinimumSize(new Dimension(200,200));
		viewerTracker.setMinimumSize(new Dimension(200,50));
		JSplitPane splitPane=
				new JSplitPane(JSplitPane.VERTICAL_SPLIT,viewer,viewerTracker);
		splitPane.setDividerLocation(400);
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		gbc.weightx=1.0;
		gbc.weighty=1.0;
		gbc.gridx=1;
		gbc.gridy=1;
		add(splitPane,gbc);

		laidOut=true;
	}

	/** change the text labels */
	public void updateTextLabels(edu.cmu.minorthird.text.TextLabels newLabels){
		this.viewLabels=newLabels;
		viewer.updateTextLabels(newLabels);
		viewerTracker.updateViewLabels(newLabels);
	}

	/** add a 'save' button */
	public void setSaveAs(File file){
		viewerTracker.setSaveAs(file);
	}

	protected void buildFrame(){
		JComponent main=new StatusMessagePanel(this,this.statusMsg);

		JFrame frame=new JFrame(this.getClass().getName());
		frame.getContentPane().add(main,BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter(){
			// public void windowClosing(WindowEvent e)
			// {
			// System.exit(0);
			// }
		});
		frame.pack();
		frame.setVisible(true);
	}
}
