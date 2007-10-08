package edu.cmu.minorthird.classify.algorithms.svm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.util.Random;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.FeatureIdFactory;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.MutableInstance;
import libsvm.svm;
import libsvm.svm_node;
import libsvm.svm_model;
import libsvm.svm_problem;


/**
 *
 * This class is responsible for testing nodeToFeature and nodeArrayToInstance
 * functions from SVMUtils class.
 *
 * @author chiachi
 */
public class SVMUtilsTest extends TestCase
{

    Logger log = Logger.getLogger(this.getClass());
  
    FeatureIdFactory m_testDataIdFactory;

    /**
     * Standard test class constructior for SVMUtilsTest
     * @param name Name of the test
     */
    public SVMUtilsTest(String name)
    {
	super(name);

	Dataset testDataSet = setTestDatasets();

	m_testDataIdFactory = new FeatureIdFactory(testDataSet);
    }

    /**
     * Convinence constructior for SVMUtilsTest
     */
    public SVMUtilsTest()
    {

	super("SVMUtilsTest");

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

    public void testNodeToFeature()
    {
	System.out.println("    -- Testing NodeToFeature(...) --");
	  
	//Dataset testdata = setTestDatasets();
	//FeatureIdFactory idFactoryTest = new FeatureIdFactory(m_testDataSet);
	int featureId = 7;

	Feature testFeature = m_testDataIdFactory.getFeature(featureId);

	String featureStrName = testFeature.toString();
	  
	libsvm.svm_node svmNodeTemp = new libsvm.svm_node();

	svmNodeTemp.index = featureId;

	svmNodeTemp.value = 10.7;
      
	Feature returnedFeature = SVMUtils.nodeToFeature(svmNodeTemp, m_testDataIdFactory);

	assertNotNull(returnedFeature);

	assertEquals(featureStrName, returnedFeature.toString());
      
	libsvm.svm_node svmNodeTemp2 = new libsvm.svm_node();

	svmNodeTemp.index = 100;

	svmNodeTemp.value = 10.7;
      
	Feature returnedFeature2 = SVMUtils.nodeToFeature(svmNodeTemp2, m_testDataIdFactory);

	assertNull(returnedFeature2);

    }

    public void testInstanceToNodeArray()
    {

	System.out.println("    -- Testing InstanceToNodeArray(...) --");
	  
	//set up the parameters passed into the tested function
	int nodesCount = 3;//at least 3	

        libsvm.svm_node[] svmNodes = new libsvm.svm_node[nodesCount];
	
        int[] featureId = {1, 2, 3};// size of array = nodesCount
	
        double[] featureWeight = new double[nodesCount];
	
        boolean[] isChecked = new boolean[nodesCount];
	
        String[] featureStrName = new String[nodesCount];
	
        for(int index = 0 ; index < nodesCount ; ++index) {
	    
            featureStrName[index] = m_testDataIdFactory.getFeature(featureId[index]).toString();
	    
            featureWeight[index] = 3.1 + (double)index;

	    svmNodes[index] = new libsvm.svm_node();

	    svmNodes[index].index = featureId[index];

	    svmNodes[index].value = featureWeight[index];

	    isChecked[index] = false;

	}
	  
	// Calling tested function
	Instance returnedInstance = SVMUtils.nodeArrayToInstance(svmNodes, m_testDataIdFactory);

	// Check returned object
	assertNotNull(returnedInstance);
	
        checkReturnedInstance(returnedInstance, featureStrName, featureWeight, nodesCount, isChecked);
	  
	svmNodes[2].index = 100;//incorrect id

	Instance returnedInstance2 = SVMUtils.nodeArrayToInstance(svmNodes, m_testDataIdFactory);

	assertNull(returnedInstance2);

    }
  
    private void checkReturnedInstance(Instance returnedInstance, String[] featureStrName, double[] featureWeight, int nodesCount, boolean[] isChecked) {
	for (Feature.Looper fl = returnedInstance.numericFeatureIterator(); fl.hasNext();) {
		  
	    Feature returnedFeature = fl.nextFeature();
		  
	    boolean isFound = false;

	    for(int index = 0; index < nodesCount; ++index) {

		if(featureStrName[index].equals(returnedFeature.toString())) {

		    isFound = true;
				  
		    assertEquals(featureWeight[index], returnedInstance.getWeight(returnedFeature));
				  
		    isChecked[index] = true;
		}
	    }
		  
	    if(!isFound) {

		assertNotNull(returnedFeature.toString() + " is not from node array.", null);

	    }
	}
	  
	for(int index = 0; index < nodesCount; ++index) {

	    if (!isChecked[index]) {

		assertNotNull("svm node with feature name (" + featureStrName[index] + ") is not generated in instance.", null);

	    }
	}
    }
  
    /**
     * Creates a TestSuite from all testXXX methods
     * @return TestSuite
     */
    public static Test suite()
    {

	return new TestSuite(SVMUtilsTest.class);

    }

    private static Dataset setTestDatasets()
    {
	Random r = new Random();

	Dataset result = new BasicDataset();

	String[][] what = new String[][] {
	    { "bad", "slow", "mistake", "complain", "angry", "stress" },
	    { "good", "excellent", "potentical", "new", "many", "conquare", "trial" } };

	String[] who = new String[] { "critisize", "appreciate" };

	for (int i = 0; i < 10; i++) {

	    int ci = r.nextInt(2);

	    int ni = r.nextInt(2) + 1;

	    MutableInstance instance = new MutableInstance();

	    for (int j = 0; j < ni; j++) {

		int wj = r.nextInt(what[ci].length);

		instance.addBinary(new Feature(new String[] { "testdata",
							      what[ci][wj] }));
	    }

	    result.add(new Example(instance, new ClassLabel(who[ci])));

	}

	return result;  
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