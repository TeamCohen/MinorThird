/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;

/**
 * A Classifier which associates instances with a real number.
 * Positive numbers indicate a positive class, negative numbers
 * indicate a negative class.
 * 
 * @author William Cohen
 */

abstract public class BinaryClassifier implements Classifier,Serializable
{
	static private final long serialVersionUID = 1;
	public ClassifierLearner classifierLearner = null;

	public void setClassifierLearner(ClassifierLearner cl) {
		this.classifierLearner = cl;
	}

	public ClassifierLearner getClassifierLearner() {
		return classifierLearner;
	}

	@Override
	public ClassLabel classification(Instance instance)
	{
		double s = score(instance);
		return s>=0 ? ClassLabel.positiveLabel(s) : ClassLabel.negativeLabel(s);
	}

	/** Get the weight for an instance being in the positive class. */
	abstract public double score(Instance instance);
}
