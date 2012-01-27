/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.algorithms.svm.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.util.gui.*;

import org.apache.log4j.*;

/**
 * Online version of a BatchClassifierLearner. 
 *
 * @author William Cohen
 */

public class OnlineVersion extends OnlineClassifierLearner
{
	private static Logger log = Logger.getLogger(OnlineVersion.class);

	private BatchClassifierLearner innerLearner;
	private OnlineClassifierLearner bootstrapLearner;
	private double loadFactor; 
	private int minBatchTrainingSize;
	private Classifier storedClassifier;
	private int lastTrainingSetSize;
	private Dataset dataset;

	/**
	 * Emulate on-line learning with a batch algorithm.
	 *
	 * @param innerLearner batch learning algorithm
	 * @param loadFactor re-train batch algorithm when number of available
	 * examples is loadFactor * M, where M is the number of examples
	 * available at the last training round.
	 * @param bootstrapLearner on-line learner used for the first few rounds
	 * @param minBatchTrainingSize use online bootstrapLearner until minBatchTrainingSize examples are available.
	 */
	public OnlineVersion(
		BatchClassifierLearner innerLearner,double loadFactor,
		OnlineClassifierLearner bootstrapLearner,int minBatchTrainingSize)
	{
		this.innerLearner = innerLearner;
		this.loadFactor = loadFactor;
		this.bootstrapLearner = bootstrapLearner;
		this.minBatchTrainingSize = minBatchTrainingSize;
		reset();
	}
	public OnlineVersion(BatchClassifierLearner innerLearner,double loadFactor)	
	{	
		this(innerLearner,loadFactor,new VotedPerceptron(),10);	
	}
	public OnlineVersion(BatchClassifierLearner innerLearner)
	{	
		this(innerLearner,1.5);
	}
	public OnlineVersion() {	
		this(new SVMLearner()); 
	}

	public BatchClassifierLearner getInnerLearner() { return innerLearner; }
	public void setInnerLearner(BatchClassifierLearner learner) { this.innerLearner=learner; }
	public OnlineClassifierLearner getBootstrapLearner() { return bootstrapLearner; }
	public void setBootstrapLearner(OnlineClassifierLearner learner) { this.bootstrapLearner = learner; }
	public double getBatchLoadFactor() { return loadFactor; }
	public void setBatchLoadFactor(double d) { loadFactor=d; }
	public int getMinBatchTrainingSize() { return minBatchTrainingSize; }
	public void setMinBatchTrainingSize(int m) { minBatchTrainingSize=m; }

	@Override
	final public void setSchema(ExampleSchema schema)	{	
		innerLearner.setSchema(schema);	
		bootstrapLearner.setSchema(schema); 
	}
	
	@Override
	final public ExampleSchema getSchema(){
		return innerLearner.getSchema();
	}

	@Override
	final public void reset()
	{
		storedClassifier = null;
		lastTrainingSetSize = 0; 
		dataset = new BasicDataset();
		innerLearner.reset();
		bootstrapLearner.reset();
	}

	@Override
	final public void addExample(Example example)
	{
		dataset.add(example);
		if (dataset.size()<minBatchTrainingSize) {
			bootstrapLearner.addExample(example);
		}
	}

	@Override
	final public void completeTraining()
	{
		new ViewerFrame("compete data",dataset.toGUI());
		if (dataset.size()>lastTrainingSetSize || storedClassifier==null) {
			log.info("final training for "+innerLearner+" on "+dataset.size()+" examples");
			storedClassifier = innerLearner.batchTrain(dataset);
			new ViewerFrame("classifier", new SmartVanillaViewer(storedClassifier));
			lastTrainingSetSize = dataset.size();
		}
	}

	@Override
	final public Classifier getClassifier()
	{
		if (dataset.size() < minBatchTrainingSize) {
			return bootstrapLearner.getClassifier();
		} else if (dataset.size() > lastTrainingSetSize*loadFactor || storedClassifier==null) {
			log.info("re-training "+innerLearner+" on "+dataset.size()+" examples");
			storedClassifier = innerLearner.batchTrain(dataset);
			log.info("batch classifier is "+storedClassifier);
			lastTrainingSetSize = dataset.size();
			return storedClassifier;
		} else {
			return storedClassifier;
		}
	}
}
