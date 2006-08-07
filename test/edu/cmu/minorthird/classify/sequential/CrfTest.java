package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.experiments.Evaluation;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import edu.cmu.minorthird.classify.sequential.CRFLearner;
import edu.cmu.minorthird.classify.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.io.*;
import java.util.StringTokenizer;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class CrfTest extends AbstractClassificationChecks
{
    Logger log = Logger.getLogger(this.getClass());

    /**
     * Standard test class constructior for CrfTest
     * @param name Name of the test
     */
    public CrfTest(String name)
    {
        super(name);
    }

    /**
     * Convinence constructior for CrfTest
     */
    public CrfTest()
    {
        super("CrfTest");
    }

    /**
     * setUp to run before each test
     */
    protected void setUp()
    {
        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.BasicConfigurator.configure();
        log.setLevel(Level.DEBUG);
        super.setCheckStandards(false);
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
     * Creates a TestSuite from all testXXX methods
     * @return TestSuite
     */
    public static Test suite()
    {
        return new TestSuite(CrfTest.class);
    }

    /**
     * Run the full suite of tests with text output
     * @param args - unused
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }


    /**
     *  Test a full cycle of training, testing, saving (serializing), loading, and testing again.<br>
     *  <br>
     *  This test was added when feature names were changed over from using the old Feature.Factory.getId()
     *  method (or Feature.getNumericName(), which calls getId()) to the newer FeatureIdFactory methods.
     **/
    public void testSerialization() {
        try {
            // Create a classifier using the CRFLearner and the toyTrain dataset
            CRFLearner l = new CRFLearner();
            SequenceClassifier c1 = new DatasetSequenceClassifierTeacher(SampleDatasets.makeToySequenceData()).train(l);

            // Evaluate it immediately saving the stats
            Evaluation e1 = new Evaluation(SampleDatasets.makeToySequenceData().getSchema());
            e1.extend(c1, SampleDatasets.makeToySequenceTestData());
            double[] stats1 = new double[4];
            stats1[0] = e1.errorRate();
            stats1[1] = e1.averagePrecision();
            stats1[2] = e1.maxF1();
            stats1[3] = e1.averageLogLoss();

            // Serialize the classifier to disk
            ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("CRFTest.classifier")));
            out.writeObject(c1);
            out.flush();
            out.close();

            // Load it back in.
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("CRFTest.classifier")));
            SequenceClassifier c2 = (SequenceClassifier)in.readObject();
            in.close();

            // Evaluate again saving the stats
            Evaluation e2 = new Evaluation(SampleDatasets.makeToySequenceData().getSchema());
            e2.extend(c2, SampleDatasets.makeToySequenceTestData());
            //double[] stats2 = e2.summaryStatistics();
            double[] stats2 = new double[4];
            stats2[0] = e2.errorRate();
            stats2[1] = e2.averagePrecision();
            stats2[2] = e2.maxF1();
            stats2[3] = e2.averageLogLoss();

            // Only use the basic stats for now because some of the advanced stats
            //  come back as NaN for both datasets and the check stats method can't
            //  handle NaN's
            log.info("using Standard stats only (4 of them)");

            // Compare the stats produced from each run to make sure they are identical
            checkStats(stats1, stats2);

            // Remove the temporary classifier file
            File theClassifier = new File("CRFTest.classifier");
            theClassifier.delete();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
