package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.UnionIterator;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

import java.util.HashSet;
import java.util.Set;

/** 
 * An instance that appears as part of a sequence.
 * 
 * @author William Cohen
*/

public class InstanceFromSequence implements Instance,SequenceConstants
{
	private Instance instance;
	private Set history; 

	/**
	 * @param instance - the instance to extend
	 * @param previousLabels - element k is the classLabel of the example
	 *  (k+1) positions before this instance in the sequence.
	 */
	public InstanceFromSequence(Instance instance,String[] previousLabels)
	{
		this.instance = instance;
		history = new HashSet();
		for (int i=0; i<previousLabels.length; i++) {
			history.add( 
				new Feature(
					new String[]{ HISTORY_FEATURE, Integer.toString((i+1)), previousLabels[i]}) );
		}
	}

	/** Return the wrapped instance. */
	public Instance asPlainInstance() { return instance; }

	//
	// delegate to wrapped instance
	//
	final public Object getSource() { return instance.getSource(); }
	final public String getSubpopulationId() { return instance.getSubpopulationId(); }
	final public Feature.Looper numericFeatureIterator() { return instance.numericFeatureIterator(); }

	//
	// extend the binary feature set
	//

	final public Feature.Looper binaryFeatureIterator() 
	{ 
		return new Feature.Looper( new UnionIterator( history.iterator(), instance.binaryFeatureIterator() ) );
	}

	final public Feature.Looper featureIterator() 
	{ 
		return new Feature.Looper( new UnionIterator( history.iterator(), instance.featureIterator() ) );
	}
	final public double getWeight(Feature f) 
	{ 
		if (history.contains(f)) return 1.0;
		else return instance.getWeight(f); 
	}

	public String toString()
	{
		return "[instFromSeq "+history+" "+instance+"]";
	}

	final public Viewer toGUI()
	{
		return new GUI.InstanceViewer(this);
	}

	/** Utility to create history from a sequence of examples, starting at positive j-1. */
	static public void fillHistory(String[] history, Example[] sequence, int j)
	{
		for (int k=0; k<history.length; k++) {
			if (j-k-1>=0) history[k] = sequence[j-k-1].getLabel().bestClassName();
			else history[k] = NULL_CLASS_NAME;
		}
	}

	/** Utility to create history from a sequence of class labels, starting at positive j-1. */
	static public void fillHistory(String[] history, ClassLabel[] labels, int j)
	{
		for (int k=0; k<history.length; k++) {
			if (j-k-1>=0) history[k] = labels[j-k-1].bestClassName();
			else history[k] = NULL_CLASS_NAME;
		}
	}

	/** Utility to create history from a sequence of Strings, starting at positive j-1. */
	static public void fillHistory(String[] history, String[] labels, int j)
	{
		for (int k=0; k<history.length; k++) {
			if (j-k-1>=0) history[k] = labels[j-k-1];
			else history[k] = NULL_CLASS_NAME;
		}
	}


	static public void main(String[] argv)
	{
		MutableInstance instance = new MutableInstance("William Cohen");
		instance.addBinary( new Feature("token lc william") );
		instance.addBinary( new Feature("token lc cohen") );
		instance.addNumeric( new Feature("iq"), 250);
		instance.addNumeric( new Feature("office"), 5317);
		InstanceFromSequence inseq = 
			new InstanceFromSequence(instance, new String[] { "dweeb","cool", "cool" }); 
		ViewerFrame f = new ViewerFrame("TestInstance Viewer", inseq.toGUI());
	}
}
