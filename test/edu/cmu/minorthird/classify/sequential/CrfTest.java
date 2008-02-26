package edu.cmu.minorthird.classify.sequential;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.AbstractClassificationChecks;
import edu.cmu.minorthird.classify.SampleDatasets;
import edu.cmu.minorthird.classify.experiments.Evaluation;

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

    //  Test the basic functions of CRFLearner to make sure they are working properly
    public void testBasicCRF() {
        double[] refs = new double[]{0.0, // Error Rate
                                     0.0, // std. deviation of Error Rate
                                     0.0, // Balanced Error Rate
                                     0.0, // Error Rate on POS
                                     0.0, // std. deviation of Error Ratr on POS
                                     0.0, // Error Rate on NEG
                                     0.0, // std. deviation of Error Ratr on NEG
                                     1.0, // Average Precision
                                     1.0, // Maximum F1
                                     3.277534399186934, // Average Log Loss
                                     1.0, // Recall
                                     1.0, // Precision
                                     1.0, // F1
                                     1.0}; // Kappa        
        CRFLearner l = new CRFLearner();
        SequenceClassifier c = new DatasetSequenceClassifierTeacher(SampleDatasets.makeToySequenceData()).train(l);

        // Evaluate it immediately saving the stats
        Evaluation e = new Evaluation(SampleDatasets.makeToySequenceData().getSchema());
        e.extend(c, SampleDatasets.makeToySequenceTestData());
        checkStats(e.summaryStatistics(), refs);
    }

    // Test the SegmentCRFLearner subclass of CRFLearner to make sure that its basic 
    //   functions are working properly
    public void testSegmentCRF() {
        double[] refs = new double[]{0.0, // Error Rate
                                     0.0, // std. deviation of Error Rate
                                     0.0, // Balanced Error Rate
                                     0.0, // Error Rate on POS
                                     0.0, // std. deviation of Error Ratr on POS
                                     0.0, // Error Rate on NEG
                                     0.0, // std. deviation of Error Ratr on NEG
                                     1.0, // Average Precision
                                     1.0, // Maximum F1
                                     3.277534399186934, // Average Log Loss
                                     1.0, // Recall
                                     1.0, // Precision
                                     1.0, // F1
                                     1.0}; // Kappa
        SegmentCRFLearner l = new SegmentCRFLearner();
        SequenceClassifier c = new DatasetSequenceClassifierTeacher(SampleDatasets.makeToySequenceData()).train(l);

        // Evaluate it immediately saving the stats
        Evaluation e = new Evaluation(SampleDatasets.makeToySequenceData().getSchema());
        e.extend(c, SampleDatasets.makeToySequenceTestData());
        checkStats(e.summaryStatistics(), refs);
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
