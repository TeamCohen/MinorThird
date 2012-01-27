/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;



/**
 * Simple abstract class, getBinaryClassifier() method for a
 * BinaryClassifierLearner, and also a batchTrainBinary() method.
 *
 * @author William Cohen
 */

public abstract class BatchBinaryClassifierLearner extends BatchClassifierLearner implements BinaryClassifierLearner{
	
	@Override
	final public void setSchema(ExampleSchema schema){
		if(!ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema)){
			throw new IllegalStateException("Can only learn binary example data.");
		}
	}
	
	@Override
	final public ExampleSchema getSchema(){
		return ExampleSchema.BINARY_EXAMPLE_SCHEMA;
	}

	/** Train a binary classifier. */
	final public BinaryClassifier batchTrainBinary(Dataset dataset){
		return (BinaryClassifier)batchTrain(dataset);
	}

	/** Get the last-trained a binary classifier. */
	@Override
	final public BinaryClassifier getBinaryClassifier(){
		return (BinaryClassifier)getClassifier();
	}

}
