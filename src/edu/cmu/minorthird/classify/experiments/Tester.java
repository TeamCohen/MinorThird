/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.experiments;

import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetClassifierTeacher;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.StackedDatasetClassifierTeacher;
import edu.cmu.minorthird.classify.multi.MultiClassifier;
import edu.cmu.minorthird.classify.multi.MultiDataset;
import edu.cmu.minorthird.classify.multi.MultiDatasetClassifierTeacher;
import edu.cmu.minorthird.classify.multi.MultiEvaluation;
import edu.cmu.minorthird.classify.multi.MultiExample;
import edu.cmu.minorthird.classify.relational.RealRelationalDataset;
import edu.cmu.minorthird.classify.relational.StackedBatchClassifierLearner;
import edu.cmu.minorthird.classify.relational.StackedGraphicalLearner;
import edu.cmu.minorthird.classify.semisupervised.DatasetSemiSupervisedClassifierTeacher;
import edu.cmu.minorthird.classify.semisupervised.SemiSupervisedClassifier;
import edu.cmu.minorthird.classify.semisupervised.SemiSupervisedClassifierLearner;
import edu.cmu.minorthird.classify.semisupervised.SemiSupervisedDataset;
import edu.cmu.minorthird.classify.sequential.DatasetSequenceClassifierTeacher;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;
import edu.cmu.minorthird.classify.sequential.SequenceClassifierLearner;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.classify.transform.AbstractInstanceTransform;
import edu.cmu.minorthird.classify.transform.PredictedClassTransform;
import edu.cmu.minorthird.classify.transform.TransformingMultiClassifier;
import edu.cmu.minorthird.util.ProgressCounter;

/** Test a classifier, in a number of ways.
 *
 * @author William Cohen
 */

public class Tester
{
	static private Logger log = Logger.getLogger(Tester.class);
	private static final boolean DEBUG = log.getEffectiveLevel().isGreaterOrEqual( Level.DEBUG );


	/** Do some sort of hold-out experiment, as determined by the splitter */
	static public Evaluation evaluate(StackedBatchClassifierLearner learner,RealRelationalDataset d,Splitter<Example> splitter, String stacked)
	{
		
		Evaluation v = new Evaluation(d.getSchema()); 
		RealRelationalDataset.Split s = d.split(splitter);
		//System.out.println("Test Splitter: "+splitter);
		ProgressCounter pc = new ProgressCounter("train/test","fold",s.getNumPartitions());
		for (int k=0; k<s.getNumPartitions(); k++) {
			RealRelationalDataset trainData = (RealRelationalDataset)s.getTrain(k);
			RealRelationalDataset testData = (RealRelationalDataset)s.getTest(k);
			
			log.info("splitting with "+splitter+", preparing to train on "+trainData.size()
							 +" and test on "+testData.size());
			
			Classifier c = new StackedDatasetClassifierTeacher(trainData).trainStacked(learner);
			if (DEBUG) log.debug("classifier for fold "+(k+1)+"/"+s.getNumPartitions()+" is:\n" + c);
			v.extend4SGM( (StackedGraphicalLearner.StackedGraphicalClassifier)c, testData, k );
			log.info("splitting with "+splitter+", completed train-test round");
			pc.progress();
		}
		pc.finished();
		return v;
	}
	
	
	/** Do some sort of hold-out experiment, as determined by the splitter */
	static public Evaluation evaluate(ClassifierLearner learner,Dataset d,Splitter<Example> splitter)
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
	static public MultiEvaluation multiEvaluate(ClassifierLearner learner,MultiDataset d,Splitter<MultiExample> splitter)
	{
	    return multiEvaluate(learner, d, splitter, false);
	}

    /** Do some sort of hold-out experiment, as determined by the splitter */
    static public MultiEvaluation multiEvaluate(ClassifierLearner learner,MultiDataset d,Splitter<MultiExample> splitter, boolean cross)
	{
		MultiEvaluation v = new MultiEvaluation(d.getMultiSchema()); 
		MultiDataset.MultiSplit s = d.MultiSplit(splitter);
		ProgressCounter pc = new ProgressCounter("train/test","fold",s.getNumPartitions());
		for (int k=0; k<s.getNumPartitions(); k++) {		    
		    //for (int k=0; k<1; k++) {
			MultiDataset trainData = s.getTrain(k);
			if(cross) trainData=trainData.annotateData();
			MultiDataset testData = s.getTest(k);
			log.info("splitting with "+splitter+", preparing to train on "+trainData.size()
							 +" and test on "+testData.size());
			MultiClassifier c = new MultiDatasetClassifierTeacher(trainData).train(learner);
			//if(cross) testData=testData.annotateData(c);
			if(cross) {
			    AbstractInstanceTransform transformer = new PredictedClassTransform(c);
			    c = new TransformingMultiClassifier(c, transformer);
			}
			if (DEBUG) log.debug("classifier for fold "+(k+1)+"/"+s.getNumPartitions()+" is:\n" + c);
			v.extend( c, testData);
			log.info("splitting with "+splitter+", completed train-test round");
			pc.progress();
		}
		pc.finished();
		return v;
	}    

	/** Do some sort of hold-out experiment, as determined by the splitter */
	static public Evaluation evaluate(SequenceClassifierLearner learner,SequenceDataset d,Splitter<Example[]> splitter)
	{
		Evaluation v = new Evaluation(d.getSchema());
		Dataset.Split s = d.splitSequence(splitter);
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
   static public Evaluation evaluate(SemiSupervisedClassifierLearner learner,SemiSupervisedDataset d,Splitter<Example> splitter)
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
		Splitter<Example> trainTestSplitter = new FixedTestSetSplitter<Example>(testData.iterator());
		return evaluate(learner,trainData,trainTestSplitter);
	}

	/** Do a train and test experiment */
	static public Evaluation 
	evaluate(SequenceClassifierLearner learner,SequenceDataset trainData,SequenceDataset testData)
	{
		Splitter<Example[]> trainTestSplitter = new FixedTestSetSplitter<Example[]>(testData.sequenceIterator());
		return evaluate(learner,trainData,trainTestSplitter);
	}

   /** Do a train and test experiment */
   static public Evaluation
   evaluate(SemiSupervisedClassifierLearner learner,SemiSupervisedDataset trainData,SemiSupervisedDataset testData)
   {
      Splitter<Example> trainTestSplitter = new FixedTestSetSplitter<Example>(testData.iterator());
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
		for (Iterator<Example> i=d.iterator(); i.hasNext(); ) {
			Example e = i.next();
			loss += logLoss(c, e);
		}
		return loss/d.size();
	}

	/** Return the error rate of a classifier on a dataset. */
	static public double errorRate(Classifier c,Dataset d) 
	{
		double errors = 0;
		for (Iterator<Example> i=d.iterator(); i.hasNext(); ) {
			Example e = i.next();
			if (! c.classification(e).isCorrect( e.getLabel())) {
				errors++;
			}
		}
		return errors/d.size();
	}
}
