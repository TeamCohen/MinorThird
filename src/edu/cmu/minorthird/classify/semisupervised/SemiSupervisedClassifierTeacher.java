package edu.cmu.minorthird.classify.semisupervised;

/**
 * Interface for something that trains semi-supervised classifiers.
 *
 * @author Edoardo Airoldi
 * Date: Jul 19, 2004
 */

public interface SemiSupervisedClassifierTeacher
{
   public SemiSupervisedClassifier train(SemiSupervisedClassifierLearner learner);
}

