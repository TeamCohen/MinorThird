/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.text.Annotator;
import org.apache.log4j.Logger;

/**
 * Learn a conditional markov model from examples.
 *
 * @author William Cohen
 */

public class CMMAnnotatorLearner extends SequenceAnnotatorLearner
{
	private static Logger log = Logger.getLogger(CMMAnnotatorLearner.class);

	public CMMAnnotatorLearner(SpanFeatureExtractor fe,ClassifierLearner classifierLearner,int historySize)
	{
		super(new CMMLearner(classifierLearner,historySize),fe,historySize);
	}
}
