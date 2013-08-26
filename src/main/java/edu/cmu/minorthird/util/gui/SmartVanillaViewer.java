package edu.cmu.minorthird.util.gui;

import javax.swing.*;

/**
 * View an objects in one of two ways: its toString() representation, if it's
 * not visible; or its toGUI() representation, if it is.
 * 
 * @author William cohen
 */

public class SmartVanillaViewer extends ComponentViewer{
	
	static final long serialVersionUID=20080517L;

	public SmartVanillaViewer(Object o){
		super(o);
	}

	public SmartVanillaViewer(){
		super();
	}

	@Override
	public boolean canReceive(Object obj){
		return true;
	}

	@Override
	public JComponent componentFor(Object o){
		if(o==null){
			return new JLabel("[null pointer]");
		}else if(o instanceof Visible){
			Viewer v=((Visible)o).toGUI();
			v.setSuperView(this);
			v.setContent(o);
			return v;
		}else{
			return new JScrollPane(new JTextArea(o.toString()));
		}
	}
}
