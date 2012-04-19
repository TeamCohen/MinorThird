package edu.cmu.minorthird.classify.experiments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.cmu.minorthird.classify.Splitter;

/**
 * Split into one train, one test partition. Preserves subpopulation
 * information, if it's present.
 * 
 * @author William Cohen
 */

public class RandomSplitter<T> implements Splitter<T>{

	private Random random;

	private double trainFraction;

	private List<T> trainList=null;
	
	private List<T> testList=null;

	public RandomSplitter(Random random,double trainFraction){
		this.random=random;
		this.trainFraction=trainFraction;
	}

	public RandomSplitter(double trainFraction){
		this(new Random(),trainFraction);
	}

	public RandomSplitter(){
		this(0.7);
	}

	public void setTrainFraction(double f){
		this.trainFraction=f;
	}

	public double getTrainFraction(){
		return trainFraction;
	}

	@Override
	public void split(Iterator<T> i){
		trainList=new ArrayList<T>();
		testList=new ArrayList<T>();
		Iterator<List<T>> j=new SubpopSorter<T>(i).subpopIterator();
		while(j.hasNext()){
			List<T> subpop=j.next();
			if(random.nextDouble()<=trainFraction){
				trainList.addAll(subpop);
			}
			else{
				testList.addAll(subpop);
			}
		}
	}

	@Override
	public int getNumPartitions(){
		return 1;
	}

	@Override
	public Iterator<T> getTrain(int k){
		return trainList.iterator();
	}

	@Override
	public Iterator<T> getTest(int k){
		return testList.iterator();
	}

	@Override
	public String toString(){
		return "[RandomSplit "+trainFraction+"]";
	}
}
