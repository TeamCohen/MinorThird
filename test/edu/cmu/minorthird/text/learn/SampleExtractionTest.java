package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.BatchVersion;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.StackedLearner;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class SampleExtractionTest extends TestCase
{
  static Logger log = Logger.getLogger(SampleExtractionTest.class);

  /** text base of training data */
  protected TextBase base;
  protected TextEnv env;
  /** testing data */
  protected TextBase testBase;
  protected TextEnv testEnv;
  /** span checking */
  private String documentId;
  private String labelString;


  /**
   * Standard test class constructior for SampleExtractionTest
   * @param name Name of the test
   */
  public SampleExtractionTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for SampleExtractionTest
   */
  public SampleExtractionTest()
  {
    super("SampleExtractionTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    Logger.getRootLogger().removeAllAppenders();
    org.apache.log4j.BasicConfigurator.configure();
    //TODO add initializations if needed
    base = SampleExtractionProblem.trainBase();
    env = SampleExtractionProblem.trainEnv();

    //create test date
    testBase = SampleExtractionProblem.testBase();
    testEnv = SampleExtractionProblem.testEnv();
    //convert to Dataset

    this.labelString = SampleExtractionProblem.LABEL;

  }

  /**
   * clean up to run after each test
   */
  protected void tearDown()
  {
    //TODO clean up resources if needed
  }

  /**
   * Base test for SampleExtractionTest
   */
  public void testSampleExtractionTest()
  {
		SpanFeatureExtractor fe = SampleFE.makeExtractionFE(3);
		ClassifierLearner classifierLearner =
			new StackedLearner(	new BatchVersion(new NaiveBayes()), new RandomSplitter(0.8) );
		AnnotatorLearner cmmLearner = new CMMAnnotatorLearner( fe, classifierLearner, 3 );
		doExtractionTest(cmmLearner);
  }

	private void doExtractionTest(AnnotatorLearner learner)
	{
		AnnotatorTeacher annotatorTeacher = new TextEnvAnnotatorTeacher( env, labelString );
		learner.setAnnotationType( "prediction" );
		Annotator learnedAnnotator = annotatorTeacher.train( learner );
		TextEnv trainEnv1 = learnedAnnotator.annotatedCopy( env );
		TextEnv testEnv1 = learnedAnnotator.annotatedCopy( testEnv );
		checkSpans( "prediction", labelString, trainEnv1, 1.0, 1.0, 0.05);
		checkSpans( "prediction", labelString, testEnv1, 1.0, 0.888, 0.05);
		//TextBaseViewer.view( testEnv1 );
		//TextBaseViewer.view( trainEnv1 );
	}
	
	private void 
	checkSpans(String guessType,String truthType,TextEnv env,double tokRec,double tokPrec,double epsilon)
	{
		SpanDifference sd = new SpanDifference(env.instanceIterator(guessType),env.instanceIterator(truthType));
		assertEquals( tokPrec, sd.tokenPrecision(), epsilon );
		assertEquals( tokRec, sd.tokenRecall(), epsilon );
	}

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(SampleExtractionTest.class);
  }

  /**
   * Run the full suite of tests with text output
   * @param args - unused
   */
  public static void main(String args[])
  {
    junit.textui.TestRunner.run(suite());
  }
}
