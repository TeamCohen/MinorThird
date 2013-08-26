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

	@Override
	public MultiExampleSchema schema(){
		return dataset.getMultiSchema();
	}

	@Override
	public Iterator<MultiExample> examplePool(){
		if(activeLearning){
			return Collections.<MultiExample>emptySet().iterator();
		}
		else{
			return dataset.multiIterator();
		}
	}

	@Override
	public Iterator<Instance> instancePool(){
		if(activeLearning){
			return Util.toInstanceIterator(dataset.multiIterator());
		}else if(dataset instanceof MultiDataset){
			return dataset.iteratorOverUnlabeled();
		}else{
			return Collections.<Instance>emptySet().iterator();
		}
	}

	@Override
	public MultiExample labelInstance(Instance query){
		if(query instanceof MultiExample)
			return (MultiExample)query;
		else
			return null;
	}

	@Override
	public boolean hasAnswers(){
		return activeLearning;
	}
}
