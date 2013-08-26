package edu.cmu.minorthird.classify.experiments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.minorthird.classify.Splitter;

/**
 * Provides exactly one 'split', between the entire set given, and a fixed
 * designated test set.
 * 
 * @author William Cohen
 */

public class FixedTestSetSplitter<T> implements Splitter<T>{

	private List<T> testList,trainList;

	public FixedTestSetSplitter(Iterator<T> testIterator){
		testList=iteratorToList(testIterator);
	}

	@Override
	public void split(Iterator<T> i){
		trainList=iteratorToList(i);
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

	private List<T> iteratorToList(Iterator<T> i){
		List<T> result=new ArrayList<T>();
		while(i.hasNext()){
			result.add(i.next());
		}
		return result;
	}
}
