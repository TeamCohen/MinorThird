/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;



/**
 * Simple abstract class, getBinaryClassifier() method for a
 * BinaryClassifierLearner, and also a batchTrainBinary() method.
 *
 * @author William Cohen
 */

public abstract class BatchBinaryClassifierLearner extends BatchClassifierLearner implements BinaryClassifierLearner
{
	final public void setSchema(ExampleSchema schema)
	{
		if (!ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema)) {
			throw new IllegalStateException("can only learn binary example data");
		}
	}

	/** Train a binary classifier. */
	final public BinaryClassifier batchTrainBinary(Dataset dataset) 
	{
		return (BinaryClassifier)batchTrain(dataset);
	}

	/** Get the last-trained a binary classifier. */
	final public BinaryClassifier getBinaryClassifier() 
	{
		return (BinaryClassifier)getClassifier();
	}

}
