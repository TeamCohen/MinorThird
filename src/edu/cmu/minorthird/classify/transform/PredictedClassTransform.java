/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import java.io.Serializable;

import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.multi.InstanceFromPrediction;
import edu.cmu.minorthird.classify.multi.MultiClassLabel;
import edu.cmu.minorthird.classify.multi.MultiClassifier;

/**
 * @author Cameron Williams
 * Date: October 11, 2005
 * Stores a learned multiClassifier and uses that to predict a multiLabel
 * for each instance.  Each label from the multiLabel is then added as
 * a feature to each instance.
 */

public class PredictedClassTransform extends AbstractInstanceTransform
		implements Serializable{

	static final long serialVersionUID=20080201L;
	
	private MultiClassifier multiClassifier;

	public PredictedClassTransform(MultiClassifier multiClassifier){
		this.multiClassifier=multiClassifier;
	}

	/*  Adds the predicted multiClassLabel as features to the instance */
	@Override
	public Instance transform(Instance instance){
		MultiClassLabel predicted=
				multiClassifier.multiLabelClassification(instance);
		Instance annotatedInstance=
				new InstanceFromPrediction(instance,predicted.bestClassName());
		return annotatedInstance;
	}
}