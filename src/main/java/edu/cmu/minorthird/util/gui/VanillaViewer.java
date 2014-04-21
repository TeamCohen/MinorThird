package edu.cmu.minorthird.util.gui;

import javax.swing.*;

/**
 * View an objects 'toString()' representation.
 * 
 * @author William cohen
 */

public class VanillaViewer extends ComponentViewer{
	
	static final long serialVersionUID=20080517L;

	public VanillaViewer(Object o){
		super(o);
	}

	public VanillaViewer(){
		super();
	}

	@Override
	public JComponent componentFor(Object o){
		return new JScrollPane(new JTextArea(o.toString()));
	}

	@Override
	public boolean canReceive(Object obj){
		return true;
	}
}
