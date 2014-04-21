package edu.cmu.minorthird.classify.experiments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.cmu.minorthird.classify.Splitter;

/** Split into one train, one test partition.
 * Ignores subpopulation information.
 *
 * @author William Cohen
 */

public class SimpleRandomSplitter<T> implements Splitter<T>{

	private Random random;

	private double trainFraction;

	private List<T> trainList=null,testList=null;

	public SimpleRandomSplitter(Random random,double trainFraction){
		this.random=random;
		this.trainFraction=trainFraction;
	}

	public SimpleRandomSplitter(double trainFraction){
		this(new Random(),trainFraction);
	}

	public SimpleRandomSplitter(){
		this(0.7);
	}

	@Override
	public void split(Iterator<T> i){
		trainList=new ArrayList<T>();
		testList=new ArrayList<T>();
		while(i.hasNext()){
			T t=i.next();
			if(random.nextDouble()<=trainFraction)
				trainList.add(t);
			else
				testList.add(t);
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
}
