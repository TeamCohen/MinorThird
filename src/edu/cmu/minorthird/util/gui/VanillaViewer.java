package edu.cmu.minorthird.util.gui;

import javax.swing.*;

/**
 * View an objects 'toString()' representation.
 * 
 * @author William cohen
 */

public class VanillaViewer extends ComponentViewer
{
	public VanillaViewer(Object o)
	{
		super(o);
	}
	public VanillaViewer()
	{
		super();
	}


	public JComponent componentFor(Object o) 
	{
		return new JScrollPane(new JTextArea(o.toString()));
	}

	public boolean canReceive(Object obj)
	{
		return true;
	}
}
