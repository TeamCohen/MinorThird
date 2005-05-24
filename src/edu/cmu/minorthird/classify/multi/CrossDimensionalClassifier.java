/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.gui.*;
import javax.swing.*;
import java.io.Serializable;


/** A Classifier composed of a bunch of binary classifiers, each of
 * which separates one class from the others.
 *
 * @author Cameron Williams
 */

public class CrossDimensionalClassifier implements Classifier,Visible,Serializable
{
    private MultiClassifier multiClassifier;

    /** Create a CrossDimensionalClassifier.
     */
    public CrossDimensionalClassifier(MultiClassifier multiClassifier) {
	this.multiClassifier = multiClassifier;
    }

    public MultiClassifier getMultiClassifier() { return multiClassifier; }

    public MultiClassLabel multiLabelClassification(Instance instance) 
    {
	MultiClassLabel predicted = multiClassifier.multiLabelClassification(instance);
	Instance annotatedInstance = new InstanceFromPrediction(instance, predicted.bestClassName());
	MultiClassLabel label = multiClassifier.multiLabelClassification(annotatedInstance);
	return label;
    }

    /** Give you the class label for the first dimension */
    public ClassLabel classification(Instance instance) 
    {
	return multiClassifier.classification(instance);
    }

    public String explain(Instance instance) 
    {
	return multiClassifier.explain(instance);
    }

    public String toString() {
	return multiClassifier.toString();
    }

    public Viewer toGUI()
    {
	final Viewer v = multiClassifier.toGUI();
	return v;
    }
}

