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
    fe = SampleFE.BAG_OF_WORDS;
    seqLearner = new CollinsPerceptronLearner();
  }
}
