/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.util.Iterator;
import java.util.Random;

import edu.cmu.minorthird.util.gui.Visible;

/**
 * A set of examples for learning.
 *
 * @author William Cohen
 */

public interface Dataset extends Visible{
	
	/** Get the FeatureFactory associated with the dataset */
	public FeatureFactory getFeatureFactory();
	
	/** Get the schema associated with the dataset */
	public ExampleSchema getSchema();

	/** Add a new example to the dataset. */
	public void add(Example example);
	
	/** Add a new example to the dataset. Specifying whether or not to compress it. */
	public void add(Example example, boolean compress);

	/** 
	 * Return an iterator over all examples.  This iterator must always
	 * return examples in the order in which they were added, unless the
	 * data has been shuffled.
	 */
	public Iterator<Example> iterator(); 

	/** Return the number of examples. */
	public int size(); 

	// these operations are mostly to support train/testing experiments

	/** Randomly re-order the examples. */
	public void shuffle(Random r);

	/** Randomly re-order the examples. */
	public void shuffle();

	/** Make a shallow copy of the dataset. Examples are shared, but not the 
	 * ordering of the examples. */
	public Dataset shallowCopy();

	/** Partition the dataset as required by the splitter. */
	public Split split(Splitter<Example> splitter);

	/** 
	 * 	A partitioning of the dataset into a number of train/test partitions 
	 */
	public interface Split{
		/** Return the number of partitions */
		public int getNumPartitions();

		/** Return a dataset containing the training cases in the k-th split */
		public Dataset getTrain(int k);

		/** Return a dataset containing the test cases in the k-th split */
		public Dataset getTest(int k);
	}
}


