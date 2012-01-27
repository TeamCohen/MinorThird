/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;



/**
 * Trains a SequenceClassifierLearner using the information in a labeled Dataset.
 *
 * @author William Cohen
 *
 */
public class DatasetSequenceClassifierTeacher implements SequenceClassifierTeacher
{
	private SequenceDataset dataset;

	public DatasetSequenceClassifierTeacher(SequenceDataset dataset) { this.dataset = dataset; }

	/** Currently, only support batch learners.
	 */
	@Override
	public SequenceClassifier train(SequenceClassifierLearner learner)
	{
		BatchSequenceClassifierLearner batchLearner = (BatchSequenceClassifierLearner)learner;
		return batchLearner.batchTrain(dataset);
	}
}
