package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.*;
import java.util.*;

/**
 * Provides exactly one 'split', between the entire set given, and
 * a fixed designated test set.
 *
 * @author William Cohen
 */

public class FixedTestSetSplitter implements Splitter
{
	private List testList,trainList;

	public FixedTestSetSplitter(Iterator testIterator) { testList = iteratorToList(testIterator);	}
	public void split(Iterator i) { trainList = iteratorToList(i); }
	public int getNumPartitions() { return 1; }
	public Iterator getTrain(int k) { return trainList.iterator(); }
	public Iterator getTest(int k) { return testList.iterator(); }

	private List iteratorToList(Iterator i)
	{
		List result = new ArrayList();
		while (i.hasNext()) {
			Object o = i.next();
			result.add(o);
		}
		return result;
	}
}
	

