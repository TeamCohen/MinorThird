package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.ExampleSchema;


/**
 * Interface for something that learns sequence classifiers.
 *
 * @author William Cohen
 */

public interface SequenceClassifierLearner
{
	public void setSchema(ExampleSchema schema);
}

