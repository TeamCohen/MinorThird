package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.learn.SequenceAnnotatorLearner;
import edu.cmu.minorthird.text.learn.SampleFE;
import edu.cmu.minorthird.classify.sequential.CollinsPerceptronLearner;

/**
 * This class...
 * @author ksteppe
 */
public class CollinsSequenceAnnotatorLearner extends SequenceAnnotatorLearner
{
  public CollinsSequenceAnnotatorLearner()
  {
    fe = new SampleFE.ExtractionFE();
    seqLearner = new CollinsPerceptronLearner();
  }
}
