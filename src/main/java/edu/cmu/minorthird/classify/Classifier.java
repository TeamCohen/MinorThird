/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

/**
 * Interface for a classifier. This is specialized to BinaryClassifier and KWayClassifier.
 *
 * @author William Cohen
 */

public interface Classifier 
{
	/** Return a predicted type for the span, as a class label. */
	public ClassLabel classification(Instance instance);

	/** Return some string that 'explains' the classification */
	public String explain(Instance instance);

    /** Return an Explanation for the classification */
    public Explanation getExplanation(Instance instance);
}

