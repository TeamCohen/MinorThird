/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import java.util.Collections;
import java.util.Iterator;

import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.Util;

/**
 * Trains a MultiClassifierLearner using the information in a labeled Dataset.
 *
 * @author Cameron Williams
 *
 */
public class MultiDatasetClassifierTeacher extends MultiClassifierTeacher{

	private MultiDataset dataset;

	private boolean activeLearning=false;

	public MultiDatasetClassifierTeacher(MultiDataset dataset){
		this.dataset=dataset;
	}

	public MultiExampleSchema schema(){
		return dataset.getMultiSchema();
	}

	public Iterator<MultiExample> examplePool(){
		if(activeLearning){
			return Collections.EMPTY_SET.iterator();
		}
		else{
			return dataset.multiIterator();
		}
	}

	public Iterator<Instance> instancePool(){
		if(activeLearning){
			return Util.toInstanceIterator(dataset.multiIterator());
		}else if(dataset instanceof MultiDataset){
			return dataset.iteratorOverUnlabeled();
		}else{
			return Collections.EMPTY_SET.iterator();
		}
	}

	public MultiExample labelInstance(Instance query){
		if(query instanceof MultiExample)
			return (MultiExample)query;
		else
			return null;
	}

	public boolean hasAnswers(){
		return activeLearning;
	}
}
