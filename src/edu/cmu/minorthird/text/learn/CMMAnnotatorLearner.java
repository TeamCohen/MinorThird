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

  public CMMAnnotatorLearner()
  {
    CMMAnnotatorLearner copy = (CMMAnnotatorLearner)SampleLearners.CMM;
    copyFrom(copy);
  }

  private void copyFrom(CMMAnnotatorLearner copy)
  {
    this.seqLearner = copy.seqLearner;
    this.seqData = copy.seqData;
    this.annotationType = copy.annotationType;
    this.fe = copy.fe;
  }

  protected Object clone() throws CloneNotSupportedException
  {
    CMMAnnotatorLearner newObj = new CMMAnnotatorLearner();
    newObj.copyFrom(this);
    return newObj;
  }
}
