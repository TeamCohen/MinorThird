package edu.cmu.minorthird.util.gui;

/**
 * Marker interface for things with GUIs.
 *
 */

public interface Visible
{
	/** Create a view of this object */ 
	public Viewer toGUI();
}
