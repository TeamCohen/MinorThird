/* Copyright 2004, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import java.io.*;
import java.util.*;

/** Allows one to adjust the parameters of a learned extractor. 
 */

public class ExtractorTweaker
{
  private CMMTweaker cmmTweaker = new CMMTweaker();

  /** Return the value of bias term before the last tweak.
   */
  public double oldBias() { return cmmTweaker.oldBias(); }

  /** Return the value of bias term after the last tweak.
   */
  public double newBias() { return cmmTweaker.newBias(); }

  /** Return a modified copy of the annotator.  Only works for annotators
   * learned by the voted perceptron and/or CRF learners.
   */
  public ExtractorAnnotator tweak(ExtractorAnnotator annotator,double bias)
  {
    if (annotator instanceof SequenceAnnotatorLearner.SequenceAnnotator) {
      SequenceAnnotatorLearner.SequenceAnnotator sa = (SequenceAnnotatorLearner.SequenceAnnotator)annotator;
      SequenceClassifier sc = (SequenceClassifier) sa.getSequenceClassifier();
      if ((sc instanceof CMM)) {
        CMM cmm = (CMM)sc;
        return new 
          SequenceAnnotatorLearner.SequenceAnnotator(
            cmmTweaker.tweak(cmm,bias),
            sa.getSpanFeatureExtractor(),
            sa.getReduction(),
            sa.getSpanType()
          );
      } else {
        throw new IllegalArgumentException("can't tweak annotator based on sequence classifier of type "+
                                           sc.getClass());
      }
    } else {
      throw new IllegalArgumentException("can't tweak annotator of type "+
                                         annotator.getClass());
    }
  }

	/** 
	 */
	public static void main(String[] args) throws Exception
	{
		if (args.length<2) {
			System.out.println("usage: annotator-file learned-extractor bias [new-annotator-file]");
		} else {
			ExtractorAnnotator annotator = (ExtractorAnnotator)IOUtil.loadSerialized(new File(args[0]));
      double bias = StringUtil.atof(args[1]);
      ExtractorAnnotator tweaked = new ExtractorTweaker().tweak(annotator,bias);
      //new ViewerFrame("original",  new SmartVanillaViewer(annotator));
      //new ViewerFrame("tweaked",  new SmartVanillaViewer(tweaked));
      if (args.length>=3) {
        IOUtil.saveSerialized((Serializable)tweaked, new File(args[2]));
      }
		}
	}
}
