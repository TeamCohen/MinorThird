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

	public void split(Iterator<T> i){
		trainList=iteratorToList(i);
	}

	public int getNumPartitions(){
		return 1;
	}

	public Iterator<T> getTrain(int k){
		return trainList.iterator();
	}

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
