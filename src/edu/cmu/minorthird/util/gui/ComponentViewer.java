/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 *
 * Abstract viewer which displays JComponent.
 * 
 * @author William cohen
 */

abstract public class ComponentViewer extends Viewer
{
//	static private Logger log = Logger.getLogger(ComponentViewer.class);
  protected Logger log = Logger.getLogger(this.getClass());

	public ComponentViewer()
	{
		super();
	}

	public ComponentViewer(Object o)
	{
		super(o);
	}

	/** Called at creation time. */
	protected void initialize() 
	{
		setLayout(new GridBagLayout());
	}

	/** Get new content. */	
	public void receiveContent(Object content)
	{
//		log.info("recieving content: "+content+" at "+this);
		removeAll();
		JComponent c = componentFor(content);
		//Kevin, the line below was crashing, presumably because 'log'
		//is null---removing the line below lets it work...
		//the line that threw the error was
		// java edu.cmu.minorthird.ui.TrainTestClassifier -labels cspace.07 -spanType meeting -showLabels -learner "SVMLearner()" -showResult -showData -splitter k5
		//log.debug("adding component "+c+" at "+this);
		add( c, fillerGBC() );
	}	

	public void clearContent()
	{
		removeAll();
	}	

	//
	// override if needed
	//

	protected void handle(int signal,Object argument,ArrayList senders) 
	{
		throw new IllegalStateException("signal:"+signal+" argument:"+argument+" at:"+this);
	}
	protected boolean canHandle(int signal,Object argument,ArrayList senders) 
	{
		return false;
	}

	//
	// abstract action
	//

	abstract public JComponent componentFor(Object o); 

	// default: recieve anything that can be converted to a component
	public boolean canReceive(Object obj)
	{
		try { 
			JComponent tmp = componentFor(obj);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
