package edu.cmu.minorthird.classify.relational;

import java.util.Iterator;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.SGMExample;

/**
 * Abstract ClassifierLearner which instantiates the teacher-learner protocol
 * so as to implement a stacked batch learner.
 *
 * @author Zhenzhen Kou
 */

public abstract class StackedBatchClassifierLearner implements
		ClassifierLearner{

	/** This variable saves the last classifier produced by batchTrain.
	 * If it is non-null, then it will be returned by class to
	 * getClassifier().  Implementations of batchTrain should save the
	 * returned classifier to avoid extra work.
	 */

	protected RealRelationalDataset dataset=new RealRelationalDataset();
	
	protected Classifier classifier=null;

	@Override
	final public void reset(){
		dataset=new RealRelationalDataset();
		classifier=null;
	}

	@Override
	final public void setInstancePool(Iterator<Instance> i){}

	@Override
	final public boolean hasNextQuery(){
		return false;
	}

	@Override
	final public Instance nextQuery(){
		return null;
	}

	@Override
	final public void addExample(Example answeredQuery){
		dataset.addSGM((SGMExample)answeredQuery);
		classifier=null;
	}

	@Override
	final public void completeTraining(){
		classifier=batchTrain(dataset);
	}

	@Override
	final public Classifier getClassifier(){
		if(classifier==null){
			classifier=batchTrain(dataset);
		}
		return classifier;
	}
	
	@Override
	public ClassifierLearner copy(){
		StackedBatchClassifierLearner bcl;
		try{
			bcl=(StackedBatchClassifierLearner)this.clone();
			bcl.dataset=new RealRelationalDataset();
			bcl.classifier=null;
		}catch(Exception e){
			System.err.println("Cannot clone "+this);
			e.printStackTrace();
			bcl=null;
		}
		return bcl;
	}

	/** subclasses  should use this method to implement a batch supervised learning algorithm. 
	 */
	abstract public Classifier batchTrain(RealRelationalDataset RelDataset);

}
