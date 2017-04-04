/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.algorithms.knn.KnnLearner;
import edu.cmu.minorthird.classify.algorithms.linear.BalancedWinnow;
import edu.cmu.minorthird.classify.algorithms.linear.KernelVotedPerceptron;
import edu.cmu.minorthird.classify.algorithms.linear.MaxEntLearner;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.classify.algorithms.linear.PoissonLearner;
import edu.cmu.minorthird.classify.algorithms.linear.VitorBalancedWinnow;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.algorithms.linear.Winnow;
import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.experiments.SubsamplingCrossValSplitter;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.classify.semisupervised.SemiSupervisedNaiveBayesLearner;

/**
 *
 * @author William Cohen
 */

public class TestPackage extends TestSuite{

	private static Logger log=Logger.getLogger(TestPackage.class);

	public TestPackage(String name){
		super(name);
	}

	public static TestSuite suite(){

		TestSuite suite=new TestSuite();

		// these are error rates that the learners empirically obtain
		// if we don't get these, something has changed---which doesn't
		// necessarily mean there's a bug...

		suite.addTest(new LearnerTest("bayesUnlabeled",new SemiSupervisedNaiveBayesLearner(),0.0,0.0));
		suite.addTest(new LearnerTest("bayesExtreme",new PoissonLearner(),0.0,0.0));
		suite.addTest(new LearnerTest("bayesExtreme",new NaiveBayes(),0.5,0.5));
		suite.addTest(new LearnerTest("toy",new NaiveBayes(),1.0/7.0,1.0/7.0));
		suite.addTest(new LearnerTest("bayes",new PoissonLearner(),1.0/7.0,1.0/7.0));
		suite.addTest(new LearnerTest("toy",new BinaryBatchVersion(new VotedPerceptron()),0.0,1.0/7.0));
		suite.addTest(new LearnerTest("toy",new BinaryBatchVersion(new KernelVotedPerceptron(),5),0.0,0.25));
		suite.addTest(new LearnerTest("toy",new VotedPerceptron(),1.0/7.0,1.0/7.0));
		suite.addTest(new LearnerTest("toy",new KernelVotedPerceptron(),0.0,0.0));
		suite.addTest(new LearnerTest("toy",new Winnow(),0.0,0.0));
		suite.addTest(new LearnerTest("toy",new BalancedWinnow(),1.0/7.0,0.0));
		suite.addTest(new LearnerTest("toy",new VitorBalancedWinnow(),0.0,0.0));
		suite.addTest(new LearnerTest("toy",new DecisionTreeLearner(5,2),1.0/7.0,1.0/7.0));
		suite.addTest(new LearnerTest("toy",new KnnLearner(10),0.0,0.10));
		suite.addTest(new LearnerTest("toy3",new KnnLearner(10),0.20,0.10));
		suite.addTest(new LearnerTest("toy",new AdaBoost(new DecisionTreeLearner(5,2),10),1.0/7.0,1.0/7.0));
		suite.addTest(new LearnerTest("num",new DecisionTreeLearner(5,2),0.05,0.10));
		suite.addTest(new LearnerTest("sparseNum",new DecisionTreeLearner(5,2),0.0,0.10));
		suite.addTest(new LogisticRegressionTest());
		suite.addTest(new LearnerTest("toy",new SVMLearner(),0.0,0.0));
		suite.addTest(new LearnerTest("toy3",new SVMLearner(),0.0,0.1));
		suite.addTest(new XValTest(10,1));
		suite.addTest(new XValTest(3,5));
		suite.addTest(new XValTest(50,1,true));
		suite.addTest(new XValTest(3,25,true));
		return suite;
	}

	public static class LogisticRegressionTest extends TestCase{

		public LogisticRegressionTest(){
			super("doTest");
		}

		public void doTest(){
			MaxEntLearner lr=new MaxEntLearner();
			Dataset data=
					SampleDatasets.makeLogisticRegressionData(new Random(0),1000,0.2,0.3);
			Classifier c=lr.batchTrain(data);
			double error=Tester.errorRate(c,data);
			assertEquals(0.415,error,0.05);
		}
	}

