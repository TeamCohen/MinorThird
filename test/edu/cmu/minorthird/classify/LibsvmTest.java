package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;
import edu.cmu.minorthird.text.learn.ClassifyTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import libsvm.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.StringTokenizer;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class LibsvmTest extends ClassifyTest
{
  Logger log = Logger.getLogger(this.getClass());
  private static final String trainFile = "../../example2/a1a.dat";
  private static final String model = "modelFile.dat";
  private static final String testFile = "../../example2/a1a.t.dat";

  /**
   * Standard test class constructior for LibsvmTest
   * @param name Name of the test
   */
  public LibsvmTest(String name)
  {
    super(name);
  }

  /**
   * Convinence constructior for LibsvmTest
   */
  public LibsvmTest()
  {
    super("LibsvmTest");
  }

  /**
   * setUp to run before each test
   */
  protected void setUp()
  {
    org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
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
   * tests using svm_train.main, etc.
   */
  public void testDirectCode()
  {
    log.debug("start");

    try
    {
      svm_train.main(new String[]{"-t", "0", trainFile, model});

      log.debug("trained, sent model to: " + model);
      double[] results = prediction(new String[]{testFile, model, "results.dat"});
      log.debug("ran predict on testfile");

      double[] expect = new double[]{0.8352766230693838, 0.6588935077224645, 0.28752077092970524};
      checkStats(results, expect);
    }
    catch (Exception e)
    {
      log.error(e, e);
      fail("exception");
    }

  }

  /**
   * use wrapper on the provided data, should get same results
   * as the direct
   */
  public void testWrapper()
  {
    try
    { //get datasets
      Dataset trainData = DatasetLoader.loadSVMStyle(new File(trainFile));
      Dataset testData = DatasetLoader.loadSVMStyle(new File(testFile));
//      log.debug("loaded: " + dataset);

      //create the SVMLearner

      //send expectations to checkClassify()
      double[] expect = new double[]{0.16472337693061612, 0.5532531341004251, 0.6413123436810357, 1.3132616875183545};
      super.checkClassify(new SVMLearner(), trainData, testData, expect);
    }
    catch (Exception e)
    {
      log.error(e, e);
    }
  }

  /**
   * run the svm wrapper on the sample data
   */
  public void testSampleData()
  {
    double[] refs = new double[]{0.0, 1.0, 1.0, 1.3132616875182228};
    super.checkClassify(new SVMLearner(), SampleDatasets.toyTrain(), SampleDatasets.toyTest(), refs);
  }


  /**
   * Creates a TestSuite from all testXXX methods
   * @return TestSuite
   */
  public static Test suite()
  {
    return new TestSuite(LibsvmTest.class);
  }

  /**
   * Run the full suite of tests with text output
   * @param args - unused
   */
  public static void main(String args[])
  {
    junit.textui.TestRunner.run(suite());
  }


  // Crap from svm_predict.java
  private double[] predict(BufferedReader input, DataOutputStream output, svm_model model) throws IOException
  {
    int correct = 0;
    int total = 0;
    double error = 0;
    double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;

    while (true)
    {
      String line = input.readLine();
      if (line == null) break;

      StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

      double target = atof(st.nextToken());
      int m = st.countTokens() / 2;
      svm_node[] x = new svm_node[m];
      for (int j = 0; j < m; j++)
      {
        x[j] = new svm_node();
        x[j].index = atoi(st.nextToken());
        x[j].value = atof(st.nextToken());
      }
      double v = svm.svm_predict(model, x);
      if (v == target)
        ++correct;
      error += (v - target) * (v - target);
      sumv += v;
      sumy += target;
      sumvv += v * v;
      sumyy += target * target;
      sumvy += v * target;
      ++total;

//      output.writeBytes(v+"\n");
    }
    log.debug("Accuracy = " + (double)correct / total * 100 +
        "% (" + correct + "/" + total + ") (classification)\n");
    log.debug("Mean squared error = " + error / total + " (regression)\n");
    log.debug("Squared correlation coefficient = " +
        ((total * sumvy - sumv * sumy) * (total * sumvy - sumv * sumy)) /
        ((total * sumvv - sumv * sumv) * (total * sumyy - sumy * sumy)) + " (regression)\n"
    );

    double[] rvalues = new double[3];
    rvalues[0] = (double)correct / (double)total;
    rvalues[1] = error / (double)total;
    rvalues[2] = ((total * sumvy - sumv * sumy) * (total * sumvy - sumv * sumy)) /
        ((total * sumvv - sumv * sumv) * (total * sumyy - sumy * sumy));

    return rvalues;

  }

  private double[] prediction(String argv[]) throws IOException
  {
    if (argv.length != 3)
    {
      System.err.print("usage: svm-predict test_file model_file output_file\n");
      System.exit(1);
    }

    BufferedReader input = new BufferedReader(new FileReader(argv[0]));
    DataOutputStream output = new DataOutputStream(new FileOutputStream(argv[2]));
    svm_model model = svm.svm_load_model(argv[1]);
    return predict(input, output, model);
  }

  private static double atof(String s)
  {
    return Double.valueOf(s).doubleValue();
  }

  private static int atoi(String s)
  {
    return Integer.parseInt(s);
  }

}