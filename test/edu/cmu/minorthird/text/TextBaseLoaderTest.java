package edu.cmu.minorthird.text;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.io.File;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class TextBaseLoaderTest extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());

  /**
   * Standard test class constructior for TextBaseLoaderTest
   * @param name Name of the test
   */
  public TextBaseLoaderTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for TextBaseLoaderTest
   */
  public TextBaseLoaderTest()
  {
    super("TextBaseLoaderTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    Logger.getRootLogger().removeAllAppenders();
    org.apache.log4j.BasicConfigurator.configure();
    //TODO add initializations if needed
  }

  /**
   * clean up to run after each test
   */
  protected void tearDown()
  {
    //TODO clean up resources if needed
  }

  /**
   * Base test for TextBaseLoaderTest
   */
  public void testTextBaseLoaderTest()
  {
//        try
//        {
    TextBase base = new BasicTextBase();
    TextBaseLoader loader = new TextBaseLoader();

    File dir = new File("examples/20newgroups/20news-bydate-train");
    loader.loadLabeledDir(base, dir);

    log.debug("loaded training set");

    // set up an environment that contains the labels
    MutableTextEnv env = loader.getEnvironment();


    log.debug("passed first assertion");
    log.debug("base size = " + base.size());
    // for verification/correction of the labels, if we care...
    // TextBaseLabeler.label( env, new File("my-document-labels.env"));

    // set up a simple bag-of-words feature extractor
    edu.cmu.minorthird.text.learn.SpanFeatureExtractor fe = edu.cmu.minorthird.text.learn.SampleFE.BAG_OF_LC_WORDS; //SimpleFeatureExtractor();

    // create a binary dataset for the class 'delete'
    Dataset data = new BasicDataset();
    int numSpans = 0;
    for (Span.Looper i = base.documentSpanIterator(); i.hasNext();)
    {
      log.debug("span: " + numSpans++);
      Span s = i.nextSpan();
      double label = env.hasType(s, "delete") ? +1 : -1;
      data.add(new BinaryExample(fe.extractInstance(s), label));
    }

    log.debug("extracted dataset");
    log.debug("data size = " + data.size());

    Example.Looper it = data.iterator();
    log.debug("got looper: " + it);

    TextBase testBase = new BasicTextBase();
    loader.loadLabeledDir(testBase, new File("examples/20newgroups/20news-bydate-test"));

    log.debug("loaded test data");

    Dataset testData = new BasicDataset();
    for (Span.Looper i = testBase.documentSpanIterator(); i.hasNext();)
    {
      Span s = i.nextSpan();
      double label = env.hasType(s, "delete") ? +1 : -1;
      testData.add(new BinaryExample(fe.extractInstance(s), label));
    }

    // pick a learning algorithm
    //ClassifierLearner learner = new AdaBoost(new DecisionTreeLearner(), 10);
    //ClassifierLearner learner = new DecisionTreeLearner();
    ClassifierLearner learner = new VotedPerceptron();
    //ClassifierLearner learner = new NaiveBayes();
    //ClassifierLearner learner = new AdaBoost(new BinaryBatchVersion(new NaiveBayes()), 10);

    // do a 10-fold cross-validation experiment
    Evaluation v = Tester.evaluate(learner, data, testData);
//    Evaluation v = Tester.evaluate(learner, data, new CrossValSplitter(10));

    //need to determine an appropriate result

    // display the results
    ViewerFrame f = new ViewerFrame("Results of 10-fold CV on 'delete'", v.toGUI());

//        }
//        catch (IOException e)
//        {
//            log.error(e, e);  //To change body of catch statement use Options | File Templates.
//        }

  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(TextBaseLoaderTest.class);
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