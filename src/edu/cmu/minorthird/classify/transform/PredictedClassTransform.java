/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.multi.*;

import java.io.Serializable;

/**
 * @author Cameron Williams
 * Date: October 11, 2005
 * Stores a learned multiClassifier and uses that to predict a multiLabel
 * for each instance.  Each label from the multiLabel is then added as
 * a feature to each instance.
 */

public class PredictedClassTransform extends AbstractInstanceTransform implements Serializable
{
    private MultiClassifier multiClassifier;

    public PredictedClassTransform(MultiClassifier multiClassifier) { this.multiClassifier = multiClassifier; }
    
    /*  Adds the predicted multiClassLabel as features to the instance */
    public Instance transform(Instance instance)
    {
	MultiClassLabel predicted = multiClassifier.multiLabelClassification(instance);
	Instance annotatedInstance = new InstanceFromPrediction(instance, predicted.bestClassName());
	return annotatedInstance;
    }
}