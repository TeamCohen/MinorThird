package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Common code for all instance implementations
 * @author ksteppe
 */
public abstract class AbstractInstance implements Instance, Visible
{
  protected double weight = 1.0;
  protected Object source;
  protected String subpopulationId;

  /** Return the underlying object being represented. */
	public Object getSource() { return source; }

  /** Return the subpopulation from which the source was drawn. */
	public String getSubpopulationId() { return subpopulationId; }

  /** Get the weight of this instance. */
	public double getWeight() { return weight; }

  /** Retrieve Viewer for the instance */
  public Viewer toGUI()	
	{ 
		return new GUI.InstanceViewer(this); 
	}
}
