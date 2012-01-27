/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

import edu.cmu.minorthird.classify.experiments.Expt;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Trains a ClassifierLearner using the information in  a labeled Dataset.
 *
 * @author William Cohen
 *
 */
public class DatasetClassifierTeacher extends ClassifierTeacher{

	private Dataset dataset;

	private boolean activeLearning=false;

	public DatasetClassifierTeacher(Dataset dataset){
		this(dataset,false);
	}

	/**
	 * @param activeLearning if true, all learning is active - ie nothing is
	 * pushed at the learner, everything must be 'pulled' via queries.
	 * if false, all examples fron the dataset are 'pushed' at the learner
	 * via addExample.
	 */
	public DatasetClassifierTeacher(Dataset dataset,boolean activeLearning){
		this.dataset=dataset;
		this.activeLearning=activeLearning;
	}

	@Override
	public ExampleSchema schema(){
		return dataset.getSchema();
	}

	@Override
	public Iterator<Example> examplePool(){
		if(activeLearning){
			return Collections.<Example>emptySet().iterator();
		}
		else{
			return dataset.iterator();
		}
	}

	@Override
	public Iterator<Instance> instancePool(){
		if(activeLearning){
			return Util.toInstanceIterator(dataset.iterator());
		}else if(dataset instanceof BasicDataset){
			// (Edoardo Airoldi)  this itearator is empty whenever there are no
			// unlabeled examples available for semi-supervised learning.
			return ((BasicDataset)dataset).iteratorOverUnlabeled();
		}else{
			return Collections.<Instance>emptySet().iterator();
		}
	}

	@Override
	public Example labelInstance(Instance query){
		// the label was hidden by just hiding the type
		if(query instanceof Example)
			return (Example)query;
		else
			return null;
	}

	@Override
	public boolean hasAnswers(){
		return activeLearning;
	}

	static public void main(String[] argv){
		try{
			Dataset dataset=Expt.toDataset(argv[0]);
			ClassifierLearner learner=Expt.toLearner(argv[1]);
			Classifier c=new DatasetClassifierTeacher(dataset).train(learner);
			if(c instanceof Visible){
				new ViewerFrame("from "+argv[0]+" and "+argv[1],((Visible)c).toGUI());
			}else{
				System.out.println("Learnt classifier: "+c);
			}
			System.out.println("Training error:   "+Tester.errorRate(c,dataset));
			if(c instanceof BinaryClassifier){
				System.out.println("Average log loss: "+
						Tester.logLoss(((BinaryClassifier)c),dataset));
			}
			if(argv.length>=3&&(c instanceof Serializable)){
				IOUtil.saveSerialized(((Serializable)c),new File(argv[2]));
			}
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("usage: dataset learner [classifierFile]");
		}
	}
}
