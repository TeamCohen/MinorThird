package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.Splitter;

import java.util.Iterator;

/**
 * Provides exactly one 'split', between the entire set given, and
 * a fixed designated test set.
 *
 * @author William Cohen
 */

public class FixedTestSetSplitter implements Splitter
{
	private Iterator testIterator,trainIterator;

	public FixedTestSetSplitter(Iterator testIterator) { this.testIterator=testIterator; }
	public void split(Iterator i) { this.trainIterator = i; }
	public int getNumPartitions() { return 1; }
	public Iterator getTrain(int k) { return trainIterator; }
	public Iterator getTest(int k) { return testIterator; }
}
	

