package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.algorithms.random.RandomElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Split into one train, one test partition.
 * Preserves subpopulation information, if it's present.
 *
 * @author William Cohen
 */

public class RandomSplitter implements Splitter
{
	private RandomElement random;
	private double trainFraction;

	private List trainList=null, testList=null;

	public RandomSplitter(RandomElement random, double trainFraction) 
	{
		this.random = random; this.trainFraction = trainFraction;
	}
	public RandomSplitter(double trainFraction) {	this(new RandomElement(), trainFraction);	}
	public RandomSplitter() {	this(new RandomElement(), 0.7);	}

	public void setTrainFraction(double f) { this.trainFraction=f; }
	public double getTrainFraction() { return trainFraction; }

	public void split(Iterator i) {
		trainList = new ArrayList();
		testList = new ArrayList();
		Iterator j = new SubpopSorter(i).subpopIterator();
		while (j.hasNext()) {
			List subpop = (List)j.next();
			if (random.raw() <= trainFraction) trainList.addAll(subpop);
			else testList.addAll(subpop);
		}
	}

	public int getNumPartitions() { return 1; }

	public Iterator getTrain(int k) { 
		return trainList.iterator();
	}

	public Iterator getTest(int k) {
		return testList.iterator();
	}

	public String toString() { return "[RandomSplit "+trainFraction+"]"; }
}
