package edu.cmu.minorthird.classify.semisupervised;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Instance;

/**
 * Interface for a semi-supervised classifier.
 *
 * @author Edoardo Airoldi
 * Date: Jul 19, 2004
 */

public interface SemiSupervisedClassifier
{
   /** Return a predicted type for each element of the sequence. */
   public ClassLabel classification(Instance instance);

   /** Return some string that 'explains' the classification */
   public String explain(Instance instance);
}
