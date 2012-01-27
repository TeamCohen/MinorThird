/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import edu.cmu.minorthird.util.UnionIterator;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/** 
 * A single instance for a learner. 
 *
 * @author William Cohen
 */

public class MutableInstance extends AbstractInstance{

	private Set<Feature> binarySet=new TreeSet<Feature>();
	private WeightedSet<Feature> numericSet=new WeightedSet<Feature>();
	
	public MutableInstance(Object source,String subpopulationId){ 
		this.source=source; 
		this.subpopulationId=subpopulationId;
	}
	
	public MutableInstance(Object source){
		this(source,null);
	}

	public MutableInstance(){
		this("_unknownSource_");
	}

	/** 
	 * Add a numeric feature. This also deletes the binary version of
	 * feature, if it exists.
	 */
	public void addNumeric(Feature feature,double value){ 
		binarySet.remove(feature);
		numericSet.add(feature,value); 
	}

	/** Add a binary feature. */
	public void addBinary(Feature feature){
		binarySet.add(feature);
	}

	/** Get the weight assigned to a feature in this instance. */
	@Override
	public double getWeight(Feature feature){
		if(binarySet.contains(feature)){
			return 1.0;
		}
		else{
			return numericSet.getWeight(feature);
		}
	}
	
	/** Return an iterator over all binary features */
	@Override
	public Iterator<Feature> binaryFeatureIterator(){
		return binarySet.iterator();
	}

	/** Return an iterator over all numeric features */
	@Override
	public Iterator<Feature> numericFeatureIterator(){
		return numericSet.iterator();
	}

	/** Return an iterator over all features */
	@Override
	public Iterator<Feature> featureIterator(){
		return new UnionIterator<Feature>(binaryFeatureIterator(),numericFeatureIterator());
	}
	
	@Override
	public int numFeatures(){
		return binarySet.size()+numericSet.size();
	}

	static public void main(String[] args){
		try{
			MutableInstance instance=new MutableInstance("William Cohen");
			instance.addBinary(new Feature("token lc william"));
			instance.addBinary(new Feature("token lc cohen"));
			instance.addNumeric(new Feature("iq"),250);
			instance.addNumeric(new Feature("office"),5317);
			System.out.println(instance);
			new ViewerFrame("TestInstance Viewer", instance.toGUI());
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}


