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
import edu.cmu.minorthird.classify.SampleDatasets;
import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import libsvm.svm;
import libsvm.svm_node;
import libsvm.svm_model;
import libsvm.svm_problem;
import libsvm.svm_parameter;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.io.*;
import java.util.Arrays;


/**
 *
 * This class is responsible for testing VisibleSVM class.
 *
 * @author chiachi
 */
public class VisibleSVMTest extends TestCase
{
    Logger log = Logger.getLogger(this.getClass());
  
    svm_model        m_toyModel;
  
    svm_model        m_toy3Model;
  
    FeatureIdFactory m_toyIdFactory;
  
    FeatureIdFactory m_toy3IdFactory;
  
    ExampleSchema    m_toy3ExampleSchema;

    MutableInstance[]  m_toyInstances;
  
    MutableInstance[]  m_toy3Instances;
  
    /**
     * Standard test class constructior for VisibleSVMTest
     * @param name Name of the test
     */
    public VisibleSVMTest(String name)
    {
	super(name);

	setTestedDataset();
    
    }

    /**
     * Convinence constructior for VisibleSVMTest
     */
    public VisibleSVMTest()
    {
	super("VisibleSVMTest");
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

    public void testConstructorWithTwoParams() 
    {
	System.out.println("    -- Testing Construtor for SVMLearner --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toyModel, m_toyIdFactory);
	  
	assertNotNull(testedVSVM);
    }

    public void testConstructorWithThreeParams()
    {
	System.out.println("    -- Testing Constructor for MultiClassSVMLearner --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toy3Model, m_toy3IdFactory, m_toy3ExampleSchema);
	  
	assertNotNull(testedVSVM);
    }


    public void testGetExamples() 
    {
	System.out.println("    -- Testing getExamples(...) --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toyModel, m_toyIdFactory);
	  
	Example[] returnedExamples = testedVSVM.getExamples();
	  
	assertTrue(isReturnedDataMatched(returnedExamples, false));
    }

    public void testGetExamplesMultiClass() 
    {
	System.out.println("    -- Testing getExamples(...) for MultiClassSVM --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toy3Model, m_toy3IdFactory, m_toy3ExampleSchema);
	  
	Example[] returnedExamples = testedVSVM.getExamples();
	  
	assertEquals(returnedExamples.length, m_toy3Instances.length);
	  
	assertTrue(isToy3MatchedReturnedData(returnedExamples));
    }

    public void testGetExampleWeightLabels() 
    {
	System.out.println("    -- Testing getExampleWeightLabels(...) --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toyModel, m_toyIdFactory);
	  
	String[][] returnedLabels = testedVSVM.getExampleWeightLabels();
	  
	libsvm.m3gateway gate = new libsvm.m3gateway(m_toyModel);
	  
	double[][] weights = gate.getCoefficientsForSVsInDecisionFunctions();
	  
	double[] rlTemp = new double[returnedLabels.length];
	  
	double[] rw = new double[weights[0].length];
	  
	DecimalFormat df = new DecimalFormat("0.0000");
	  
	for(int index = 0; index < weights[0].length; ++index) {
		  
	    rlTemp[index] = Double.parseDouble(returnedLabels[index][0]);
		  
	    rw[index] = Double.parseDouble(df.format(weights[0][index]));
		  
	    assertEquals(rw[index], rlTemp[index]);
	}
    }
  
    public void testGetExampleWeightLabelsMultiClass() 
    {
	System.out.println("    -- Testing getExampleWeightLabels(...) for MultiClassSVM --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toy3Model, m_toy3IdFactory);
	  
	String[][] returnedLabels = testedVSVM.getExampleWeightLabels();
	  
	libsvm.m3gateway gate = new libsvm.m3gateway(m_toy3Model);
	  
	double[][] weights = gate.getCoefficientsForSVsInDecisionFunctions();
	  
	double[] rlTemp = new double[returnedLabels.length];
	  
	double[] rw = new double[weights[0].length];
	  
	for(int k = 0 ; k < weights.length ; ++k) {
		  
	    DecimalFormat df = new DecimalFormat("0.0000");
		  
	    for(int index = 0; index < weights[0].length; ++index) {
			  
		if ((k == 1 && index == 4) || returnedLabels[index][k] == "null") {
				  
		    rlTemp[index] = 0.0;
				  
		    rw[index] = 0.0;
			  
		} else {
				  
		    rlTemp[index] = Double.parseDouble(returnedLabels[index][k]);
				  
		    rw[index] = Double.parseDouble(df.format(weights[k][index]));
		}
			  
		assertEquals(rw[index], rlTemp[index]);
	    }
	}
    }
  

    public void testGetHyperplane()
    {
	System.out.println("    -- Testing getHyperplane(...) --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toyModel, m_toyIdFactory);
	  
	Hyperplane hp = testedVSVM.getHyperplane(0);
	  
	assertNotNull(hp);
    }

    public void testGetHyperplaneMultiClass()
    {
	System.out.println("    -- Testing getHyperplane(...) for MultiClassSVM --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toy3Model, m_toy3IdFactory, m_toy3ExampleSchema);
	  
	Hyperplane hp1 = testedVSVM.getHyperplane(0);
	  
	Hyperplane hp2 = testedVSVM.getHyperplane(1);
	  
	assertNotNull(hp1);
	  
	assertNotNull(hp2);
    }

    public void testGetHyperplaneLabel() 
    {
	System.out.println("    -- Testing getHyperplaneLabel(...) --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toyModel, m_toyIdFactory);
	  
	String returnedHPLabels = testedVSVM.getHyperplaneLabel(0);
	  
	assertTrue(returnedHPLabels.equals(""));
	  
    }

    public void testGetHyperplaneLabelMultiClass() 
    {
	System.out.println("    -- Testing getHyperplaneLabel(...) for MultiClassSVM --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toy3Model, m_toy3IdFactory, m_toy3ExampleSchema);
	
	String label1 = testedVSVM.getHyperplaneLabel(0);
	  
	String label2 = testedVSVM.getHyperplaneLabel(1);
	  
	assertEquals("marge vs. homer", label1);
	  
	assertEquals("marge vs. bart", label2);

    }
  
    public void testToGUI()
    {
	System.out.println("    -- Testing toGUI() --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toyModel, m_toyIdFactory);
	  
	assertNotNull(testedVSVM.toGUI());
    }
  
    public void testToGUIMultiClass()
    {
	System.out.println("    -- Testing toGUI() for MultiClassSVM --");
	  
	VisibleSVM testedVSVM = new VisibleSVM(m_toy3Model, m_toy3IdFactory, m_toy3ExampleSchema);
	  
	assertNotNull(testedVSVM.toGUI());
    }
  
    public void testGetHyperplaneOutOfBound() {

	System.out.println("    -- Testing GetHyperplane() Out of bound --");
	  
	try {
	    VisibleSVM testedVSVM = new VisibleSVM(m_toyModel, m_toyIdFactory);
		  
	    Hyperplane returnedHPLabels = testedVSVM.getHyperplane(3);
		  
	    fail("Hyperplane retrieved by out of bound index!");
	}
	catch (IllegalArgumentException success) {
	    	
	    assertNotNull(success.getMessage());
	        
	}
    }

    public void testGetHyperplaneMultiClassOutOfBound() {

	System.out.println("    -- Testing GetHyperplane() MultiClass Out of bound --");
	  
	try {
	    VisibleSVM testedVSVM = new VisibleSVM(m_toy3Model, m_toy3IdFactory, m_toy3ExampleSchema);
		  
	    Hyperplane returnedHPLabels = testedVSVM.getHyperplane(5);
		  
	    fail("Hyperplane retrieved by out of bound index!");
	}
	catch (IllegalArgumentException success) {
	    	
	    assertNotNull(success.getMessage());
	        
	}
    }
  
    public void testGetHyperplaneLabelOutOfBound() {

	System.out.println("    -- Testing GetHyperplaneLabel() Out of bound --");
	  
	try {
	    VisibleSVM testedVSVM = new VisibleSVM(m_toyModel, m_toyIdFactory);
		  
	    String returnedHPLabels = testedVSVM.getHyperplaneLabel(3);
		  
	    fail("HPLabel retrieved by out of bound index!");
	}
	catch (IllegalArgumentException success) {
	    	
	    assertNotNull(success.getMessage());
	        
	}
    }

    public void testGetHyperplaneLabelMultiClassOutOfBound() {

	System.out.println("    -- Testing GetHyperplaneLabel() MultiClass Out of bound --");
	  
	try {
	    VisibleSVM testedVSVM = new VisibleSVM(m_toy3Model, m_toy3IdFactory, m_toy3ExampleSchema);
		  
	    String returnedHPLabels = testedVSVM.getHyperplaneLabel(5);
		  
	    fail("HPLabel retrieved by out of bound index!");
	}
	catch (IllegalArgumentException success) {
	    	
	    assertNotNull(success.getMessage());
	        
	}
    }
  
    /**
     * Creates a TestSuite from all testXXX methods
     * @return TestSuite
     */
    public static Test suite()
    {
	return new TestSuite(VisibleSVMTest.class);
    }

    private Dataset makeToyDataset()
    {  
	Dataset result = SampleDatasets.sampleData("toy", false);
	  
	m_toyInstances = new MutableInstance[result.size()];
      
	Example.Looper it = result.iterator();
      
	for (int i = 0; it.hasNext(); i++) {

	    Example example1 = it.nextExample();
      	
	    Feature.Looper fLoop = example1.featureIterator();
      	
	    m_toyInstances[i] = new MutableInstance();
      	
	    for(int j = 0; fLoop.hasNext(); j++) {

      		Feature f = fLoop.nextFeature();
      		
      		m_toyInstances[i].addNumeric( f, example1.getWeight(f));
	    }
	} 

	return result;
    }
  
    private Dataset makeToy3Dataset()
    {
	Random r = new Random(100);
	
	m_toy3Instances = new MutableInstance[5];
    
	Dataset result = new BasicDataset();
    
	String[][] what =
	    new String[][]{ { "money","cash","sleep","booze","chocolate","fun","beer","pizza" },
			    { "stocks","bonds","money","cash","influence","power","fame" },
			    { "chocolate","beer","pizza","pringles","popcorn","spam","crisco" } };
    
	String[] who = new String[] { "homer","marge","bart" };
    
	for (int i = 0 ; i < 5 ; i++) {
      
	    int ci = r.nextInt(3);
      
	    int ni = r.nextInt(3)+2;

	    m_toy3Instances[i] = new MutableInstance();

	    for (int j=0; j<ni; j++) {
        
		int wj = r.nextInt( what[ci].length );
        
		m_toy3Instances[i].addNumeric( new Feature(new String[]{ "word",what[ci][wj] }), 1.0);
      
	    }
      
	    result.add( new Example(m_toy3Instances[i], new ClassLabel(who[ci]) ) );
	}
	
	return result;
    }
  
    private void setTestedDataset()
    {
	//init parameters, exactly the same as initparams in SVMLearner, and MultiClassSVMLearner
	svm_parameter parameters = new svm_parameter();
	parameters.svm_type = svm_parameter.C_SVC;
	parameters.kernel_type = svm_parameter.LINEAR;
	parameters.degree = 3;
	parameters.gamma = 0; // 1/k
	parameters.coef0 = 0;
	parameters.nu = 0.5;
	parameters.cache_size = 40;
	parameters.C = 1;
	parameters.eps = 1e-3;
	parameters.p = 0.1;
	parameters.shrinking = 1;
	parameters.nr_weight = 0;
	parameters.weight_label = new int[0];
	parameters.weight = new double[0];
	parameters.probability = 0;

	//init dataset, toy and toy3 returned from SampleDatasets
	Dataset dataset1 = makeToyDataset();
	  
	m_toyIdFactory = new FeatureIdFactory(dataset1);
      
	svm_problem problem1 = SVMUtils.convertToSVMProblem(dataset1, m_toyIdFactory);
      
	m_toyModel = svm.svm_train(problem1, parameters);

	Dataset dataset2 = makeToy3Dataset();
	  
	m_toy3IdFactory = new FeatureIdFactory(dataset2);
	  
	m_toy3ExampleSchema = dataset2.getSchema();
      
	svm_problem problem2 = SVMUtils.convertToMultiClassSVMProblem(dataset2, m_toy3IdFactory, m_toy3ExampleSchema);
      
	m_toy3Model = svm.svm_train(problem2, parameters);

    }

    private boolean isToy3MatchedReturnedData(Example[] returnedExamples)
    {
	if (returnedExamples.length != m_toy3Instances.length) {
		
	    return false;
	  
	}
	  
	String[][] originalNames = new String[m_toy3Instances.length][5];
	  
	int[][] originalNumNames = new int[m_toy3Instances.length][5];
	  
	for(int index = 0 ; index < m_toy3Instances.length ; ++index) {
		  
	    int subidx = 0;
			
	    for (Feature.Looper flidx1 = m_toy3Instances[index].featureIterator(); flidx1.hasNext(); ) {			
				
		Feature ftemp1 = flidx1.nextFeature();
			  
		originalNames[index][subidx] = ftemp1.toString();
			  
		originalNumNames[index][subidx] = ftemp1.numericName();
			  
		++subidx;
			  
	    }
	}
	  
	boolean[]  exampleChecked = new boolean[returnedExamples.length];
	  
	String[][] returnedNames = new String[returnedExamples.length][5];
	  
	int[][] returnedNumNames = new int[returnedExamples.length][5];
	  
	for(int index = 0 ; index < returnedExamples.length ; ++index) {
		  
	    exampleChecked[index] = false;
		  
	    int subidx = 0;
			
	    for (Feature.Looper flidx1 = returnedExamples[index].featureIterator(); flidx1.hasNext(); ) {			
				
		Feature ftemp1 = flidx1.nextFeature();
				
		returnedNames[index][subidx] = ftemp1.toString();
				
		returnedNumNames[index][subidx] = ftemp1.numericName();
			    
		++subidx;
	    }
	}
	  
	int[] matchedMap = new int[m_toy3Instances.length];
	  
	for(int index = 0 ; index < matchedMap.length ; ++index) {
		  
	    int comparedIndex = 0;
		  
	    while(comparedIndex < (matchedMap.length - 1) && exampleChecked[comparedIndex]) {
			  
		++comparedIndex;
		  
	    }
		  
	    boolean found = false;
		  
	    boolean continued = true;
		  
	    while(!found && continued && comparedIndex < matchedMap.length) {
			  
		for(int idx = 0; idx < 5; ++idx) {
				  
		    if (originalNames[index][idx] == null && returnedNames[comparedIndex][idx] == null && continued == true) {
					  
			idx = 5;
				  
		    } else if ( originalNames[index][idx] == null || returnedNames[comparedIndex][idx] == null || 
				!originalNames[index][idx].equals(returnedNames[comparedIndex][idx]) ||
				originalNumNames[index][idx] != returnedNumNames[comparedIndex][idx] ) {
					  
			idx = 5;
					  
			continued = false;
				  
		    }
		}
			  
		if (continued) {
				  
		    found = true;
				  
		    matchedMap[index] = comparedIndex + 1;
				  
		    exampleChecked[comparedIndex] = true;
			  
		} else {
				  
		    if (comparedIndex < (matchedMap.length - 1)) {
					  
			++comparedIndex;
				  
		    } else {
					  
			return false;
				  
		    }
				  
		    while(comparedIndex < (matchedMap.length - 1) && exampleChecked[comparedIndex]) {
					  
			++comparedIndex;
        		  
		    }
				  
		    continued = true;
		}
	    }
		  
	    if (!found) {
			  
		return false;
		  
	    }
	}
	  
	for(int index = 0 ; index < exampleChecked.length ; ++index) {
		  
	    if (!exampleChecked[index]) {
			  
		return false;
		  
	    }
	}
	  
	return true;
    }
  
    private boolean isReturnedDataMatched(Example[] returnedExamples, boolean isMultiClassSVM)
    {
	MutableInstance[] instances;
	  
	if (isMultiClassSVM) {
		  
	    instances = m_toy3Instances;
	  
	} else {
		  
	    instances = m_toyInstances;
	  
	}
	  
	int featureCount = 10;
	  
	String[][] originalNames = new String[instances.length][featureCount];
	  
	int[][] originalNumNames = new int[instances.length][featureCount];
	  
	for(int index = 0 ; index < instances.length ; ++index) {
		  
	    int subidx = 0;
		  
	    String temphold = "";
		  
	    for (Feature.Looper flidx1 = instances[index].featureIterator(); flidx1.hasNext(); ) {			
			
		Feature ftemp1 = flidx1.nextFeature();
			
		originalNames[index][subidx] = ftemp1.toString();
			
		originalNumNames[index][subidx] = ftemp1.numericName();
			
		temphold += originalNames[index][subidx] + " ";
			
		++subidx;
	    }
	}
	  
	boolean[]  exampleChecked = new boolean[returnedExamples.length];

	String[][] returnedNames = new String[returnedExamples.length][featureCount];
	  
	int[][] returnedNumNames = new int[returnedExamples.length][featureCount];
	  
	for(int index = 0 ; index < returnedExamples.length ; ++index) {
		  
	    exampleChecked[index] = false;
		  
	    int subidx = 0;
		  
	    String temphold = "";
			
	    for (Feature.Looper flidx1 = returnedExamples[index].featureIterator(); flidx1.hasNext(); ) {
			  
		Feature ftemp1 = flidx1.nextFeature();
			  
		returnedNames[index][subidx] = ftemp1.toString();
			  
		returnedNumNames[index][subidx] = ftemp1.numericName();
			  
		temphold += returnedNames[index][subidx] + " ";
			  
		++subidx;
	    }
	}
	  
	int[] matchedMap = new int[instances.length];
	  
	for(int index = 0 ; index < exampleChecked.length ; ++index) {
		  
	    for(int idx = 0 ; idx < originalNames.length ; ++idx) {
			  
		if (originalNames[idx].length == returnedNames[index].length) {
				  
		    Arrays.sort(originalNumNames[idx]);
				  
		    Arrays.sort(returnedNumNames[index]);
				  
		    if(Arrays.equals(originalNumNames[idx], returnedNumNames[index])) {
					 
			matchedMap[idx] = index + 1;
					  
			exampleChecked[index] = true;
					  
			idx = originalNames.length;
				  
		    }
		}
	    }
	}
	  
	for(int index = 0 ; index < exampleChecked.length ; ++index) {
		  
	    if (!exampleChecked[index]) {
			  
		return false;
	    }
	}
	  
	return true;

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