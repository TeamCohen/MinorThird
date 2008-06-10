package edu.cmu.minorthird.text.learn;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;
import edu.cmu.minorthird.classify.sequential.CMMLearner;
import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.SpanDifference;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.SpanDifference.Looper;
import edu.cmu.minorthird.ui.Recommended;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class SampleExtractionTest extends TestCase
{
	static Logger log = Logger.getLogger(SampleExtractionTest.class);

	// text base of training data
	protected TextBase base;
	protected TextLabels labels;
	// text base of testing data
	protected TextBase testBase;
	protected TextLabels testLabels;
	// labelString
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
	protected void setUp(){
		Logger.getRootLogger().removeAllAppenders();
		org.apache.log4j.BasicConfigurator.configure();
		//TODO add initializations if needed
		base = SampleExtractionProblem.trainBase();
		labels = SampleExtractionProblem.trainLabels();

		//create test date
		testBase = SampleExtractionProblem.testBase();
		testLabels = SampleExtractionProblem.testLabels();
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
		SpanFeatureExtractor fe = new Recommended.TokenFE();
		doExtractionTest( new SequenceAnnotatorLearner( new CMMLearner(new VotedPerceptron(), 3), fe),
				new double[]{0.93,0.75,0.25,1.0,0.6,0.25});
		doExtractionTest( new SequenceAnnotatorLearner( new CMMLearner(new SVMLearner(), 3),  fe),
				new double[]{0.93,1.0,0.25,1.0,1.0,0.25} );
	}

	// double array is <precision,recall,tolerance> for train & test
	private void doExtractionTest(AnnotatorLearner learner, double[]expected)
	{
		AnnotatorTeacher annotatorTeacher = new TextLabelsAnnotatorTeacher( labels, labelString );
		learner.setAnnotationType( "prediction" );
		Annotator learnedAnnotator = annotatorTeacher.train( learner );
		TextLabels trainLabels1 = learnedAnnotator.annotatedCopy( labels );
		TextLabels testLabels1 = learnedAnnotator.annotatedCopy( testLabels );
		//TextBaseViewer.view( testLabels1 );
		//TextBaseViewer.view( trainLabels1 );
		checkSpans( "prediction", labelString, trainLabels1, expected[0],expected[1],expected[2]);
		checkSpans( "prediction", labelString, testLabels1, expected[3],expected[4],expected[5]);
	}

	private void 
	checkSpans(String guessType,String truthType,TextLabels labels,double tokRec,double tokPrec,double epsilon)
	{
		SpanDifference sd = new SpanDifference(labels.instanceIterator(guessType),labels.instanceIterator(truthType));
		System.out.println();
		System.out.println(sd.toSummary());
		System.out.println(sd);
		Looper l=sd.differenceIterator();
		while(l.hasNext()){
			System.out.println(">>"+l.next());
			//System.out.println(">>>"+l.next());
		}
		System.out.println(tokPrec+" "+sd.tokenPrecision()+" "+epsilon);
		assertEquals( tokPrec, sd.tokenPrecision(), epsilon );
		System.out.println(tokRec+" "+sd.tokenRecall()+" "+epsilon);
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
