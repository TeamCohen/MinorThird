package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.StackedGraphicalLearning.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.*;

/**
 * Implements the teacher's side of the learner-teacher protocol for SGM.
 *
 * @author Zhenzhen Kou
 *
 */
public abstract class StackedClassifierTeacher extends ClassifierTeacher
{

	/** Train a StackedGraphicalLearner and return the learned Classifier, using
	 * some unspecified source of information to get labels.  
	 */

	final public Classifier trainStacked(StackedBatchClassifierLearner learner) 
	{
		// initialize the learner for a new problem
		learner.reset();

		// tell learner the schema of examples
		learner.setSchema( schema() );
		learner.RelDataset.setAggregators(this.getAggregators());
		learner.RelDataset.setLinksMap(this.getLinksMap());
		// provide unlabeled examples to the learner, for unsupervised
		// training, semi-supervised training, or active learner
		learner.setInstancePool( instancePool() );

		// passive learning from already-available labeled data
		for (Example.Looper i=examplePool(); i.hasNext(); ) {
			learner.addExample( i.nextExample() );
		}
		// active learning 
		while (learner.hasNextQuery() && hasAnswers()) {
			Instance query = learner.nextQuery();
			Example answeredQuery = labelInstance(query);
			if (answeredQuery!=null) {
				learner.addExample( answeredQuery );
			}
		}
		// signal that there's no more data available
		learner.completeTraining();

		// final result
		return learner.getClassifier();
	}
	

	/** The linkMaps for stacked graphical learning
	 */
	abstract protected HashMap getLinksMap();

	/** The Aggregators for stacked graphical learning
	 */
	abstract protected HashMap getAggregators();

}
