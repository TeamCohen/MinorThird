package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.BatchVersion;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.StackedLearner;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.algorithms.svm.*;
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
		SpanFeatureExtractor fe = SampleFE.makeExtractionFE(2);
		doExtractionTest( new CMMAnnotatorLearner( fe, new VotedPerceptron(), 2),
										 new double[]{0.93, 0.8, 0.1, 0.75, 0.6, 0.1});
		doExtractionTest( new CMMAnnotatorLearner(fe, new SVMLearner(), 3), 
											new double[]{1.0,1.0,0.05,1.0,1.0,0.05} );
  }

	// double array is <precision,recall,tolerance> for train & test
	private void doExtractionTest(AnnotatorLearner learner, double[]expected)
	{
		AnnotatorTeacher annotatorTeacher = new TextEnvAnnotatorTeacher( env, labelString );
		learner.setAnnotationType( "prediction" );
		Annotator learnedAnnotator = annotatorTeacher.train( learner );
		TextEnv trainEnv1 = learnedAnnotator.annotatedCopy( env );
		TextEnv testEnv1 = learnedAnnotator.annotatedCopy( testEnv );
		checkSpans( "prediction", labelString, trainEnv1, expected[0],expected[1],expected[2]);
		checkSpans( "prediction", labelString, testEnv1, expected[3],expected[4],expected[5]);
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
