/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.util.UnionIterator;
import edu.cmu.minorthird.util.gui.ViewerFrame;

import java.util.Set;
import java.util.TreeSet;

/** A single instance for a learner. 
 *
 * @author William Cohen
*/

public class MutableInstance extends AbstractInstance
{
	private WeightedSet wset = new WeightedSet();
	private Set set = new TreeSet();

	public MutableInstance() { this("_unknownSource_",null); }
	public MutableInstance(Object source) { this(source,null); }
	public MutableInstance(Object source,String subpopulationId) { 
		this.source=source; 
		this.subpopulationId=subpopulationId;
	}

  /** Add a numeric feature.  This also deletes the binary version of
	 * f, if it exists.
	 */
	public void addNumeric(Feature f,double v) { 
		set.remove(f);
		wset.add(f,v); 
	}

	/** Add a binary feature. */
	public void addBinary(Feature f) { set.add(f); 	}

	/** Get the weight assigned to a feature in this instance. */
	public double getWeight(Feature f) {
		if (set.contains(f)) return 1.0;
		else return wset.getWeight(f);
	}

	/** Return an iterator over all binary features */
	public Feature.Looper binaryFeatureIterator() {
		return new Feature.Looper( set );
	}

	/** Return an iterator over all numeric features */
	public Feature.Looper numericFeatureIterator() {
		return new Feature.Looper( wset.asSet() );
	}

	/** Return an iterator over all features */
	public Feature.Looper featureIterator() {
		return new Feature.Looper( new UnionIterator(set.iterator(), wset.asSet().iterator()) );
	}

  static public void main(String[] args)
	{
		try {
			MutableInstance instance = new MutableInstance("William Cohen");
			instance.addBinary( new Feature("token lc william") );
			instance.addBinary( new Feature("token lc cohen") );
			instance.addNumeric( new Feature("iq"), 250);
			instance.addNumeric( new Feature("office"), 5317);
			System.out.println(instance);
			ViewerFrame f = new ViewerFrame("TestInstance Viewer", instance.toGUI());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
	

