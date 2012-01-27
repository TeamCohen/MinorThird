package edu.cmu.minorthird.classify.experiments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.cmu.minorthird.classify.Splitter;

/** 
 * Variant of cross-validation in which not all training data is used.
 * Splits data into k separate disjoint folds, then return k
 * train/test splits where each train set a sample of the union of k-1
 * folds, and the test set is the k-th fold.  Preserves subpopulation
 * information.
 *
 * @author William Cohen
 */

public class SubsamplingCrossValSplitter<T> implements Splitter<T>{

	private Random random;

	private int folds;

	private double subsampleFraction;

	private List<Iterator<T>> trainIt;
	private List<Iterator<T>>testIt;

	private CrossValSplitter<T> cvs;

	private RandomSplitter<T> rs;

	public SubsamplingCrossValSplitter(Random random,int folds,
			double subsampleFraction){
		this.random=random;
		this.folds=folds;
		this.subsampleFraction=subsampleFraction;
	}

	public SubsamplingCrossValSplitter(int folds,double subsampleFraction){
		this(new Random(),folds,subsampleFraction);
	}

	public SubsamplingCrossValSplitter(){
		this(5,0.5);
	}

	public int getNumberOfFolds(){
		return folds;
	}

	public void setNumberOfFolds(int k){
		this.folds=k;
	}

	public double getSubsampleFraction(){
		return subsampleFraction;
	}

	public void setSubsampleFraction(double d){
		this.subsampleFraction=d;
	}

	@Override
	public void split(Iterator<T> i){
		cvs=new CrossValSplitter<T>(random,folds);
		rs=new RandomSplitter<T>(random,subsampleFraction);
		cvs.split(i);
		testIt=new ArrayList<Iterator<T>>(folds);
		trainIt=new ArrayList<Iterator<T>>(folds);
		for(int k=0;k<folds;k++){
			testIt.add(cvs.getTest(k));
			rs.split(cvs.getTrain(k));
			trainIt.add(rs.getTrain(0));
		}
	}

	@Override
	public int getNumPartitions(){
		return folds;
	}

	@Override
	public Iterator<T> getTrain(int k){
		return trainIt.get(k);
	}

	//public Iterator getTest(int k) { return testIt[k]; }
	@Override
	public Iterator<T> getTest(int k){
		return cvs.getTest(k);
	}

	@Override
	public String toString(){
		return "[SubCV "+folds+";"+subsampleFraction+"]";
	}

}
