package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.algorithms.random.RandomElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 
 * Variant of cross-validation in which not all training data is used.
 * Splits data into k separate disjoint folds, then return k
 * train/test splits where each train set a sample of the union of k-1
 * folds, and the test set is the k-th fold.  Preserves subpopulation
 * information.
 *
 * @author William Cohen
 */

public class SubsamplingCrossValSplitter implements Splitter
{
	private RandomElement random;
	private int folds;
	private double subsampleFraction;
	private Iterator[] trainIt, testIt;

	public SubsamplingCrossValSplitter(RandomElement random, int folds, double subsampleFraction) 
	{
		this.random = random; 
		this.folds = folds; 
		this.subsampleFraction=subsampleFraction;
	}
	public SubsamplingCrossValSplitter(int folds, double subsampleFraction) 
	{ 
		this(new RandomElement(), folds, subsampleFraction); 
	}
	public SubsamplingCrossValSplitter() 
	{	
		this(new RandomElement(), 5, 0.5);	
	}

	public int getNumberOfFolds() { return folds; }
	public void setNumberOfFolds(int k) { this.folds=k; }

	public double getSubsampleFraction() { return subsampleFraction; }
	public void setSubsampleFraction(double d) { this.subsampleFraction=d; }

	public void split(Iterator i) {
		CrossValSplitter cvs = new CrossValSplitter(random,folds);
		RandomSplitter rs = new RandomSplitter(random,subsampleFraction);
		cvs.split( i );
		testIt = new Iterator[folds];
		trainIt = new Iterator[folds];		
		for (int k=0; k<folds; k++) {
			testIt[k] = cvs.getTest(k); 
			rs.split( cvs.getTrain(k) );
			trainIt[k] = rs.getTrain(0); 
		}
	}

	public int getNumPartitions() { return folds; }

	public Iterator getTrain(int k) { return trainIt[k]; }

	public Iterator getTest(int k) { return testIt[k]; }

	public String toString() { return "[SubCV "+folds+";"+subsampleFraction+"]"; }

}

