/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Conceptually this allows one to view several parallel aspects
 * of a single object.  This is mapped to a JTabbedPane.
 *
 * <p>Note: when content is sent to a ParallelViewer, the viewer
 * immediate forwards the content only to the currently selected
 * subview. This means that the non-default subviews can be expensive
 * to compute - no extra overhead will be incurred unless the user
 * actually selects these views.  <b>However</b>, before the
 * ParallelViewer recieves content, it also checks that <b>every</b>
 * subview can receive the content, using that subview's canRecieve *
 * method.  So if a subview's canRecieve method is expensive (e.g., if
 * it simply calls receive to see if there is an error, as the default
 * implementation of ComponentViewer does) then this extra overhead
 * will be incurred.
 * 
 * @author William cohen
 */

public class ParallelViewer extends Viewer
{
	static private Logger log = Logger.getLogger(ParallelViewer.class);

	private JTabbedPane parallelPane;
	private ArrayList subViewList;

	public ParallelViewer()
	{
		super();
	}

	/** Called at creation time. */
	protected void initialize() 
	{
		setLayout(new GridBagLayout());
		parallelPane = new JTabbedPane();
		subViewList = new ArrayList();
		add( parallelPane, fillerGBC() );
		parallelPane.addChangeListener( new ChangeListener() {
				public void stateChanged(ChangeEvent ev) {
					// update the content of the currently selected view
					receiveContent( ParallelViewer.this.getContent() );
				}
			});
	}
	
	/** Add a new way of viewing the content object. */
	public void addSubView(String title,Viewer view)
	{
		view.setSuperView(this,title);
		subViewList.add(view);
		parallelPane.add(title,view);
	}

	public void receiveContent(Object content)
	{
		// just send content to the currently selected subview
		Viewer subView = (Viewer)subViewList.get( parallelPane.getSelectedIndex() );
		subView.setContent(content);
	}

	/* override the default definition, to make sure the subView
		 returned is current. */
	public Viewer getNamedSubView(String name) 
	{ 
		Viewer subviewer = super.getNamedSubView(name);
		subviewer.setContent( getContent() );
		return subviewer;
	}


	public boolean canReceive(Object content)
	{
		for (Iterator i=subViewList.iterator(); i.hasNext(); ) {
			Viewer subView = (Viewer)i.next();
			if (!subView.canReceive(content))  return false;
		}
		return true;
	}

	public void clearContent()
	{
		for (Iterator i=subViewList.iterator(); i.hasNext(); ) {
			Viewer subView = (Viewer)i.next();
			subView.clearContent();
		}
	}


	protected void handle(int signal,Object argument,ArrayList senders) 
	{
		throw new IllegalStateException("signal:"+signal+" argument:"+argument+" at:"+this);
	}
	protected boolean canHandle(int signal,Object argument,ArrayList senders) 
	{
		return false;
	}

}
