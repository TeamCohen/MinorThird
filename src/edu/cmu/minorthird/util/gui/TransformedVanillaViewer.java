package edu.cmu.minorthird.util.gui;

import javax.swing.*;

/**
 * A 'vanilla' view of a transformed object.
 * 
 * @author William cohen
 *
 */

abstract public class TransformedVanillaViewer extends ComponentViewer
{
	public TransformedVanillaViewer()
	{
		super();
	}

	abstract public Object transform(Object o);

	final public JComponent componentFor(Object o) 
	{
		return new JTextArea(transform(o).toString());
	}

	public boolean canReceive(Object o)
	{
		try {
			return transform(o)!=null;
		} catch (Exception e) {
			return false;
		}
	}
}
