package edu.cmu.minorthird.classify.multi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.UnionIterator;
import edu.cmu.minorthird.util.gui.Viewer;

/** 
 * An instance that has predicted values from each dimension added as features
 * 
 * @author Cameron Williams
 */

public class InstanceFromPrediction implements Instance{

	private Instance instance;

	private Set<Feature> history;

	/**
	 * @param instance - the instance to extend
	 * @param previousLabels - element k is the classLabel of the example
	 *  (k+1) positions before this instance in the sequence.
	 */
	public InstanceFromPrediction(Instance instance,String[] previousLabels){

		this.instance=instance;
		history=new HashSet<Feature>();
		for(int i=0;i<previousLabels.length;i++){
			history.add(new Feature(new String[]{"Predicted Label for Dimension: "+i,
					previousLabels[i]}));
		}
	}

	/** Return the wrapped instance. */
	public Instance asPlainInstance(){
		return instance;
	}

	//
	// delegate to wrapped instance
	//
	@Override
	final public Object getSource(){
		return instance.getSource();
	}

	@Override
	final public String getSubpopulationId(){
		return instance.getSubpopulationId();
	}

	@Override
	final public Iterator<Feature> numericFeatureIterator(){
		return instance.numericFeatureIterator();
	}

	//
	// extend the binary feature set
	//

	@Override
	final public Iterator<Feature> binaryFeatureIterator(){
		return new UnionIterator<Feature>(history.iterator(),instance.binaryFeatureIterator());
	}

	@Override
	final public Iterator<Feature> featureIterator(){
		return new UnionIterator<Feature>(history.iterator(),instance.featureIterator());
	}
	
	@Override
	final public int numFeatures(){
		return history.size()+instance.numFeatures();
	}

	@Override
	final public double getWeight(Feature f){
		if(history.contains(f))
			return 1.0;
		else
			return instance.getWeight(f);
	}

	@Override
	public String toString(){
		return "[instFromPrediction "+history+" "+instance+"]";
	}

	@Override
	final public Viewer toGUI(){
		return new GUI.InstanceViewer(this);
	}

}