	public static class XValTest extends TestCase{

		private int numSites,numPagesPerSite;

		private boolean subsample;

		public XValTest(int numSites,int numPagesPerSite){
			this(numSites,numPagesPerSite,false);
		}

		public XValTest(int numSites,int numPagesPerSite,boolean subsample){
			super("doTest");
			this.numSites=numSites;
			this.numPagesPerSite=numPagesPerSite;
			this.subsample=subsample;
		}

		public void doTest(){
			log.debug("[XValTest sites: "+numSites+" pages/site: "+numPagesPerSite+
					"]");
			List<Instance> list=new ArrayList<Instance>();
			for(int site=1;site<=numSites;site++){
				String subpop="www.site"+site+".com";
				for(int page=1;page<=numPagesPerSite;page++){
					MutableInstance inst=new MutableInstance("page"+page+".html",subpop);
					inst.addBinary(new Feature("site"+site+".page"+page));
					list.add(inst);
					log.debug("instance: "+inst);
				}
			}
			int totalSize=list.size();
			Splitter<Instance> splitter=null;
			if(subsample)
				splitter=new SubsamplingCrossValSplitter<Instance>(3,0.2);
			else
				splitter=new CrossValSplitter<Instance>(3);
			splitter.split(list.iterator());
			assertEquals(3,splitter.getNumPartitions());
			Set<Instance>[] train=new Set[3];
			Set<Instance>[] test=new Set[3];
			int totalTest=0;
			for(int i=0;i<3;i++){
				log.debug("partition "+(i+1)+":");
				train[i]=asSet(splitter.getTrain(i));
				test[i]=asSet(splitter.getTest(i));
				for(Iterator<Instance> j=test[i].iterator();j.hasNext();){
					Instance inst=j.next();
					log.debug("  test:  "+inst);
					assertTrue(!train[i].contains(inst));
				}
				log.debug("  -----\n  "+test[i].size()+" total");
				for(Iterator<Instance> j=train[i].iterator();j.hasNext();){
					Instance inst=j.next();
					log.debug("  train:  "+inst);
					assertTrue(!test[i].contains(inst));
				}
				log.debug("  -----\n  "+train[i].size()+" total");
				if(subsample){
					assertTrue(totalSize>=(train[i].size()+test[i].size()));
				}else{
					assertEquals(totalSize,train[i].size()+test[i].size());
				}
				totalTest+=test[i].size();
			}
			assertEquals(totalSize,totalTest);
		}

		private Set<Instance> asSet(Iterator<Instance> i){
			Set<Instance> set=new HashSet<Instance>();
			while(i.hasNext())
				set.add(i.next());
			return set;
		}
	}

	public static class LearnerTest extends TestCase{

		private ClassifierLearner learner;

		private double expectedTestError;

		private double allowedVariance;

		private String testName;

		public LearnerTest(String testName,ClassifierLearner learner,
				double expectedTestError,double allowedVariance){
			super("doTest");
			this.learner=learner;
			this.expectedTestError=expectedTestError;
			this.testName=testName;
			this.allowedVariance=allowedVariance;
		}

		public void doTest(){
			Dataset data=SampleDatasets.sampleData(testName,false);
			data.shuffle(new Random(0));
			ClassifierTeacher teacher=new DatasetClassifierTeacher(data);
			Classifier c=teacher.train(learner);
			log.debug("classifier is "+c);
			System.out.println("classifier is "+c);
			Dataset testSet=SampleDatasets.sampleData(testName,true);
			double actualTestError=Tester.errorRate(c,testSet);
			log.debug("error of "+learner+" is "+actualTestError);
			System.out.println("error of "+learner+" is "+actualTestError);
			assertEquals(expectedTestError,actualTestError,allowedVariance+0.001);
		}
	}

	static public void main(String[] argv){
		junit.textui.TestRunner.run(suite());
	}
}
