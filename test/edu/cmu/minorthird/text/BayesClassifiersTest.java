package edu.cmu.minorthird.text;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.PoissonLearner;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class BayesClassifiersTest extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());

  /**
   * Standard test class constructior for BayesClassifiersTest
   * @param name Name of the test
   */
  public BayesClassifiersTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for BayesClassifiersTest
   */
  public BayesClassifiersTest()
  {
    super("BayesClassifiersTest");
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
   * Base test for BayesClassifiersTest
   */
  public void testBayesClassifiersTest()
  {

    try
    { // load the documents into a textbase
      TextBase base = new BasicTextBase();
      TextBaseLoader loader = new TextBaseLoader();
      File dir = new File("examples/testData.dir");
      loader.loadTaggedFiles(base, dir);

// set up an environment that contains the labels
      MutableTextEnv env = new BasicTextEnv(base);
      new TextEnvLoader().importOps(env, base, new File("examples/testData.env"));

// for verification/correction of the labels, if we care...
//TextBaseLabeler.label( env, new File("my-document-labels.env"));

// set up a simple bag-of-words feature extractor
      edu.cmu.minorthird.text.learn.SpanFeatureExtractor fe = new edu.cmu.minorthird.text.learn.SpanFeatureExtractor()
      {
        public Instance extractInstance(TextEnv env, Span s)
        {
          edu.cmu.minorthird.text.learn.FeatureBuffer buf = new edu.cmu.minorthird.text.learn.FeatureBuffer(env, s);
          try
          { edu.cmu.minorthird.text.learn.SpanFE.from(s, buf).tokens().eq().lc().punk().usewords("examples/t1.words.text").emit(); }
          catch (IOException e)
          { log.error(e, e); }
          //SpanFE.from(s,buf).tokens().eq().lc().punk().stopwords("remove").emit();
          return buf.getInstance();
        }
        public Instance extractInstance(Span s) {
          return extractInstance(null,s);
        }
      };

// check
      log.debug(env.getTypes().toString());

// create a binary dataset for the class 'rr'
      Dataset data = new BasicDataset();
      for (Span.Looper i = base.documentSpanIterator(); i.hasNext();)
      {
        Span s = i.nextSpan();
        //System.out.println( env );
        double label = env.hasType(s, "rr") ? +1 : -1;
        data.add(new BinaryExample(fe.extractInstance(s), label));
        //BinaryExample example = new BinaryExample( fe.extractInstance(s), label );
        //data.add( example );
      }

      ViewerFrame f = new ViewerFrame("rr data", data.toGUI());
//      System.exit(0);

// pick a learning algorithm
      ClassifierLearner learner = new PoissonLearner();


// do a 10-fold cross-validation experiment
      Evaluation v = Tester.evaluate(learner, data, new CrossValSplitter(10));

// display the results
      f = new ViewerFrame("Results of 10-fold CV on 'rr'", v.toGUI());
    }
    catch (Exception e)
    {
      log.error(e, e);
      fail();
    }
  }

  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(BayesClassifiersTest.class);
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