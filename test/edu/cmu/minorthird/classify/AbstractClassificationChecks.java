package edu.cmu.minorthird.classify;

import junit.framework.TestCase;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * This class...
 * @author ksteppe
 */
abstract public class AbstractClassificationChecks extends TestCase
{
  protected Logger log = Logger.getLogger(this.getClass());

  protected final static ClassifierLearner DEFAULT_LEARNER = new NaiveBayes();

  private boolean checkStandardStatsOnly = false;
  private double delta = 0.001;

  public AbstractClassificationChecks(String name)
  {
    super(name);
    log.setLevel(Level.DEBUG);
  }

  /**
   *
   * @param learner
   * @param trainData
   * @param testData
   * @param referenceStats should be error, precision, recall, ??
   */
  public void checkClassify(ClassifierLearner learner, Dataset trainData, Dataset testData, double[] referenceStats)
  {
    Evaluation v = Tester.evaluate(learner, trainData, testData);


    double[] stats;
    //System.out.println("Average Log Loss: " + );
    if (checkStandardStatsOnly)
    {
      stats = new double[4];
      stats[0] = v.errorRate();
      stats[1] = v.averagePrecision();
      stats[2] = v.maxF1();
      stats[3] = v.averageLogLoss();
      log.info("using Standard statos only (4 of them)");
    }
    else
      stats = v.summaryStatistics();

    if (referenceStats != null && stats.length != referenceStats.length)
      throw new IllegalStateException("number of statistics to check is different from the number of reference stats given!");

    checkStats(stats, referenceStats);
  }

  protected void checkStats(double[] stats, double[] referenceStats)
  {
    for (int i = 0; i < stats.length; i++)
    {
      double stat = stats[i];
      log.info("stat("+i+") = " + stat);
      System.out.println("Predictedstat("+i+") = " + stat);
      System.out.println("Referencestat("+i+") = " + referenceStats[i]);
      if (referenceStats != null)
        assertEquals(referenceStats[i], stat, delta);
    }
  }

  public double getDelta()
  { return delta; }

  public void setDelta(double delta)
  { this.delta = delta; }

  protected void setCheckStandards(boolean b)
  { checkStandardStatsOnly = b; }
}
