/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.Expt;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;

/**
 * Trains a MultiClassifierLearner using the information in a labeled Dataset.
 *
 * @author Cameron Williams
 *
 */
public class MultiDatasetClassifierTeacher extends MultiClassifierTeacher
{
    private MultiDataset dataset;
    private boolean activeLearning = false;

    public MultiDatasetClassifierTeacher(MultiDataset dataset) { this.dataset = dataset; }

    public MultiExampleSchema schema()
    {
	return dataset.getMultiSchema();
    }

    public MultiExample.Looper examplePool() 
    { 
	return activeLearning? 
	    new MultiExample.Looper(Collections.EMPTY_SET.iterator()) : dataset.multiIterator();
    }

    public Instance.Looper instancePool() 
    { 
	if (activeLearning) {
	    return new Instance.Looper(dataset.multiIterator());
	} else if (dataset instanceof MultiDataset) {
	    return ((MultiDataset)dataset).iteratorOverUnlabeled();
	} else {
	    return new Instance.Looper(Collections.EMPTY_SET);
	}
    }

    public MultiExample labelInstance(Instance query) 
    { 
	if (query instanceof MultiExample) return (MultiExample)query;
	else return null;
    }

    public boolean hasAnswers() 
    { 
	return activeLearning;
    }
}
