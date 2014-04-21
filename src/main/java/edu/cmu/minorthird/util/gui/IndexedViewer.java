package edu.cmu.minorthird.util.gui;

import javax.swing.*;

/**
 * Transforms selected objects by running them through an 'index',
 * which maps them to a list, the items of which can be selected.
 * For instance, selected features might be transformed to
 * a list of examples which contain them.
 * 
 * @author William cohen
 */

public abstract class IndexedViewer extends ComponentViewer
{
	
	static final long serialVersionUID=20081125L;
	
	@Override
	public JComponent componentFor(Object o)
	{
		JList jList = new JList(indexFor(o));
		monitorSelections(jList);
		return new JScrollPane(jList);		
	}

	/** Transform the object to a list. */ 
	abstract public Object[] indexFor(Object o);
}
