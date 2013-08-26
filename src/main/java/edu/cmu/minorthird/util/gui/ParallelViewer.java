/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Conceptually this allows one to view several parallel aspects of a single
 * object. This is mapped to a JTabbedPane.
 * 
 * <p>
 * Note: when content is sent to a ParallelViewer, the viewer immediate forwards
 * the content only to the currently selected subview. This means that the
 * non-default subviews can be expensive to compute - no extra overhead will be
 * incurred unless the user actually selects these views. <b>However</b>,
 * before the ParallelViewer recieves content, it also checks that <b>every</b>
 * subview can receive the content, using that subview's canRecieve * method. So
 * if a subview's canRecieve method is expensive (e.g., if it simply calls
 * receive to see if there is an error, as the default implementation of
 * ComponentViewer does) then this extra overhead will be incurred.
 * 
 * @author William cohen
 */

public class ParallelViewer extends Viewer{
	
	static final long serialVersionUID=20080517L;

	private JTabbedPane parallelPane;

	private List<Viewer> subViewList;

	public ParallelViewer(){
		super();
	}

	/** Called at creation time. */
	@Override
	protected void initialize(){
		setLayout(new GridBagLayout());
		parallelPane=new JTabbedPane();
		subViewList=new ArrayList<Viewer>();
		add(parallelPane,fillerGBC());
		parallelPane.addChangeListener(new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent ev){
				// update the content of the currently selected view
				receiveContent(ParallelViewer.this.getContent());
			}
		});
	}

	/** Change default look of tabbed pane to put tabs on the left */
	public void putTabsOnLeft(){
		parallelPane.setTabPlacement(SwingConstants.LEFT);
		parallelPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	}

	/** Add a new way of viewing the content object. */
	public void addSubView(String title,Viewer view){
		view.setSuperView(this,title);
		subViewList.add(view);
		parallelPane.add(title,view);
	}

	@Override
	public void receiveContent(Object content){
		// just send content to the currently selected subview
		Viewer subView=subViewList.get(parallelPane.getSelectedIndex());
		subView.setContent(content);
	}

	/*
	 * override the default definition, to make sure the subView returned is
	 * current.
	 */
	@Override
	public Viewer getNamedSubView(String name){
		Viewer subviewer=super.getNamedSubView(name);
		subviewer.setContent(getContent());
		return subviewer;
	}

	@Override
	public boolean canReceive(Object content){
		for(Iterator<Viewer> i=subViewList.iterator();i.hasNext();){
			Viewer subView=i.next();
			if(!subView.canReceive(content))
				return false;
		}
		return true;
	}

	@Override
	public void clearContent(){
		for(Iterator<Viewer> i=subViewList.iterator();i.hasNext();){
			Viewer subView=i.next();
			subView.clearContent();
		}
	}

	@Override
	protected void handle(int signal,Object argument,List<Viewer> senders){
		throw new IllegalStateException("signal:"+signal+" argument:"+argument+
				" at:"+this);
	}

	@Override
	protected boolean canHandle(int signal,Object argument,List<Viewer> senders){
		return false;
	}

}
