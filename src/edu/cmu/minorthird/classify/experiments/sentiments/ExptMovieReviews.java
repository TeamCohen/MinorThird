package edu.cmu.minorthird.classify.experiments.sentiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.transform.T1InstanceTransformLearner;
import edu.cmu.minorthird.classify.transform.InstanceTransform;
import edu.cmu.minorthird.classify.transform.T1InstanceTransform;
import edu.cmu.minorthird.classify.algorithms.linear.PoissonLearner;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.text.gui.TextBaseLabeler;
import edu.cmu.minorthird.text.gui.MixupDebugger;
import edu.cmu.minorthird.text.learn.SpanFE;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.FeatureBuffer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This class is responsible for running experiments on the datasets of Movie Reviews
 * publicly available for download from Cornell Univ.
 *
 * @author Edoardo M. Airoldi
 * Date: Jan 21, 2004
 */

public class ExptMovieReviews extends TestCase
{
  Logger log = Logger.getLogger(this.getClass());

  /** Standard test class constructior for ExptMovieReviews
   * @param name Name of the test
   */
  public ExptMovieReviews(String name) {
    super(name);
  }

  /** Convinence constructior for BayesClassifiersTest
   */
  public ExptMovieReviews() {
    super("ExptMovieReviews");
  }

  /** setUp to run before each test
   */
  protected void setUp() {
    Logger.getRootLogger().removeAllAppenders();
    org.apache.log4j.BasicConfigurator.configure();
  }


  /**
   * Experiment on Movie Reviews
   */
  public void testExptMovieReviews() {
    try {

      // load the documents into a textbase
      TextBase base = new BasicTextBase();
      TextBaseLoader loader = new TextBaseLoader();
      File dir = new File("/Users/eairoldi/cmu.research/Text.Learning.Group/UAI.2004/Min3rd-Datasets/movie-reviews-100"); // /Users/eairoldi/cmu.research/Text.Learning.Group/UAI.2004
      loader.loadTaggedFiles(base, dir);

      // load labels
      MutableTextLabels labels = new BasicTextLabels(base);
      new TextLabelsLoader().importOps(labels, base, new File("/Users/eairoldi/cmu.research/Text.Learning.Group/UAI.2004/Min3rd-Datasets/movie-labels-100.env"));

      // for verification/correction of the labels, if we care...
      //TextBaseLabeler.label( labels, new File("/Users/eairoldi/cmu.research/Text.Learning.Group/UAI.2004/Min3rd-Datasets/movie-labels-100.env"));


      // apply mixup file to get candidates, if there is one
      File mixupFile = new File("/Users/eairoldi/cmu.research/Text.Learning.Group/UAI.2004/Min3rd-Datasets/sentiments.mixup");
      MixupProgram p = new MixupProgram(mixupFile);
      p.eval(labels,base);

      // set up a simple bag-of-words feature extractor
      System.out.println("extract features");
      SpanFeatureExtractor fe = new SpanFeatureExtractor() {
        public Instance extractInstance(TextLabels labels, Span s) {
          FeatureBuffer buf = new FeatureBuffer(labels, s);
          //SpanFE.from(s,buf).tokens().eq().lc().punk().stopwords("use").emit();
          /*try {
          SpanFE.from(s, buf).tokens().eq().lc().punk().usewords("examples/t1.words.text").emit();
          } catch (IOException e) {
          log.error(e, e);
          } */
          SpanFE.from(s,buf).contains("bigram").eq().emit();
          SpanFE.from(s, buf).tokens().eq().lc().punk().emit();
          return buf.getInstance();
        }
        public Instance extractInstance(Span s) {
          return extractInstance(null,s);
        }
      };

      // check
      //log.debug(labels.getTypes().toString());


      //
      // debug mixup
      //
      try {

        JFrame frame = new JFrame("TextBaseEditor");

        MixupDebugger debugger =
          new MixupDebugger(base,null,mixupFile,false,false);
        frame.getContentPane().add( debugger, BorderLayout.CENTER );
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
          });
        frame.pack();
        frame.setVisible(true);
      } catch (Exception e) {
        e.printStackTrace();
      }
      //
      //
      //




      // create a binary dataset for the class 'Pos'
      System.out.println("create a dataset");
      Dataset data = new BasicDataset();
      for (Span.Looper i = base.documentSpanIterator(); i.hasNext();)
      {
        Span s = i.nextSpan();
        //System.out.println( labels );
        double label = labels.hasType(s, "Pos") ? +1 : -1;
        data.add(new BinaryExample(fe.extractInstance(s), label));
        //BinaryExample example = new BinaryExample( fe.extractInstance(s), label );
        //data.add( example );
      }

      //Dataset data = MovieDataset.MovieReviewsData();


      // Filter
      System.out.println("Filter Features");
      T1InstanceTransformLearner filter = new T1InstanceTransformLearner();
      //filter.setREF_LENGTH(100.0);
      //filter.setPDF("Poisson");
      //filter.setPDF("Negative-Binomial");

      InstanceTransform t1stat = filter.batchTrain( data );
      //((T1InstanceTransform)t1stat).setALPHA(0.05);
      //((T1InstanceTransform)t1stat).setMIN_WORDS(50);
      //((T1InstanceTransform)t1stat).setSAMPLE(10000);

      data = t1stat.transform( data );
      //System.out.println( "Filtered Dataset:\n" + data );

      ViewerFrame f = new ViewerFrame("Pos data", data.toGUI());
      //System.exit(0);


      // pick a learning algorithm
      System.out.println("Classify Examples");
      ClassifierLearner learner = new NaiveBayes();


      // do a 10-fold cross-validation experiment
      System.out.println("Evaluate");
      Evaluation v = Tester.evaluate(learner, data, new StratifiedCrossValSplitter(10));


      // display the results
      f = new ViewerFrame("Results of 10-fold CV on 'Pos'", v.toGUI());


    } catch (Exception e) {
      log.error(e, e);
      fail();
    }
  }



  /** Creates a TestSuite from the experiment
   * @return TestSuite
   */
  public static Test suite() {
    return new TestSuite(ExptMovieReviews.class);
  }

  /** Run the suite with text output
   * @param args - unused
   */
  public static void main(String args[]) {
    junit.textui.TestRunner.run(suite());

  }
}