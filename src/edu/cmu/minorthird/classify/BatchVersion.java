/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.util.ProgressCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch version of an OnlineClassifierLearner. 
 *
 * @author William Cohen
 */

public class BatchVersion extends BatchClassifierLearner
{
	private OnlineClassifierLearner innerLearner;
	private int numberOfEpochs = 1;
	private List exampleList = new ArrayList();

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

	final public void setSchema(ExampleSchema schema)	{	innerLearner.setSchema(schema);	}

	public Classifier batchTrain(Dataset dataset)
	{
		Dataset copy = dataset.shallowCopy();
		copy.shuffle();
		innerLearner.reset();
		ProgressCounter pc1 = new ProgressCounter("training "+innerLearner.getClass(), "epoch", numberOfEpochs);
		for (int i=0; i<numberOfEpochs; i++) {
			ProgressCounter pc2 = new ProgressCounter("training "+innerLearner.getClass(), "example", copy.size());
			for (Example.Looper j=copy.iterator(); j.hasNext(); ) {
				innerLearner.addExample( j.nextExample() );
				pc2.progress();
			}
			pc2.finished();
			pc1.progress();
		}
		pc1.finished();
		classifier = innerLearner.getClassifier();
		return classifier;
	}
}
