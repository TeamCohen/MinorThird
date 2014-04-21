package edu.cmu.minorthird.classify.semisupervised;

import edu.cmu.minorthird.classify.ExampleSchema;


/**
 * Interface for something that learns sequence classifiers.
 *
 * @author Edoardo Airoldi
 * Date: Jul 19, 2004
 */

public interface SemiSupervisedClassifierLearner
{
   //public SemiSupervisedClassifier batchTrain(SemiSupervisedDataset dataset);

   public void setSchema(ExampleSchema schema);
}
