package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.algorithms.random.RandomElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Split into one train, one test partition.
 * Ignores subpopulation information.
 *
 * @author William Cohen
 */

public class SimpleRandomSplitter implements Splitter
{
	private RandomElement random;
	private double trainFraction;

	private List trainList=null, testList=null;

	public SimpleRandomSplitter(RandomElement random, double trainFraction) {
		this.random = random; this.trainFraction = trainFraction;
	}
	public SimpleRandomSplitter(double trainFraction) {
		this(new RandomElement(), trainFraction);
	}
	public SimpleRandomSplitter() {
		this(new RandomElement(), 0.7);
	}

	public void split(Iterator i) {
		trainList = new ArrayList();
		testList = new ArrayList();
		while (i.hasNext()) {
			Object o = i.next();
            System.out.println(i.getClass().getName());
			if (random.raw() <= trainFraction) trainList.add(o);
			else testList.add(o);
		}
	}

	public int getNumPartitions() { return 1; }

	public Iterator getTrain(int k) { 
		return trainList.iterator();
	}

	public Iterator getTest(int k) {
		return testList.iterator();
	}
}
