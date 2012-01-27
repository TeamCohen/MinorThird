/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JComponent;

import org.apache.log4j.Logger;

/**
 * 
 * Abstract viewer which displays JComponent.
 * 
 * @author William cohen
 */

public abstract class ComponentViewer extends Viewer{
	
	static final long serialVersionUID=20081125L;

	protected Logger log=Logger.getLogger(this.getClass());

	public ComponentViewer(){
		super();
	}

	public ComponentViewer(Object o){
		super(o);
	}

	/** Called at creation time. */
	@Override
	protected void initialize(){
		setLayout(new GridBagLayout());
	}

	/** Get new content. */
	@Override
	public void receiveContent(Object content){
		removeAll();
		JComponent c=componentFor(content);
		add(c,fillerGBC());
	}

	@Override
	public void clearContent(){
		removeAll();
	}

	//
	// override if needed
	//

	@Override
	protected void handle(int signal,Object argument,List<Viewer> senders){
		throw new IllegalStateException("signal:"+signal+" argument:"+argument+
				" at:"+this);
	}

	@Override
	protected boolean canHandle(int signal,Object argument,List<Viewer> senders){
		return false;
	}

	//
	// abstract action
	//

	abstract public JComponent componentFor(Object o);

	// default: recieve anything that can be converted to a component
	@Override
	public boolean canReceive(Object obj){
		try{
			componentFor(obj);
			return true;
		}catch(Exception e){
			return false;
		}
	}
}
