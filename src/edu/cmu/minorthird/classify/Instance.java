/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.util.AbstractLooper;
import edu.cmu.minorthird.util.gui.Visible;

import java.util.Collection;
import java.util.Iterator;

/** A single instance for a learner. 
 * This is basically a weighted set of features.
 *
 * @author William Cohen
*/

public interface Instance extends HasSubpopulationId,Visible
{
	/** Get the weight assigned to a feature in this instance. */
	public double getWeight(Feature f);

	/** Return an iterator over all binary features. */
	public Feature.Looper binaryFeatureIterator();

	/** Return an iterator over all numeric features. */
	public Feature.Looper numericFeatureIterator();

	/** Return an iterator over all features */
	public Feature.Looper featureIterator();

	/** Get the underlying object used that this instance describes */ 
	public Object getSource();

	/** Get the subpopulation from which this instance was drawn. 
	 * A null id is considered to be a unique subpopulation---different
	 * from every other subpopulation, including other nulls.
	 * A subpopulation is a subset of the training data which is
	 * expected to contain additional regularities - for instance,
	 * pages from the same site, or spans from the same document.
	 * Testing routines may use subpopulation's to more correctly
	 * spit datasets into train/test subsets. 
	 */
	public String getSubpopulationId(); 

	/** Iterator class */

	static public class Looper extends AbstractLooper {
		public Looper(Collection c) { super(c); }
		public Looper(Iterator i) { super(i); }
		public Instance nextInstance() { return (Instance)next(); }
	}

}

