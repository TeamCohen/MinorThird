/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Controls for a ControlledViewer.
 *
 * The intended use for this is to couple it with a ControllableViewer
 * in a ControlledViewer.  When this object is 'updated', the
 * ControllableViewer's applyControls() method will be called with
 * this object, and the ControllableViewer can then interrogate the
 * ViewerControls to get the state of the buttons. An 'update' is any
 * actionEvent forwarded with the ViewerControls.  
 *
 * <p> To use this, add a set of JButtons, etc to this object in the
 * abstract initialize() routine.  If any buttons are to force an
 * immediate update, then use addActionListener(this).  If desired,
 * add an 'applyButton()', which simply is a button which forces an
 * update.
 * 
 * @author William cohen
 */

abstract public class ViewerControls extends JPanel implements ActionListener{
	
	static final long serialVersionUID=20081125L;

	public static final int BOTTOM=1,RIGHT=2,TOP=3,LEFT=4;

	private Viewer viewer=null;

	public ViewerControls(){
		super();
		initialize();
	}

	/** Declare the viewer controlled by this ViewerControls object. */
	public void setControlledViewer(Viewer viewer){
		if(!(viewer instanceof Controllable))
			throw new IllegalArgumentException("viewer must be controllable");
		this.viewer=viewer;
	}

	/** Return the viewer that is controlled by this object. */
	public Controllable getControlledViewer(){
		return (Controllable)viewer;
	}

	/** Add an update button. */
	public void addApplyButton(){
		add(makeApplyButton());
	}

	/** Create an 'apply' button. */
	public JButton makeApplyButton(){
		return new JButton(new AbstractAction("Apply"){

			static final long serialVersionUID=20080517L;

			@Override
			public void actionPerformed(ActionEvent e){
				((Controllable)viewer).applyControls(ViewerControls.this);
			}
		});
	}

	// implement ActionListener
	@Override
	public void actionPerformed(ActionEvent e){
		((Controllable)viewer).applyControls(this);
	}

	/** Override this with one of the other values to help
	 * ControlledViewer decide where to place the controls. 
	 */
	public int preferredLocation(){
		return BOTTOM;
	}

	/** Override this with one of the other values to help
	 * ControlledViewer decide whether to allow the
	 * ViewerControls to be resizable
	 */
	public boolean prefersToBeResized(){
		return false;
	}

	//
	// abstract actions
	//

	/** Set up any buttons, etc for this set of ViewerControls. */
	abstract protected void initialize();
}
