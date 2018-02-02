/* Copyright 2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.ui;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.text.learn.experiments.ExtractionEvaluation;

/**
 *
 * @author William Cohen
 */

public class LongTestPackage extends TestSuite{

	public LongTestPackage(String name){
		super(name);
	}

	public static TestSuite suite(){
		TestSuite suite=new TestSuite();
		// Start Test algorithms

		//Classification

		//Decision Tree/Directive
		suite.addTest(new LongUITest(TrainTestClassifier.class,new String[]{
				"-labels","cspace.1f3","-spanType","Req","-test","cspace.2f2",
				"-learner","new DecisionTreeLearner()"}){

			public void checkResult(Object result){
				Evaluation e=(Evaluation)result;
				assertEquals(.2,e.errorRate(),0.02);
				assertEquals(.65,e.f1(),0.02);
			}
		});

		suite.addTest(new LongUITest(TrainTestClassifier.class,new String[]{
				"-labels","cspace.1f3","-spanType","NotReq","-test","cspace.2f2",
				"-learner","new DecisionTreeLearner()"}){

			public void checkResult(Object result){
				Evaluation e=(Evaluation)result;
				assertEquals(.18,e.errorRate(),0.02);
				assertEquals(.87,e.f1(),0.02);
			}
		});

		//VotedPerceptron/Deliver
		suite.addTest(new LongUITest(TrainTestClassifier.class,new String[]{
				"-labels","cspace.1f3","-spanType","Dlv","-test","cspace.2f2",
				"-learner","new VotedPerceptron()"}){

			public void checkResult(Object result){
				Evaluation e=(Evaluation)result;
				assertEquals(.18,e.errorRate(),0.02);
				assertEquals(.88,e.f1(),0.02);
			}
		});

		suite.addTest(new LongUITest(TrainTestClassifier.class,new String[]{
				"-labels","cspace.1f3","-spanType","NotDlv","-test","cspace.2f2",
				"-learner","new VotedPerceptron()"}){

			public void checkResult(Object result){
				Evaluation e=(Evaluation)result;
				assertEquals(.18,e.errorRate(),0.02);
				assertEquals(.62,e.f1(),0.02);
			}
		});

		//SVM/Commit
		suite.addTest(new LongUITest(TrainTestClassifier.class,new String[]{
				"-labels","cspace.1f3","-spanType","Cmt","-test","cspace.2f2",
				"-learner","new SVMLearner()"}){

			public void checkResult(Object result){
				Evaluation e=(Evaluation)result;
				assertEquals(.15,e.errorRate(),0.02);
				assertEquals(.31,e.f1(),0.02);
			}
		});
		suite.addTest(new LongUITest(TrainTestClassifier.class,new String[]{
				"-labels","cspace.1f3","-spanType","NotCmt","-test","cspace.2f2",
				"-learner","new SVMLearner()"}){

			public void checkResult(Object result){
				Evaluation e=(Evaluation)result;
				assertEquals(.15,e.errorRate(),0.02);
				assertEquals(.91,e.f1(),0.02);
			}
		});

		//Extraction

		suite
				.addTest(new LongUITest(
						TrainTestExtractor.class,
						new String[]{
								"-labels",
								"cspace.07",
								"-test",
								"cspace.09",
								"-spanType",
								"true_name",
								"-learner",
								"new SequenceAnnotatorLearner(new CollinsPerceptronLearner(1,20), new Recommended.TokenFE(), new BeginContinueEndUniqueReduction())"}){

					public void checkResult(Object result){
						ExtractionEvaluation e=(ExtractionEvaluation)result;
						assertEquals(0.82,e.tokenF1(),0.02);
						assertEquals(0.75,e.spanF1(),0.02);
					}
				});
		suite
				.addTest(new LongUITest(
						TrainTestExtractor.class,
						new String[]{
								"-labels",
								"cspace.07",
								"-test",
								"cspace.09",
								"-spanType",
								"true_name",
								"-learner",
								"new SequenceAnnotatorLearner(new CRFLearner(1,20), new Recommended.TokenFE(), new BeginContinueEndUniqueReduction())"}){

					public void checkResult(Object result){
						ExtractionEvaluation e=(ExtractionEvaluation)result;
						assertEquals(0.88,e.tokenF1(),0.02);
						assertEquals(0.83,e.spanF1(),0.02);
					}
				});

		//Doesn't Work - ERROR
		/*suite.addTest( new LongUITest(TrainTestExtractor.class,
															new String[]{
																"-labels","cspace.07",
																"-test","cspace.09",
																"-spanType","true_name",
																"-learner", "new SegmentAnnotatorLearner(new SegmentCollinsPerceptronLearner(20), new Recommended.MultitokenSpanFE(), 4)"}) {
				public void checkResult(Object result) {
					ExtractionEvaluation e = (ExtractionEvaluation)result;
					assertEquals( 0.88, e.tokenF1(), 0.02 );
					assertEquals( 0.83, e.spanF1(), 0.02 );
				}
			});*/

		//End Test algorithms			
		return suite;
	}

	abstract public static class LongUITest extends TestCase{

		private String[] args;

		private Class<?> mainClass;

		public LongUITest(Class<?> mainClass,String[] args){
			super("doTest");
			this.mainClass=mainClass;
			this.args=args;
		}

		public void doTest(){
			try{
				UIMain m=(UIMain)mainClass.newInstance();
				m.callMain(args);
				checkResult(m.getMainResult());
			}catch(InstantiationException ex){
				throw new IllegalArgumentException(ex.toString());
			}catch(IllegalAccessException ex){
				throw new IllegalArgumentException(ex.toString());
			}
		}

		abstract public void checkResult(Object result);
	}

	static public void main(String[] argv){
		junit.textui.TestRunner.run(suite());
	}
}
