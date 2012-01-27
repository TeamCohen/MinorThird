/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import java.awt.GridBagLayout;

import javax.swing.JSplitPane;

/**
 * Two viewers, arranged side-by-side or top-and-bottom. 
 * 
 * @author William cohen
 */

abstract public class SplitViewer extends Viewer{
	
	static final long serialVersionUID=20081125L;

	protected JSplitPane splitPane;

	protected Viewer viewer1=null,viewer2=null;

	public SplitViewer(){
		super();
	}

	public SplitViewer(Viewer viewer1,Viewer viewer2){
		super();
		setSubViews(viewer1,viewer2);
	}

	public void setVertical(){
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
	}

	public void setHorizontal(){
		splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
	}

	/** Called at creation time. */
	@Override
	protected void initialize(){
		setLayout(new GridBagLayout());
		splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setResizeWeight(0.50);
		add(splitPane,fillerGBC());
	}

	public void setSubViews(Viewer viewer1,Viewer viewer2){
		this.viewer2=viewer2;
		this.viewer1=viewer1;
		if(splitPane.getOrientation()==JSplitPane.VERTICAL_SPLIT){
			splitPane.setTopComponent(viewer1);
			splitPane.setBottomComponent(viewer2);
			viewer1.setSuperView(this,"top");
			viewer2.setSuperView(this,"bottom");
		}else{
			splitPane.setLeftComponent(viewer1);
			splitPane.setRightComponent(viewer2);
			viewer1.setSuperView(this,"left");
			viewer2.setSuperView(this,"right");
		}
	}

	@Override
	public void clearContent(){
		if(viewer1!=null&&viewer2!=null){
			viewer1.clearContent();
			viewer2.clearContent();
		}
	}
}
