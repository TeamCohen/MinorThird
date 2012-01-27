/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.util.Iterator;

import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.util.ProgressCounter;

/**
 * Batch version of an OnlineClassifierLearner. 
 *
 * @author William Cohen
 */

public class BatchVersion extends BatchClassifierLearner
{
	private OnlineClassifierLearner innerLearner;
	private int numberOfEpochs = 1;
	//private List<Example> exampleList = new ArrayList<Example>();

	public BatchVersion(OnlineClassifierLearner innerLearner,int numberOfEpochs)
	{
		this.innerLearner = innerLearner;
		this.numberOfEpochs = numberOfEpochs;
	}
	public BatchVersion(OnlineClassifierLearner innerLearner)	{	this(innerLearner,1);	}
	public BatchVersion() {	this(new VotedPerceptron(),5); }
	public int getNumberOfEpochs() { return numberOfEpochs; }
	public void setNumberOfEpochs(int n) { numberOfEpochs=n; }
	public OnlineClassifierLearner getInnerLearner() { return innerLearner; }
	public void setInnerLearner(OnlineClassifierLearner learner) { this.innerLearner=learner; }

	@Override
	final public void setSchema(ExampleSchema schema)	{	innerLearner.setSchema(schema);	}
	@Override
	final public ExampleSchema getSchema(){return innerLearner.getSchema();}

	@Override
	public Classifier batchTrain(Dataset dataset)
	{
		Dataset copy = dataset.shallowCopy();
		copy.shuffle();
		innerLearner.reset();
		ProgressCounter pc1 = new ProgressCounter("training "+innerLearner.getClass(), "epoch", numberOfEpochs);
		for (int i=0; i<numberOfEpochs; i++) {
			ProgressCounter pc2 = new ProgressCounter("training "+innerLearner.getClass(), "example", copy.size());
			for (Iterator<Example> j=copy.iterator(); j.hasNext(); ) {
				innerLearner.addExample( j.next() );
				pc2.progress();
			}
			pc2.finished();
			pc1.progress();
		}
		pc1.finished();
    innerLearner.completeTraining();
		classifier = innerLearner.getClassifier();
		return classifier;
	}
}
