package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.UnionIterator;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

import java.util.HashSet;
import java.util.Set;

/** 
 * An instance that has predicted values from each dimension added as features
 * 
 * @author Cameron Williams
*/

public class InstanceFromPrediction implements Instance
{
	private Instance instance;
	private Set history; 

	/**
	 * @param instance - the instance to extend
	 * @param previousLabels - element k is the classLabel of the example
	 *  (k+1) positions before this instance in the sequence.
	 */
	public InstanceFromPrediction(Instance instance,String[] previousLabels)
	{
	    
		this.instance = instance;
		history = new HashSet();
		for (int i=0; i<previousLabels.length; i++) {
			history.add( 
				new Feature(
					new String[]{ "Predicted Label for Dimension: " + i, previousLabels[i]}) );
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
		return "[instFromPrediction "+history+" "+instance+"]";
	}

	final public Viewer toGUI()
	{
	    return new GUI.InstanceViewer((Instance)this);
	}

}
