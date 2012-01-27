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
	
	static final long serialVersionUID=20081125L;
	
	public TransformedVanillaViewer()
	{
		super();
	}

	abstract public Object transform(Object o);

	@Override
	final public JComponent componentFor(Object o) 
	{
		return new JTextArea(transform(o).toString());
	}

	@Override
	public boolean canReceive(Object o)
	{
		try {
			return transform(o)!=null;
		} catch (Exception e) {
			return false;
		}
	}
}
