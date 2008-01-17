package edu.cmu.minorthird.classify.algorithms.svm;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.FeatureFactory;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;

/**
 *
 * This class is responsible for testing nodeToFeature and nodeArrayToInstance
 * functions from SVMUtils class.
 *
 * @author chiachi
 */
public class SVMUtilsTest extends TestCase{

	Logger log=Logger.getLogger(this.getClass());

	FeatureFactory m_testDataFeatureFactory;

	/**
	 * Standard test class constructior for SVMUtilsTest
	 * @param name Name of the test
	 */
	public SVMUtilsTest(String name){
		super(name);
		Dataset testDataSet=createTestDataset();
		m_testDataFeatureFactory=testDataSet.getFeatureFactory();
	}

	/**
	 * Convinence constructior for SVMUtilsTest
	 */
	public SVMUtilsTest(){
		super("SVMUtilsTest");
	}

	/**
	 * setUp to run before each test
	 */
	protected void setUp(){
		Logger.getRootLogger().removeAllAppenders();
		org.apache.log4j.BasicConfigurator.configure();
		//TODO add initializations if needed
	}

	/**
	 * clean up to run after each test
	 */
	protected void tearDown(){
		//TODO clean up resources if needed
	}

	public void testNodeToFeature(){

		System.out.println("Testing NodeToFeature()...");
		
		System.out.println("FeatureFactory:");
		System.out.println(m_testDataFeatureFactory);
		
		// test case 1
		
		libsvm.svm_node svmNodeTemp=new libsvm.svm_node();
		svmNodeTemp.index=0;
		svmNodeTemp.value=10.7;

		Feature testFeature=m_testDataFeatureFactory.getFeature(svmNodeTemp.index);
		String featureStrName=testFeature.toString();

		Feature returnedFeature=SVMUtils.nodeToFeature(svmNodeTemp,m_testDataFeatureFactory);
		
		System.out.println("Feature Index "+svmNodeTemp.index+": "+returnedFeature);

		assertNotNull(returnedFeature);
		assertEquals(featureStrName,returnedFeature.toString());

		// test case 2
		
		libsvm.svm_node svmNodeTemp2=new libsvm.svm_node();
		svmNodeTemp2.index=100;
		svmNodeTemp2.value=10.7;

		Feature returnedFeature2=SVMUtils.nodeToFeature(svmNodeTemp2,m_testDataFeatureFactory);

		System.out.println("Feature Index "+svmNodeTemp2.index+": "+returnedFeature2);
		
		assertNull(returnedFeature2);
		
		System.out.println("Done.");

	}

	public void testInstanceToNodeArray(){

		System.out.println("Testing InstanceToNodeArray()...");

		//set up the parameters passed into the tested function
		int numNodes=3; //at least 3

		libsvm.svm_node[] svmNodes=new libsvm.svm_node[numNodes];

		int[] featureId={0,1,2}; // size of array = numNodes

		double[] featureWeight=new double[numNodes];

		boolean[] isChecked=new boolean[numNodes];

		String[] featureStrName=new String[numNodes];

		for(int index=0;index<numNodes;index++){
			featureStrName[index]=m_testDataFeatureFactory.getFeature(featureId[index]).toString();
			featureWeight[index]=3.1+(double)index;
			svmNodes[index]=new libsvm.svm_node();
			svmNodes[index].index=featureId[index];
			svmNodes[index].value=featureWeight[index];
			isChecked[index]=false;
		}

		// Calling tested function
		Instance returnedInstance=SVMUtils.nodeArrayToInstance(svmNodes,m_testDataFeatureFactory);

		// Check returned object
		assertNotNull(returnedInstance);
		checkReturnedInstance(returnedInstance,featureStrName,featureWeight,numNodes,isChecked);

		svmNodes[2].index = 100;//incorrect id
		Instance returnedInstance2 = SVMUtils.nodeArrayToInstance(svmNodes, m_testDataFeatureFactory);
		assertNull(returnedInstance2);
		
		System.out.println("Done.");

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

	private static Dataset createTestDataset(){
		
		int numInstances=10;
		int numMaxFeatures=10;

		Random random=new Random();

		String[][] features=new String[][]{
				{"bad","slow","mistake","complain","angry","stress"},
				{"good","excellent","potentical","new","many","conquer","trial"}
				};

		String[] labels=new String[]{
				"critisize",
				"appreciate"
				};
		
		Dataset dataset=new BasicDataset();

		for(int i=0;i<numInstances;i++){
			MutableInstance instance=new MutableInstance();
			int numFeatures=random.nextInt(numMaxFeatures)+1;
			int labelIndex=random.nextInt(labels.length);
			for(int j=0;j<numFeatures;j++){
				int featureIndex=random.nextInt(features[labelIndex].length);
				instance.addBinary(new Feature(new String[]{"testdata",features[labelIndex][featureIndex]}));
			}
			dataset.add(new Example(instance,new ClassLabel(labels[labelIndex])));
		}
		
		return dataset;
		
	}
	
	/**
	 * Creates a TestSuite from all testXXX methods
	 * @return TestSuite
	 */
	public static Test suite(){
		return new TestSuite(SVMUtilsTest.class);
	}

	/**
	 * Run the full suite of tests with text output
	 * @param args - unused
	 */
	public static void main(String args[]){
		junit.textui.TestRunner.run(suite());
	}

}