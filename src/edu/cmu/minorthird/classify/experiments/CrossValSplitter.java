package edu.cmu.minorthird.classify.experiments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.cmu.minorthird.classify.Splitter;

/** 
 * Split into k separate disjoint folds, then return k train/test splits
 * where each train set is the union of k-1 folds, and the test set
 * is the k-th fold.  Preserves subpopulation information.
 *
 * @author William Cohen
 */

public class CrossValSplitter<T> implements Splitter<T>{

	private Random random;

	private int folds;

	private List<List<T>> subpops;

	public CrossValSplitter(Random random,int folds){
		this.random=random;
		this.folds=folds;
	}

	public CrossValSplitter(int folds){
		this(new Random(),folds);
	}

	public CrossValSplitter(){
		this(new Random(),5);
	}

	public int getNumberOfFolds(){
		return folds;
	}

	public void setNumberOfFolds(int k){
		this.folds=k;
	}

	@Override
	public void split(Iterator<T> i){
		subpops=new ArrayList<List<T>>();
		for(Iterator<List<T>> j=new SubpopSorter<T>(random,i).subpopIterator();j.hasNext();){
			subpops.add(j.next());
		}
	}

	@Override
	public int getNumPartitions(){
		return folds;
	}

	@Override
	public Iterator<T> getTrain(int k){
		List<T> trainList=new ArrayList<T>();
		for(int i=0;i<subpops.size();i++){
			if(i%folds!=k){
				trainList.addAll(subpops.get(i));
			}
		}
		return trainList.iterator();
	}

	@Override
	public Iterator<T> getTest(int k){
		List<T> testList=new ArrayList<T>();
		for(int i=0;i<subpops.size();i++){
			if(i%folds==k){
				testList.addAll(subpops.get(i));
			}
		}
		return testList.iterator();
	}

	@Override
	public String toString(){
		return "["+folds+"-CV splitter]";
	}
}
