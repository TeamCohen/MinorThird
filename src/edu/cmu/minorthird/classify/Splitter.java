package edu.cmu.minorthird.classify;

import java.util.Iterator;

/** Split iterators into train/test partitions.
 *
 * @author William Cohen
 */

public interface Splitter
{
	/** Split the iterator into a number of train/test partitions. */
	public void split(Iterator i);

	/** Return the number of partitions produced by the last call to split() */
	public int getNumPartitions();

	/** Return an iterator over the training cases in the k-th split. */
	public Iterator getTrain(int k);

	/** Return an iterator over the test cases in the k-th split. */
	public Iterator getTest(int k);
}
	

