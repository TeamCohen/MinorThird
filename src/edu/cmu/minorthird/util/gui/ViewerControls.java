/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
 * add an 'updateButton()', which simply is a button which forces an
 * update.
 * 
 * @author William cohen
 */

abstract public class ViewerControls extends JToolBar implements ActionListener
{
	private Viewer viewer = null;

	public ViewerControls()	
	{	
		super(); 
		initialize(); 
	}

	/** Declare the viewer controlled by this ViewerControls object. */
	public void setControlledViewer(Viewer viewer) 
	{ 
		if (!(viewer instanceof Controllable)) throw new IllegalArgumentException("viewer must be controllable");
		this.viewer = viewer; 
	}

	/** Add an update button. */
	public void addApplyButton() 
	{ 
		add(new JButton(new AbstractAction("Apply") {
				public void actionPerformed(ActionEvent e) { 
					((Controllable)viewer).applyControls(ViewerControls.this);
				}				
			}));
	}

	// implement ActionListener
	public void actionPerformed(ActionEvent e) 
	{ 
		((Controllable)viewer).applyControls(this);
	}

	//
	// abstract actions
	//

	/** Set up any buttons, etc for this set of ViewerControls. */ 
	abstract protected void initialize();
}
