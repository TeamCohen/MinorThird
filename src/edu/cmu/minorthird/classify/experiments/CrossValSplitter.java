package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.algorithms.random.RandomElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Split into k separate disjoint folds, then return k train/test splits
 * where each train set is the union of k-1 folds, and the test set
 * is the k-th fold.  Preserves subpopulation information.
 *
 * @author William Cohen
 */

public class CrossValSplitter implements Splitter
{
	private RandomElement random;
	private int folds;
	private List subpops;

	public CrossValSplitter(RandomElement random, int folds) 
	{
		this.random = random; this.folds = folds;
	}
	public CrossValSplitter(int folds) { this(new RandomElement(), folds); }
	public CrossValSplitter() {	this(new RandomElement(), 5);	}

	public int getNumberOfFolds() { return folds; }
	public void setNumberOfFolds(int k) { this.folds=k; }

	public void split(Iterator i) {
		subpops = new ArrayList();
		for (Iterator j = new SubpopSorter(random,i).subpopIterator(); j.hasNext(); ) {
			subpops.add( j.next() );
		}
	}

	public int getNumPartitions() { return folds; }

	public Iterator getTrain(int k) { 
		List trainList = new ArrayList();
		for (int i=0; i<subpops.size(); i++) {
			if (i%folds != k) {
				trainList.addAll((List)subpops.get(i) );
			}
		}
		return trainList.iterator();
	}

	public Iterator getTest(int k) {
		List testList = new ArrayList();
		for (int i=0; i<subpops.size(); i++) {
			if (i%folds == k) {
				testList.addAll((List)subpops.get(i) );
			}
		}
		return testList.iterator();
	}
	public String toString() { return "["+folds+"-CV splitter]"; }
}

