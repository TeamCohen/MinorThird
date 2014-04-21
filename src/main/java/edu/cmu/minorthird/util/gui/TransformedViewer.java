/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import java.awt.GridBagLayout;
import java.util.List;

/**
 * View an object after passing it through a transformation.
 * 
 * @author William cohen
 */

abstract public class TransformedViewer extends Viewer{

	static final long serialVersionUID=20081125L;
	
	private Viewer subViewer;

	public TransformedViewer(){
		super();
	}

	public TransformedViewer(Object obj){
		super(obj);
	}

	public TransformedViewer(Viewer subViewer){
		super();
		setSubView(subViewer);
	}

	public void setSubView(Viewer subViewer){
		this.subViewer=subViewer;
		subViewer.setSuperView(this);
		removeAll();
		add(subViewer,fillerGBC());
	}

	/** Transform the object before viewing it. */
	abstract public Object transform(Object obj);

	//
	// delegate operations to subViewer
	//

	@Override
	final public void receiveContent(Object obj){
		if(subViewer==null){
			throw new IllegalStateException("no subViewer has bee set for "+this);
		}
		subViewer.setContent(transform(obj));
	}

	@Override
	public void clearContent(){
		subViewer.clearContent();
	}

	@Override
	final public boolean canReceive(Object obj){
		return subViewer.canReceive(transform(obj));
	}

	@Override
	final protected void handle(int signal,Object argument,List<Viewer> senders){
		subViewer.handle(signal,argument,senders);
	}

	@Override
	final protected boolean canHandle(int signal,Object argument,List<Viewer> senders){
		return subViewer.canHandle(signal,argument,senders);
	}

	@Override
	final protected void initialize(){
		setLayout(new GridBagLayout());
	}
}
