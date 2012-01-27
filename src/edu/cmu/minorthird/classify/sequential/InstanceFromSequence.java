package edu.cmu.minorthird.classify.sequential;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.GUI;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.util.UnionIterator;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/** 
 * An instance that appears as part of a sequence.
 * 
 * @author William Cohen
*/

public class InstanceFromSequence implements Instance,SequenceConstants
{
	private Instance instance;
	private Set<Feature> history; 

	/**
	 * @param instance - the instance to extend
	 * @param previousLabels - element k is the classLabel of the example
	 *  (k+1) positions before this instance in the sequence.
	 */
	public InstanceFromSequence(Instance instance,String[] previousLabels)
	{
		this.instance = instance;
		history = new HashSet<Feature>();
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
	@Override
	final public Object getSource() { return instance.getSource(); }
	@Override
	final public String getSubpopulationId() { return instance.getSubpopulationId(); }
	@Override
	final public Iterator<Feature> numericFeatureIterator() { return instance.numericFeatureIterator(); }

	//
	// extend the binary feature set
	//

	@Override
	final public Iterator<Feature> binaryFeatureIterator() 
	{ 
		return new UnionIterator<Feature>( history.iterator(), instance.binaryFeatureIterator() ) ;
	}

	@Override
	final public Iterator<Feature> featureIterator() 
	{ 
		return new UnionIterator<Feature>( history.iterator(), instance.featureIterator() );
	}
	
	@Override
	final public int numFeatures(){
		throw new UnsupportedOperationException();
	}
	
	@Override
	final public double getWeight(Feature f) 
	{ 
		if (history.contains(f)) return 1.0;
		else return instance.getWeight(f); 
	}

	@Override
	public String toString()
	{
		return "[instFromSeq "+history+" "+instance+"]";
	}

	@Override
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
		new ViewerFrame("TestInstance Viewer", inseq.toGUI());
	}
}
