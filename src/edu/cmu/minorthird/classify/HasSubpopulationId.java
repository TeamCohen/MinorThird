/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

/** Marker interface for objects which support the
 * 'getSubpopulationId' method.
 * 
 * @author William Cohen
*/

public interface HasSubpopulationId
{
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
}

