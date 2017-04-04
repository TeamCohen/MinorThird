/* Copyright 2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.ui;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.TestUI;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.learn.experiments.ExtractionEvaluation;

/**
 *
 * @author William Cohen
 */

public class TestPackage extends TestSuite{

	static Logger log=Logger.getLogger(TestUI.class);

	public TestPackage(String name) { super(name); }

	public static TestSuite suite()
	{
		TestSuite suite = new TestSuite();
		//
		// test ui 
		//
		suite.addTest( new UITest(TrainTestClassifier.class,
				new String[]{
			"-labels","sample3.train",
			"-spanType","fun",
			"-splitter","k5",
			"-showTestDetails", "false",
			"-learner","new NaiveBayes()"}) {
			public void checkResult(Object result) {
				Evaluation e = (Evaluation)result;
				assertEquals( 1.0/9.0, e.errorRate(), 0.2 );
			}
		});
		suite.addTest( new UITest(TrainClassifier.class,
				new String[]{
			"-labels","sample3.train",
			"-spanType","fun",
			"-saveAs","tmp.ann",
			"-learner","new NaiveBayes()"}) {
			public void checkResult(Object result) {
				assertTrue( result instanceof Classifier);
			}
		});
		suite.addTest( new UITest(TestClassifier.class,
				new String[]{
			"-labels","sample3.train",
			"-showTestDetails", "false",
			"-spanType","fun",
			"-loadFrom","tmp.ann"}) {
			public void checkResult(Object result) {
				Evaluation e = (Evaluation)result;
				assertEquals( 0.0, e.errorRate(), 0.2 );
			}
		});

		//
		// test ui routines on span candidate-filtering tasks
		//
		suite.addTest( new UITest(TrainTestClassifier.class,
				new String[]{
			"-labels","sample1.train",
			"-candidateType","bigram",
			"-spanType","trueName",
			"-showTestDetails", "false",
			"-splitter","k5",
			"-fe", "Recommended.MultitokenSpanFE()",
			"-learner","new NaiveBayes()"}) {
			public void checkResult(Object result) {
				Evaluation e = (Evaluation)result;
				assertEquals( 0.75, e.maxF1(), 0.25 );
			}
		});
		suite.addTest( new UITest(TrainClassifier.class,
				new String[]{
			"-labels","sample1.train",
			"-candidateType","bigram",
			"-spanType","trueName",
			"-saveAs","tmp.ann",
			"-fe", "Recommended.MultitokenSpanFE()",
			"-learner","new NaiveBayes()"}) {
			public void checkResult(Object result) {
				assertTrue( result instanceof Classifier);
			}
		});
		suite.addTest( new UITest(TestClassifier.class,
				new String[]{
			"-labels","sample1.test",
			"-showTestDetails", "false",
			"-candidateType","bigram",
			"-spanType","trueName",
			"-loadFrom","tmp.ann"}) {
			public void checkResult(Object result) {
				Evaluation e = (Evaluation)result;
				assertEquals( 8.0/9.0, e.maxF1(), 0.15 );
			}
		});

		//
		// test on extraction tasks
		// 
		suite.addTest( new UITest(TrainTestExtractor.class,
				new String[]{
			"-labels","sample1.train",
			"-spanType","trueName",
			"-showTestDetails", "false",
			"-learner", "new Recommended.VPHMMLearner()",
			"-splitter","k8"}) {
			public void checkResult(Object result) {
				ExtractionEvaluation e = (ExtractionEvaluation)result;
				//assertEquals( 0.5, e.spanF1(), 0.125 );
				assertEquals(0.5,e.spanF1(),0.25);
			}
		});
		suite.addTest( new UITest(TrainTestExtractor.class,
				new String[]{
			"-labels","sample1.train",
			"-spanType","trueName",
			"-showTestDetails", "false",
			"-learner", "new Recommended.VPHMMLearner()",
			"-test","sample1.test"}) {
			public void checkResult(Object result) {
				ExtractionEvaluation e = (ExtractionEvaluation)result;
				assertEquals( 0.75, e.spanF1(), 0.125 );
			}
		});
		suite.addTest( new UITest(TrainExtractor.class,
				new String[]{
			"-labels","sample1.train",
			"-spanType","trueName",
			"-learner", "new Recommended.VPHMMLearner()",
			"-saveAs","tmp.ann"}) {
			public void checkResult(Object result) {
				assertTrue( result instanceof Annotator );
			}
		});
		suite.addTest( new UITest(TestExtractor.class,
				new String[]{
			"-labels","sample1.test",
			"-spanType","trueName",
			"-loadFrom","tmp.ann"}) {
			public void checkResult(Object result) {
				ExtractionEvaluation e = (ExtractionEvaluation)result;
				//assertEquals( 0.5, e.spanF1(), 0.1 );
				assertEquals( 0.5, e.spanF1(), 0.25 );
			}
		});

		return suite;
	}


	// test the TrainTestClassifier package
	abstract public static class UITest extends TestCase
	{
		private String[] args;
		private Class<?> mainClass;
		public UITest(Class<?> mainClass,String[] args) 
		{ 
			super("doTest"); 
			this.mainClass = mainClass;
			this.args = args;
		}
		public void doTest()
		{
			try {
				UIMain m = (UIMain)mainClass.newInstance();
				m.callMain(args);
				checkResult(m.getMainResult());
			} catch (InstantiationException ex) {
				throw new IllegalArgumentException(ex.toString());
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex.toString());
			}
		}
		abstract public void checkResult(Object result);
	}
	static public void main(String[] argv) {
		junit.textui.TestRunner.run(suite());
	}
}
