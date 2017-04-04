package edu.cmu.minorthird.classify.algorithms.svm;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.FeatureFactory;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.classify.SampleDatasets;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;

/**
 *
 * This class is responsible for testing VisibleSVM class.
 *
 * @author chiachi
 */
public class VisibleSVMTest extends TestCase{

	Logger log=Logger.getLogger(this.getClass());

	svm_model m_toy1Model;
	svm_model m_toy2Model;

	FeatureFactory m_toy1FeatureFactory;
	FeatureFactory m_toy2FeatureFactory;

	ExampleSchema m_toy2ExampleSchema;

	MutableInstance[] m_toy1Instances;
	MutableInstance[] m_toy2Instances;

	/**
	 * Standard test class constructior for VisibleSVMTest
	 * @param name Name of the test
	 */
	public VisibleSVMTest(String name){
		super(name);
		createTestSettings();
	}

	/**
	 * Convinence constructior for VisibleSVMTest
	 */
	public VisibleSVMTest(){
		super("VisibleSVMTest");
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

	public void testConstructorWithTwoParams(){
		System.out.println("Testing Constructor for SVMLearner...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy1Model,m_toy1FeatureFactory);
		assertNotNull(testedVSVM);
	}

	public void testConstructorWithThreeParams(){
		System.out.println("Testing Constructor for MultiClassSVMLearner...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy2Model,m_toy2FeatureFactory,m_toy2ExampleSchema);
		assertNotNull(testedVSVM);
	}

	public void testGetExamples(){
		System.out.println("Testing getExamples()...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy1Model,m_toy1FeatureFactory);
		Example[] returnedExamples=testedVSVM.getExamples();
		assertTrue(isReturnedDataMatched(returnedExamples,false));
	}

	public void testGetExamplesMultiClass(){
		System.out.println("Testing getExamples() for MultiClassSVM...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy2Model,m_toy2FeatureFactory,m_toy2ExampleSchema);
		Example[] returnedExamples=testedVSVM.getExamples();
		assertEquals(returnedExamples.length,m_toy2Instances.length);

		// need to deal with this crap
		System.out.println(">>> "+isReturnedDataMatched(returnedExamples,true));
		assertTrue(isReturnedDataMatched(returnedExamples,true));
		//assertTrue(isToy3MatchedReturnedData(returnedExamples));
	}

	public void testGetExampleWeightLabels(){
		System.out.println("Testing getExampleWeightLabels()...");

		VisibleSVM testedVSVM=new VisibleSVM(m_toy1Model,m_toy1FeatureFactory);
		String[][] returnedLabels=testedVSVM.getExampleWeightLabels();
		libsvm.m3gateway gate=new libsvm.m3gateway(m_toy1Model);

		double[][] weights=gate.getCoefficientsForSVsInDecisionFunctions();
		double[] rlTemp=new double[returnedLabels.length];
		double[] rw=new double[weights[0].length];

		DecimalFormat df=new DecimalFormat("0.0000");
		for(int index=0;index<weights[0].length;++index){
			rlTemp[index]=Double.parseDouble(returnedLabels[index][0]);
			rw[index]=Double.parseDouble(df.format(weights[0][index]));
			assertEquals(rw[index],rlTemp[index]);
		}
	}

	public void testGetExampleWeightLabelsMultiClass(){
		System.out.println("Testing getExampleWeightLabels() for MultiClassSVM...");

		VisibleSVM testedVSVM=new VisibleSVM(m_toy2Model,m_toy2FeatureFactory);
		String[][] returnedLabels=testedVSVM.getExampleWeightLabels();
		libsvm.m3gateway gate=new libsvm.m3gateway(m_toy2Model);

		double[][] weights=gate.getCoefficientsForSVsInDecisionFunctions();
		double[] rlTemp=new double[returnedLabels.length];
		double[] rw=new double[weights[0].length];

		for(int k=0;k<weights.length;++k){
			DecimalFormat df=new DecimalFormat("0.0000");
			for(int index=0;index<weights[0].length;++index){
				if((k==1&&index==4)||returnedLabels[index][k]=="null"){
					rlTemp[index]=0.0;
					rw[index]=0.0;
				}else{
					rlTemp[index]=Double.parseDouble(returnedLabels[index][k]);
					rw[index]=Double.parseDouble(df.format(weights[k][index]));
				}
				assertEquals(rw[index],rlTemp[index]);
			}
		}
	}

	public void testGetHyperplane(){
		System.out.println("Testing getHyperplane()...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy1Model,m_toy1FeatureFactory);
		Hyperplane hp=testedVSVM.getHyperplane(0);
		assertNotNull(hp);
	}

	public void testGetHyperplaneMultiClass(){
		System.out.println("Testing getHyperplane() for MultiClassSVM...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy2Model,m_toy2FeatureFactory,m_toy2ExampleSchema);
		Hyperplane hp1=testedVSVM.getHyperplane(0);
		Hyperplane hp2=testedVSVM.getHyperplane(1);
		assertNotNull(hp1);
		assertNotNull(hp2);
	}

	public void testGetHyperplaneLabel(){
		System.out.println("Testing getHyperplaneLabel()...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy1Model,m_toy1FeatureFactory);
		String returnedHPLabels=testedVSVM.getHyperplaneLabel(0);
		assertTrue(returnedHPLabels.equals(""));
	}

	public void testGetHyperplaneLabelMultiClass(){
		System.out.println("Testing getHyperplaneLabel() for MultiClassSVM...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy2Model,m_toy2FeatureFactory,m_toy2ExampleSchema);
		String label1=testedVSVM.getHyperplaneLabel(0);
		String label2=testedVSVM.getHyperplaneLabel(1);
		assertEquals("marge vs. homer",label1);
		assertEquals("marge vs. bart",label2);
	}

	public void testToGUI(){
		System.out.println("Testing toGUI()...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy1Model,m_toy1FeatureFactory);
		assertNotNull(testedVSVM.toGUI());
	}

	public void testToGUIMultiClass(){
		System.out.println("Testing toGUI() for MultiClassSVM...");
		VisibleSVM testedVSVM=new VisibleSVM(m_toy2Model,m_toy2FeatureFactory,m_toy2ExampleSchema);
		assertNotNull(testedVSVM.toGUI());
	}

	public void testGetHyperplaneOutOfBounds(){
		System.out.println("Testing GetHyperplane() out of bounds...");
		try{
			VisibleSVM testedVSVM=new VisibleSVM(m_toy1Model,m_toy1FeatureFactory);
			testedVSVM.getHyperplane(3);
			fail("Hyperplane retrieved using out of bounds index!");
		}catch(IllegalArgumentException success){
			assertNotNull(success.getMessage());
		}
	}

	public void testGetHyperplaneMultiClassOutOfBounds(){
		System.out.println("Testing GetHyperplane() out of bounds for MultiClassSVM...");
		try{
			VisibleSVM testedVSVM=new VisibleSVM(m_toy2Model,m_toy2FeatureFactory,m_toy2ExampleSchema);
			testedVSVM.getHyperplane(4);
			fail("Hyperplane retrieved using out of bounds index!");
		}catch(IllegalArgumentException success){
			assertNotNull(success.getMessage());
		}
	}

	public void testGetHyperplaneLabelOutOfBounds(){
		System.out.println("Testing GetHyperplaneLabel() out of bounds...");
		try{
			VisibleSVM testedVSVM=new VisibleSVM(m_toy1Model,m_toy1FeatureFactory);
			testedVSVM.getHyperplaneLabel(3);
			fail("HPLabel retrieved using out of bounds index!");
		}catch(IllegalArgumentException success){
			assertNotNull(success.getMessage());

		}
	}

	public void testGetHyperplaneLabelMultiClassOutOfBounds(){
		System.out.println("Testing GetHyperplaneLabel() out of bounds for MultiClassSVM...");
		try{
			VisibleSVM testedVSVM=new VisibleSVM(m_toy2Model,m_toy2FeatureFactory,m_toy2ExampleSchema);
			testedVSVM.getHyperplaneLabel(4);
			fail("HPLabel retrieved using out of bounds index!");
		}catch(IllegalArgumentException success){
			assertNotNull(success.getMessage());
		}
	}

	/**
	 * Creates a TestSuite from all testXXX methods
	 * @return TestSuite
	 */
	public static Test suite(){
		return new TestSuite(VisibleSVMTest.class);
	}

	private Dataset makeToy1Dataset(){
		Dataset result=SampleDatasets.sampleData("toy",false);
		m_toy1Instances=new MutableInstance[result.size()];
		Iterator<Example> it=result.iterator();
		for(int i=0;it.hasNext();i++){
			Example example1=it.next();
			Iterator<Feature> fLoop=example1.featureIterator();
			m_toy1Instances[i]=new MutableInstance();
			for(int j=0;fLoop.hasNext();j++){
				Feature f=fLoop.next();
				m_toy1Instances[i].addNumeric(f,example1.getWeight(f));
			}
		}
		return result;
	}

	private Dataset makeToy2Dataset(){
		
		Dataset result=SampleDatasets.makeToy3ClassData(new Random(100),5);
		
		m_toy2Instances=new MutableInstance[result.size()];
		Iterator<Example> it=result.iterator();
		for(int i=0;it.hasNext();i++){
			Example example1=it.next();
			Iterator<Feature> fLoop=example1.featureIterator();
			m_toy2Instances[i]=new MutableInstance();
			for(int j=0;fLoop.hasNext();j++){
				Feature f=fLoop.next();
				m_toy2Instances[i].addNumeric(f,example1.getWeight(f));
			}
		}
		return result;

	}

	private void createTestSettings(){
		
		//init parameters, exactly the same as initparams in SVMLearner, and MultiClassSVMLearner
		svm_parameter parameters=new svm_parameter();
		parameters.svm_type=svm_parameter.C_SVC;
		parameters.kernel_type=svm_parameter.LINEAR;
		parameters.degree=3;
		parameters.gamma=0; // 1/k
		parameters.coef0=0;
		parameters.nu=0.5;
		parameters.cache_size=40;
		parameters.C=1;
		parameters.eps=1e-3;
		parameters.p=0.1;
		parameters.shrinking=1;
		parameters.nr_weight=0;
		parameters.weight_label=new int[0];
		parameters.weight=new double[0];
		parameters.probability=0;

		Dataset dataset1=makeToy1Dataset();
		m_toy1FeatureFactory=dataset1.getFeatureFactory();
		svm_problem problem1=SVMUtils.convertToSVMProblem(dataset1);
		m_toy1Model=svm.svm_train(problem1,parameters);

		Dataset dataset2=makeToy2Dataset();
		m_toy2FeatureFactory=dataset2.getFeatureFactory();
		m_toy2ExampleSchema=dataset2.getSchema();
		svm_problem problem2=SVMUtils.convertToSVMProblem(dataset2);
		m_toy2Model=svm.svm_train(problem2,parameters);

	}
	
	private boolean isReturnedDataMatched(Example[] returnedExamples,boolean isMultiClassSVM){
		MutableInstance[] instances;
		if(isMultiClassSVM){
			instances=m_toy2Instances;
		}else{
			instances=m_toy1Instances;
		}

		int featureCount=10;
		String[][] originalNames=new String[instances.length][featureCount];
		int[][] originalNumNames=new int[instances.length][featureCount];
		for(int index=0;index<instances.length;++index){
			int subidx=0;
			String temphold="";
			for(Iterator<Feature> flidx1=instances[index].featureIterator();flidx1
			.hasNext();){
				Feature ftemp1=flidx1.next();
				originalNames[index][subidx]=ftemp1.toString();
				originalNumNames[index][subidx]=ftemp1.numericName();
				temphold+=originalNames[index][subidx]+" ";
				++subidx;
			}
		}

		boolean[] exampleChecked=new boolean[returnedExamples.length];
		String[][] returnedNames=new String[returnedExamples.length][featureCount];
		int[][] returnedNumNames=new int[returnedExamples.length][featureCount];
		for(int index=0;index<returnedExamples.length;++index){
			exampleChecked[index]=false;
			int subidx=0;
			String temphold="";
			for(Iterator<Feature> flidx1=returnedExamples[index].featureIterator();flidx1
			.hasNext();){
				Feature ftemp1=flidx1.next();
				returnedNames[index][subidx]=ftemp1.toString();
				returnedNumNames[index][subidx]=ftemp1.numericName();
				temphold+=returnedNames[index][subidx]+" ";
				++subidx;
			}
		}

		int[] matchedMap=new int[instances.length];
		for(int index=0;index<exampleChecked.length;++index){
			for(int idx=0;idx<originalNames.length;++idx){
				if(originalNames[idx].length==returnedNames[index].length){
					Arrays.sort(originalNumNames[idx]);
					Arrays.sort(returnedNumNames[index]);
					if(Arrays.equals(originalNumNames[idx],returnedNumNames[index])){
						matchedMap[idx]=index+1;
						exampleChecked[index]=true;
						idx=originalNames.length;

					}
				}
			}
		}

		for(int index=0;index<exampleChecked.length;++index){
			if(!exampleChecked[index]){
				return false;
			}
		}

		return true;

	}

	/**
	 * Run the full suite of tests with text output
	 * @param args - unused
	 */
	public static void main(String args[]){
		junit.textui.TestRunner.run(suite());
	}
}