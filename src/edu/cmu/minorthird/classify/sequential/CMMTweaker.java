/* Copyright 2004, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.classify.sequential;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;

/** 
 * Adjust the precision-recall of a CMM that is based on an array of hyperplanes,
 * by adjusting the bias term of the hyperplane associated with the background (NEG)
 * class.  This is intended mainly for adjusting precision-recall performance of
 * extractors learned by CollinsPerceptronLearner and CRFLearner.
 * 
 * @author William Cohen
*/

public class CMMTweaker implements SequenceConstants
{
	static private Logger log = Logger.getLogger(CMMTweaker.class);
  static private double oldBias = -1;
  static private double newBias = -1;

  /** Return the value of bias term before the last tweak.
   */
  public double oldBias() { return oldBias; }

  /** Return the value of bias term after tweaking.
   */
  public double newBias() { return newBias; }


  /** Return a copy of a CMM in which the one bias term, for the
   * hyperplane corresponding to the NEG class, has been changed.
   * The original CMM will not be modified.
   */
  public CMM tweak(CMM cmm,double bias)
  {
    Classifier c = cmm.getClassifier();
    if (c instanceof CollinsPerceptronLearner.MultiClassVPClassifier) {
      CollinsPerceptronLearner.MultiClassVPClassifier mc = 
        (CollinsPerceptronLearner.MultiClassVPClassifier)c;
      Hyperplane[] h = mc.getHyperplanes();
      ExampleSchema schema = mc.getSchema();
      int histSize = cmm.getHistorySize();
      return new CMM( new SequenceUtils.MultiClassClassifier(schema,tweak(h,schema,bias)),histSize,schema);
    } else if (c instanceof SequenceUtils.MultiClassClassifier) {
      SequenceUtils.MultiClassClassifier mc = (SequenceUtils.MultiClassClassifier)c;
      Classifier[] bc = mc.getBinaryClassifiers();
      Hyperplane[] h = new Hyperplane[bc.length];
      for (int i=0; i<bc.length; i++) {
        if (!(bc[i] instanceof Hyperplane)) {
          throw new IllegalArgumentException("invalid type of MultiClassClassifier: contains "+
                                             bc[i].getClass());
        }
        h[i] = (Hyperplane)bc[i];
      }
      ExampleSchema schema = mc.getSchema();
      int histSize = cmm.getHistorySize();
      return new CMM( new SequenceUtils.MultiClassClassifier(schema,tweak(h,schema,bias)),histSize,schema);
    } else {
      throw new IllegalArgumentException("invalid type of CMM classifier "+c.getClass());
    }
  }

  private Hyperplane[] tweak(Hyperplane[] h,ExampleSchema schema,double bias)
  {
    Hyperplane[] tweakedH = new Hyperplane[h.length];
    for (int i=0; i<h.length; i++) {
      tweakedH[i] = h[i];
    }
    int n = schema.getClassIndex(ExampleSchema.NEG_CLASS_NAME);
    tweakedH[n] = new Hyperplane();
    tweakedH[n].increment( h[n] );

    tweakedH[n].setBias( bias );
    oldBias = h[n].featureScore( Hyperplane.BIAS_TERM );
    newBias = bias;
    log.info("bias term for NEG hyperplane: "+oldBias+" => "+bias);

    return tweakedH;
  }
}
