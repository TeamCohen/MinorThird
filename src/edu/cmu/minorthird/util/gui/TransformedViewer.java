/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import java.awt.*;
import java.util.ArrayList;

/**
 * View an object after passing it through a transformation.
 * 
 * @author William cohen
 */

abstract public class TransformedViewer extends Viewer
{
	private Viewer subViewer;

	public TransformedViewer()
	{
		super();
	}
	public TransformedViewer(Object obj)
	{
		super(obj);
	}

	public TransformedViewer(Viewer subViewer)
	{
		super();
		setSubView(subViewer);
	}

	public void setSubView(Viewer subViewer)
	{
		this.subViewer = subViewer;
		subViewer.setSuperView(this);
		removeAll();
		add( subViewer, fillerGBC() );
	}

	/** Transform the object before viewing it. */
	abstract public Object transform(Object obj);

	//
	// delegate operations to subViewer
	//

	final public void receiveContent(Object obj)
	{
		subViewer.setContent(transform(obj));
	}
	public void clearContent()
	{
		subViewer.clearContent();
	}
	final public boolean canReceive(Object obj)
	{
		return subViewer.canReceive(transform(obj));
	}
	final protected void handle(int signal,Object argument,ArrayList senders)
	{
		subViewer.handle(signal,argument,senders);
	}
	final protected boolean canHandle(int signal,Object argument,ArrayList senders)
	{
		return subViewer.canHandle(signal,argument,senders);		
	}
	final protected void initialize()
	{
		setLayout(new GridBagLayout());
	}
}
