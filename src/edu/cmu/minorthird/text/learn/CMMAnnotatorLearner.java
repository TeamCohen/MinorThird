/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.CMM;
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
	private ClassifierLearner classifierLearner;

	public CMMAnnotatorLearner(SpanFeatureExtractor fe,ClassifierLearner classifierLearner,int historySize)
	{
		super(fe,historySize);
		this.classifierLearner = classifierLearner;
	}

	/** Return the learned annotator
	 */
	public Annotator getAnnotator()
	{
		classifierLearner.setSchema(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
		for (Example.Looper i=seqData.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			classifierLearner.addExample( e );
		}
		Classifier classifier = classifierLearner.getClassifier();
		log.info("learned classifier: "+classifier);
		CMM cmm = new CMM(classifier,historySize,ExampleSchema.BINARY_EXAMPLE_SCHEMA);
		return new SequenceAnnotatorLearner.SequenceAnnotator( cmm, fe, annotationType );
		//return new SequenceAnnotator( new TrivialSequenceClassifier(classifier,historySize), fe, annotationType );
	}
}
