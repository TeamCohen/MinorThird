package edu.cmu.minorthird.classify.semisupervised;

import java.util.Iterator;

import edu.cmu.minorthird.classify.Instance;


/**
 * Trains a SemiSuperisedClassifierLearner using the information in a labeled Dataset.
 *
 * @author Edoardo Airoldi
 * Date: Jul 19, 2004
 */

public class DatasetSemiSupervisedClassifierTeacher implements SemiSupervisedClassifierTeacher
{
   private SemiSupervisedDataset dataset;

   public DatasetSemiSupervisedClassifierTeacher(SemiSupervisedDataset dataset) { this.dataset=dataset; }


   /** Currently, only support batch learners.
    */
   @Override
	public SemiSupervisedClassifier train(SemiSupervisedClassifierLearner learner)
   {
      //System.out.println("in SemiSupervisedClassifier.train()");
      SemiSupervisedBatchClassifierLearner batchLearner = (SemiSupervisedBatchClassifierLearner)learner;
      return (SemiSupervisedClassifier)batchLearner.batchTrain(dataset);
   }

   public Iterator<Instance> instancePool()
   {
      //System.out.println("in instancePool()");
      // (Edoardo Airoldi)  this itearator is empty whenever there are no
      // unlabeled examples available for semi-supervised learning.
      return dataset.iteratorOverUnlabeled();
   }

}

