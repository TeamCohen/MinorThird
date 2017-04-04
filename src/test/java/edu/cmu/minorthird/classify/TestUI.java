/* Copyright 2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;

/**
 *
 * @author William Cohen
 */

public class TestUI extends TestSuite
{
  //private static Logger log = Logger.getLogger(TestUI.class);

  public TestUI(String name) { super(name); }

  public static TestSuite suite()
  {
    TestSuite suite = new TestSuite();
		//
		// test ui 
		//
		suite.addTest( new UITest(new String[]{
			"-data", "sample:toy.train", "-learner", "new NaiveBayes()","-splitter","k5"}) {
				public void checkResult(Object result) {
					assertTrue(result instanceof Evaluation);
					Evaluation e = (Evaluation)result;
					assertEquals( 0.92, e.f1(),  0.1 );
				}
			});
		suite.addTest( new UITest(new String[]{
			"-op", "train", "-data", "sample:toy.train", "-learner", "new NaiveBayes()", "-saveAs", "tmp.cls"}) {
				public void checkResult(Object result) {
					assertTrue(result instanceof Classifier);
				}
			});
		suite.addTest( new UITest(new String[]{
			"-op", "test", "-data", "sample:toy.test", "-classifierFile", "tmp.cls"}) {
				public void checkResult(Object result) {
					assertTrue(result instanceof Evaluation);
					Evaluation e = (Evaluation)result;
					assertEquals( 0.9, e.f1(),  0.2 );
				}
			});
		suite.addTest( new UITest(new String[]{
			"-op", "trainTest", "-seq", "-data", "sample:toySeq.train", "-splitter", "k5"}) {
				public void checkResult(Object result) {
					assertTrue(result instanceof Evaluation);
					Evaluation e = (Evaluation)result;
					assertEquals( 0.2, e.errorRate(),  0.2 );
				}
			});
		suite.addTest( new UITest(new String[]{
			"-op", "train", "-seq", "-data", "sample:toySeq.train", "-saveAs", "tmp.cls"}) {
				public void checkResult(Object result) {
					assertTrue(result instanceof SequenceClassifier);
				}
			});
		suite.addTest( new UITest(new String[]{
			"-op", "test", "-seq", "-data", "sample:toySeq.test", "-classifierFile", "tmp.cls"}) {
				public void checkResult(Object result) {
					assertTrue(result instanceof Evaluation);
					Evaluation e = (Evaluation)result;
					assertEquals( 0.25, e.errorRate(),  0.2 );
				}
			});
		
    return suite;
  }


	// test the TrainTestClassifier package
  abstract public static class UITest extends TestCase
  {
		private String[] args;
    public UITest(String[] args) 
		{ 
			super("doTest"); 
			this.args = args;
		}
    public void doTest()
    {
			UI.DataClassificationTask m = new UI.DataClassificationTask();
			m.callMain(args);
			checkResult(m.getMainResult());
    }
		abstract public void checkResult(Object result);
  }
  static public void main(String[] argv) {
    junit.textui.TestRunner.run(suite());
  }
}
