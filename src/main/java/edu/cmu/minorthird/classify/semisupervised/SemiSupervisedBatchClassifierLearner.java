
package edu.cmu.minorthird.classify.semisupervised;

import java.util.Iterator;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Instance;


/**
 * Abstract ClassifierLearner which instantiates the teacher-learner protocol
 * so as to implement a standard batch learner.
 *
 * @author Edoardo Airoldi
 *
 */

public abstract class SemiSupervisedBatchClassifierLearner implements ClassifierLearner
{
   private SemiSupervisedDataset dataset = new SemiSupervisedDataset();

   /** This variable saves the last classifier produced by batchTrain.
    * If it is non-null, then it will be returned by class to
    * getClassifier().  Implementations of batchTrain should save the
    * returned classifier to avoid extra work.
    */
   protected Classifier classifier = null;

   @Override
	final public void reset() {
      dataset = new SemiSupervisedDataset();
      classifier = null;
   }
   @Override
	final public boolean hasNextQuery() { return false; }
   @Override
	final public Instance nextQuery() { return null; }
   @Override
	final public void addExample(Example answeredQuery) { ((Dataset)dataset).add(answeredQuery); classifier=null; }
   @Override
	final public void completeTraining() { classifier = batchTrain(dataset); }

   @Override
	final public Classifier getClassifier() {
      if (classifier==null) classifier = batchTrain(dataset);
      return classifier;
   }


   /** Enables control on number of classes allowed to be passed to the learner */
   @Override
	abstract public void setSchema(ExampleSchema schema);

   /** subclasses should use this method to get the unlabeled examples available for
    * semi-supervised learning.
    */
   @Override
	abstract public void setInstancePool(Iterator<Instance> i);

   /** subclasses should use this method to implement a batch supervised learning algorithm.
    */
   abstract public Classifier batchTrain(SemiSupervisedDataset dataset);
}
