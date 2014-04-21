package edu.cmu.minorthird.text.gui;

import javax.swing.JLabel;

/**
 * A simple display tool for status messages.
 * 
 * @author William Cohen
 */

public class StatusMessage extends JLabel{

	static final long serialVersionUID=200803014L;
	
	public StatusMessage(){
		super("");
	}

	public void display(String msg){
		setText(msg);
	}
}
