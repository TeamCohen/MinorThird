/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.sequential.TrivialSequenceClassifier;
import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.learn.SequenceAnnotatorLearner;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;

/**
 * Learn a trivial sequence model from examples.  In this model, the
 * best classification of each token is assigned left-to-right, and
 * then made available as a feature.
 *
 * @author William Cohen
 */

public class TrivialSequenceAnnotatorLearner extends edu.cmu.minorthird.text.learn.SequenceAnnotatorLearner
{
	private ClassifierLearner classifierLearner;

	public TrivialSequenceAnnotatorLearner(
		edu.cmu.minorthird.text.learn.SpanFeatureExtractor fe,ClassifierLearner classifierLearner,int historySize)
	{
		super(fe,historySize);
		this.classifierLearner = classifierLearner;
	}

	public Annotator getAnnotator()
	{
		for (Example.Looper i=seqData.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			classifierLearner.addExample( e );
		}
		Classifier classifier = classifierLearner.getClassifier();
		return new edu.cmu.minorthird.text.learn.SequenceAnnotatorLearner.SequenceAnnotator( new TrivialSequenceClassifier(classifier,historySize), fe, annotationType );
	}
}
