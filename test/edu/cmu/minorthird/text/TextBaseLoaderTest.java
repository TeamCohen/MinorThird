package edu.cmu.minorthird.text;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.Globals;
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

  public void testSeminarSet()
  {
    try
    {
      TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE);
      loader.setLabelsInFile(true);
      File dataLocation = new File(Globals.DATA_DIR + "seminar-subset");
      TextBase textBase = loader.load(dataLocation);

      TextLabels labels = TextBaseLoader.loadDirOfTaggedFiles(dataLocation);
    }
    catch (Exception e)
    {
      log.fatal(e, e);
      fail();
    }
  }

  public void testLoadLabeledFile()
  {
    try
    {
//      new TextBaseLoader().loadDir(null, null);
//      fail("need data");
//      TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE);
//      loader.setLabelsInFile(true);
//      TextBase textBase = loader.load(new File("demos/sampleData/webmasterCommands.txt"));
    }
    catch (Exception e)
    {
      log.fatal(e, e);
      fail();
    }

  }

  public void testLines()
  {
    try
    {
      TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_LINE, TextBaseLoader.IN_FILE);
      File dataLocation = new File(Globals.DATA_DIR + "webmasterCommands.base");
      TextBase base = loader.load(dataLocation);

      base = TextBaseLoader.loadDocPerLine(dataLocation, true);
    }
    catch (Exception e)
    {
      log.fatal(e, e);
      fail();
    }
  }

  /**
   * Base test for TextBaseLoaderTest
   */
  public void testDirectories()
  {
    try
    {
      TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE, TextBaseLoader.FILE_NAME,
                                                  TextBaseLoader.NONE, TextBaseLoader.DIRECTORY_NAME, false, true);

      log.info("loader labels: " + loader.isLabelsInFile());
      File dir = new File(Globals.DATA_DIR + "20newgroups/20news-bydate-train");
      TextBase base = loader.load(dir);
  //    loader.loadLabeledDir(base, dir);

      log.debug("loaded training set");

      // set up the labels
      MutableTextLabels labels = loader.getLabels();


      log.debug("passed first assertion");
      log.debug("base size = " + base.size());
      // for verification/correction of the labels, if we care...
      // TextBaseLabeler.label( labels, new File("my-document-labels.env"));

      // set up a simple bag-of-words feature extractor
      edu.cmu.minorthird.text.learn.SpanFeatureExtractor fe = edu.cmu.minorthird.text.learn.SampleFE.BAG_OF_LC_WORDS; //SimpleFeatureExtractor();

      // create a binary dataset for the class 'delete'
      Dataset data = extractDataset(base, labels, fe);

      log.debug("extracted dataset");
      log.debug("data size = " + data.size());

      Example.Looper it = data.iterator();
      log.debug("got looper: " + it);

//      TextBase testBase = loader.load(new File("c:\\cmu/radar/extractionGroup/extract/examples/20newgroups/20news-bydate-test"));
//      labels = loader.getLabels();
//      log.debug("loaded test data");
//
//      Dataset testData = extractDataset(testBase, labels, fe);

      // pick a learning algorithm
      //ClassifierLearner learner = new AdaBoost(new DecisionTreeLearner(), 10);
      //ClassifierLearner learner = new DecisionTreeLearner();
      ClassifierLearner learner = new VotedPerceptron();
      //ClassifierLearner learner = new NaiveBayes();
      //ClassifierLearner learner = new AdaBoost(new BinaryBatchVersion(new NaiveBayes()), 10);

      // do a 10-fold cross-validation experiment
//      Evaluation v = Tester.evaluate(learner, data, testData);
  //    Evaluation v = Tester.evaluate(learner, data, new CrossValSplitter(10));

      //need to determine an appropriate result

      // display the results
//      ViewerFrame f = new ViewerFrame("Results of 10-fold CV on 'delete'", v.toGUI());

      }
    catch (Exception e)
    {
      log.error(e, e);  //To change body of catch statement use Options | File Templates.
      fail();
    }

  }

  private Dataset extractDataset(TextBase base, MutableTextLabels labels, edu.cmu.minorthird.text.learn.SpanFeatureExtractor fe)
  {
    Dataset data = new BasicDataset();
    int numSpans = 0;
    for (Span.Looper i = base.documentSpanIterator(); i.hasNext();)
    {
      log.debug("span: " + numSpans++);
      Span s = i.nextSpan();
      double label = labels.hasType(s, "delete") ? +1 : -1;
      data.add(new BinaryExample(fe.extractInstance(s), label));
    }
    return data;
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