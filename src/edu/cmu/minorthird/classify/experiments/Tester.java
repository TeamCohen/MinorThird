/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.semisupervised.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.util.ProgressCounter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/** Test a classifier, in a number of ways.
 *
 * @author William Cohen
 */

public class Tester
{
	static private Logger log = Logger.getLogger(Tester.class);
	private static final boolean DEBUG = log.getEffectiveLevel().isGreaterOrEqual( Level.DEBUG );

	/** Do some sort of hold-out experiment, as determined by the splitter */
	static public Evaluation evaluate(ClassifierLearner learner,Dataset d,Splitter splitter)
	{
		Evaluation v = new Evaluation(d.getSchema()); 
		Dataset.Split s = d.split(splitter);
		ProgressCounter pc = new ProgressCounter("train/test","fold",s.getNumPartitions());
		for (int k=0; k<s.getNumPartitions(); k++) {
			Dataset trainData = s.getTrain(k);
			Dataset testData = s.getTest(k);
			log.info("splitting with "+splitter+", preparing to train on "+trainData.size()
							 +" and test on "+testData.size());
			Classifier c = new DatasetClassifierTeacher(trainData).train(learner);
			if (DEBUG) log.debug("classifier for fold "+(k+1)+"/"+s.getNumPartitions()+" is:\n" + c);
			v.extend( c, testData, k );
			log.info("splitting with "+splitter+", completed train-test round");
			pc.progress();
		}
		pc.finished();
		return v;
	}

	/** Do some sort of hold-out experiment, as determined by the splitter */
	static public Evaluation evaluate(SequenceClassifierLearner learner,SequenceDataset d,Splitter splitter)
	{
		Evaluation v = new Evaluation(d.getSchema()); 
		Dataset.Split s = d.split(splitter);
		ProgressCounter pc = new ProgressCounter("train/test","fold",s.getNumPartitions());
		for (int k=0; k<s.getNumPartitions(); k++) {
			SequenceDataset trainData = (SequenceDataset)s.getTrain(k);
			SequenceDataset testData = (SequenceDataset)s.getTest(k);
			log.info("splitting with "+splitter+", preparing to train on "+trainData.size()
							 +" and test on "+testData.size());
			SequenceClassifier c = new DatasetSequenceClassifierTeacher(trainData).train(learner);
			if (DEBUG) log.debug("classifier for fold "+(k+1)+"/"+s.getNumPartitions()+" is:\n" + c);
			v.extend( c, testData );
			log.info("splitting with "+splitter+", completed train-test round");
			pc.progress();
		}
		pc.finished();
		return v;
	}

   /** Do some sort of hold-out experiment, as determined by the splitter */
   static public Evaluation evaluate(SemiSupervisedClassifierLearner learner,SemiSupervisedDataset d,Splitter splitter)
   {
      Evaluation v = new Evaluation(d.getSchema());
      Dataset.Split s = d.split(splitter);
      ProgressCounter pc = new ProgressCounter("train/test","fold",s.getNumPartitions());
      for (int k=0; k<s.getNumPartitions(); k++) {
         SemiSupervisedDataset trainData = (SemiSupervisedDataset)s.getTrain(k); // Use the Interface ?
         SemiSupervisedDataset testData = (SemiSupervisedDataset)s.getTest(k);
         log.info("splitting with "+splitter+", preparing to train on "+trainData.size()
                      +" and test on "+testData.size());
         SemiSupervisedClassifier c = new DatasetSemiSupervisedClassifierTeacher(trainData).train(learner);
         if (DEBUG) log.debug("classifier for fold "+(k+1)+"/"+s.getNumPartitions()+" is:\n" + c);
         v.extend( c, testData, k );
         log.info("splitting with "+splitter+", completed train-test round");
         pc.progress();
      }
      pc.finished();
      return v;
   }

	/** Do a train and test experiment */
	static public Evaluation evaluate(ClassifierLearner learner,Dataset trainData,Dataset testData)
	{
		Splitter trainTestSplitter = new FixedTestSetSplitter(testData.iterator());
		return evaluate(learner,trainData,trainTestSplitter);
	}

	/** Do a train and test experiment */
	static public Evaluation 
	evaluate(SequenceClassifierLearner learner,SequenceDataset trainData,SequenceDataset testData)
	{
		Splitter trainTestSplitter = new FixedTestSetSplitter(testData.iterator());
		return evaluate(learner,trainData,trainTestSplitter);
	}

   /** Do a train and test experiment */
   static public Evaluation
   evaluate(SemiSupervisedClassifierLearner learner,SemiSupervisedDataset trainData,SemiSupervisedDataset testData)
   {
      Splitter trainTestSplitter = new FixedTestSetSplitter(testData.iterator());
      return evaluate(learner,trainData,trainTestSplitter);
   }

	/** Return the log loss on an example with known true class. */
	static public double logLoss(BinaryClassifier c, Example e)
	{
		return Math.log( 1.0 + Math.exp( e.getLabel().numericLabel() * c.score(e) ) ); 
	}

	/** Return the average log loss on a dataset. */ 
	static public double logLoss(BinaryClassifier c,Dataset d) 
	{
		double loss = 0;
		for (Example.Looper i=d.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			loss += logLoss(c, e);
		}
		return loss/d.size();
	}

	/** Return the error rate of a classifier on a dataset. */
	static public double errorRate(Classifier c,Dataset d) 
	{
		double errors = 0;
		for (Example.Looper i=d.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			if (! c.classification(e).isCorrect( e.getLabel())) {
				errors++;
			}
		}
		return errors/d.size();
	}
}
