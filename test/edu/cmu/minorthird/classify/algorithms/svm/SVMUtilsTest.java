package edu.cmu.minorthird.classify.algorithms.svm;

import java.util.Iterator;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import libsvm.svm_node;

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

	static Logger logger=Logger.getLogger(SVMUtilsTest.class);

	FeatureFactory featureFactory;

	/**
	 * Standard test class constructior for SVMUtilsTest
	 * @param name Name of the test
	 */
	public SVMUtilsTest(String name){
		super(name);
		Dataset testDataSet=createTestDataset();
		featureFactory=testDataSet.getFeatureFactory();
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

		System.out.println("Testing nodeToFeature()...");
		
		System.out.println("FeatureFactory:");
		System.out.println(featureFactory);
		
		// test case 1
		
		libsvm.svm_node svmNodeTemp=new libsvm.svm_node();
		svmNodeTemp.index=1;
		svmNodeTemp.value=10.7;

		Feature testFeature=featureFactory.getFeature(svmNodeTemp.index-1);
		String featureStrName=testFeature.toString();

		Feature returnedFeature=SVMUtils.nodeToFeature(svmNodeTemp,featureFactory);
		
		System.out.println("Feature Index "+svmNodeTemp.index+": "+returnedFeature);

		assertNotNull(returnedFeature);
		assertEquals(featureStrName,returnedFeature.toString());

		// test case 2
		
		libsvm.svm_node svmNodeTemp2=new libsvm.svm_node();
		svmNodeTemp2.index=100;
		svmNodeTemp2.value=10.7;

		Feature returnedFeature2=SVMUtils.nodeToFeature(svmNodeTemp2,featureFactory);

		System.out.println("Feature Index "+svmNodeTemp2.index+": "+returnedFeature2);
		
		assertNull(returnedFeature2);
		
		System.out.println("Done.");

	}

	public void testNodeArrayToInstance(){

		System.out.println("Testing nodeArrayToInstance()...");
		
		String[] featureNames=new String[3];
		svm_node[] nodes=new svm_node[3];
		for(int i=0;i<nodes.length;i++){
			nodes[i]=new svm_node();
			nodes[i].index=(i+1);
			nodes[i].value=3.1+(double)i;
			featureNames[i]=featureFactory.getFeature(i).toString();
		}

		// calling method and check returned object
		Instance instance=SVMUtils.nodeArrayToInstance(nodes,featureFactory);
		assertNotNull(instance);
		checkInstance(instance,featureNames,nodes);

		// call method with incorrect id
		nodes[2].index=100;
		instance=SVMUtils.nodeArrayToInstance(nodes,featureFactory);
		assertNull(instance);
		
		System.out.println("Done.");

	}

	private static void checkInstance(Instance instance,String[] featureNames,svm_node[] nodes){
		for(Iterator<Feature> it=instance.numericFeatureIterator();it.hasNext();){
			Feature feature=it.next();
			boolean found=false;
			for(int i=0;i<nodes.length;i++){
				if(featureNames[i].equals(feature.toString())){
					found=true;
					assertEquals(nodes[i].value,instance.getWeight(feature));
				}
			}
			assertTrue(found);
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