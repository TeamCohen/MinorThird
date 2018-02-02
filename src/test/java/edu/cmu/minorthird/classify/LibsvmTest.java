package edu.cmu.minorthird.classify;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.algorithms.svm.SVMClassifier;
import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;
import edu.cmu.minorthird.classify.experiments.Evaluation;

/**
 *
 * This class is responsible for testing Libsvm wrappers
 *
 * @author ksteppe
 */
public class LibsvmTest extends AbstractClassificationChecks{

	Logger log=Logger.getLogger(this.getClass());

	private static final String trainFile="edu/cmu/minorthird/classify/testcases/a1a.dat";
	//private static final String model="modelFile.dat";
	private static final String testFile="edu/cmu/minorthird/classify/testcases/a1a.t.dat";

	/**
	 * Standard test class constructior for LibsvmTest
	 * @param name Name of the test
	 */
	public LibsvmTest(String name){
		super(name);
	}

	/**
	 * Convinence constructior for LibsvmTest
	 */
	public LibsvmTest(){
		super("LibsvmTest");
	}

	/**
	 * setUp to run before each test
	 */
	protected void setUp(){
		org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
		org.apache.log4j.BasicConfigurator.configure();
		log.setLevel(Level.DEBUG);
		super.setCheckStandards(false);
		//TODO add initializations if needed
	}

	/**
	 * clean up to run after each test
	 */
	protected void tearDown(){
		//TODO clean up resources if needed
	}

	/**
	 * use wrapper on the provided data, should get same results
	 * as the direct
	 */
	public void testWrapper(){
		try{
			//get datasets
			URL url=this.getClass().getClassLoader().getResource(trainFile);
			Dataset trainData=DatasetLoader.loadSVMStyle(new File(new URI(url.toExternalForm())));
			url=this.getClass().getClassLoader().getResource(testFile);
			Dataset testData=DatasetLoader.loadSVMStyle(new File(new URI(url.toExternalForm())));

			//send expectations to checkClassifyText()
			double[] expect=
				new double[]{
					0.13769470404984424,
					0.6011745705024105,
					0.6934812760055479,
					// should be infinity if not calculating probabilities
					// 1.3132616875183545,
					Double.POSITIVE_INFINITY,
				};
			super.setCheckStandards(true);
			super.checkClassify(new SVMLearner(),trainData,testData,expect);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * run the svm wrapper on the sample data
	 */
	public void testSampleData(){

		double[] refs=new double[]{
				0.0,0.0,0.0,0.0,0.0,0.0,0.0, //0-6 are 0
				1.0,1.0, //7-8 are 1
				1.3132616875182228,1.0,1.0,1.0, //10-12 are 1
				1.0 //13 is 1
		}; 

		super.checkClassify(new SVMLearner(),SampleDatasets.toyTrain(),
				SampleDatasets.toyTest(),refs);
	}

	/**
	 *  Test a full cycle of training, testing, saving (serializing), loading, and testing again.
	 **/
	public void testSerialization(){
		try{
			// Create a classifier using the SVMLearner and the toyTrain dataset
			SVMLearner l=new SVMLearner();
			Classifier c1=
				new DatasetClassifierTeacher(SampleDatasets.toyTrain()).train(l);
			File tempFile=File.createTempFile("SVMTest","classifier");

			// Evaluate it immediately saving the stats
			Evaluation e1=new Evaluation(SampleDatasets.toyTrain().getSchema());
			e1.extend(c1,SampleDatasets.toyTest(),1);
			double[] stats1=new double[4];
			stats1[0]=e1.errorRate();
			stats1[1]=e1.averagePrecision();
			stats1[2]=e1.maxF1();
			stats1[3]=e1.averageLogLoss();

			// Serialize the classifier to disk
			//ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("SVMTest.classifier")));
			ObjectOutputStream out=
				new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
						tempFile)));
			out.writeObject(c1);
			out.flush();
			out.close();

			// Load it back in.
			//ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("SVMTest.classifier")));
			ObjectInputStream in=
				new ObjectInputStream(new BufferedInputStream(new FileInputStream(
						tempFile)));
			Classifier c2=(Classifier)in.readObject();
			in.close();

			// Evaluate again saving the stats
			Evaluation e2=new Evaluation(SampleDatasets.toyTrain().getSchema());
			e2.extend(c2,SampleDatasets.toyTest(),1);
			//double[] stats2 = e2.summaryStatistics();
			double[] stats2=new double[4];
			stats2[0]=e2.errorRate();
			stats2[1]=e2.averagePrecision();
			stats2[2]=e2.maxF1();
			stats2[3]=e2.averageLogLoss();

			// Only use the basic stats for now because some of the advanced stats
			//  come back as NaN for both datasets and the check stats method can't
			//  handle NaN's
			log.info("using Standard stats only (4 of them)");

			// Compare the stats produced from each run to make sure they are identical
			checkStats(stats1,stats2);

			// Remove the temporary classifier file
			tempFile.delete();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Test the MultiClass classification stuff.  There are two cases to consider: with and without 
	 * calculation of probability estimates.  The libsvm documentation states that these two cases
	 * may return different classifications.  These tests simply classify a sample dataset and 
	 * check the stats produced against expected values.
	 */
	public void testMultiClassClassification(){
		Dataset trainSet=SampleDatasets.makeToy3ClassData(new Random(12345),100);
		Dataset testSet=SampleDatasets.makeToy3ClassData(new Random(67890),100);

		try{
			// Create a classifier using the SVMLearner and the toyTrain dataset
			SVMLearner l=new SVMLearner();

			// First run the test without probability estimates
			l.setDoProbabilityEstimates(false);
			SVMClassifier c1=
				(SVMClassifier)(new DatasetClassifierTeacher(trainSet)
				.train(l));
			Evaluation e1=new Evaluation(trainSet.getSchema());
			e1.extend(c1,testSet,1);
			double[] stats1=new double[4];
			stats1[0]=e1.errorRate();
			stats1[1]=e1.averagePrecision();
			stats1[2]=e1.maxF1();
			stats1[3]=e1.averageLogLoss();

			System.out.println("Error Rate: "+e1.errorRate());
			System.out.println("Avg Precision: "+e1.averagePrecision());
			System.out.println("Max F1: "+e1.maxF1());
			System.out.println("Avg Log Loss: "+e1.averageLogLoss());

			// The stats we expect the classification to return.
			double[] expected=new double[4];
			expected[0]=0.07;
			expected[1]=-1.0;
			expected[2]=-1.0;
			expected[3]=Double.POSITIVE_INFINITY;

			// Compare the stats produced from the run without probability estimates with expected values;
			checkStats(stats1,expected);

			//
			// On a small dataset libsvm may return vastly different stats from run to run so for now 
			//  this test is commented out.
			//
			// Now do it with probability estimates
			l.setDoProbabilityEstimates(true);
			SVMClassifier c2=
				(SVMClassifier)(new DatasetClassifierTeacher(trainSet)
				.train(l));
			Evaluation e2=new Evaluation(trainSet.getSchema());
			e2.extend(c2,testSet,1);
			double[] stats2=new double[4];
			stats2[0]=e2.errorRate();
			stats2[1]=e2.averagePrecision();
			stats2[2]=e2.maxF1();
			stats2[3]=e2.averageLogLoss();

			System.out.println("Error Rate2: "+e2.errorRate());
			System.out.println("Avg Precision2: "+e2.averagePrecision());
			System.out.println("Max F1-2: "+e2.maxF1());
			System.out.println("Avg Log Loss2: "+e2.averageLogLoss());

			// The stats we expect the classification to return.
			expected[0]=0.08;
			expected[1]=-1.0;
			expected[2]=-1.0;
			expected[3]=1.194999431381944;

			// Compare the stats produced from the run with probability estimates with expected values.  The libsvm
			//  package doesn't always come up with the "exact" same stats, but they are within 0.05 of each other
			//  so update the delta acordingly.
			setDelta(0.05);
			checkStats(stats2,expected);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Creates a TestSuite from all testXXX methods
	 * @return TestSuite
	 */
	public static Test suite(){
		return new TestSuite(LibsvmTest.class);
	}

	/**
	 * Run the full suite of tests with text output
	 * @param args - unused
	 */
	public static void main(String args[]){
		junit.textui.TestRunner.run(suite());
	}

//	// Crap from svm_predict.java
//	private double[] predict(BufferedReader input,DataOutputStream output,
//			svm_model model) throws IOException{
//		int correct=0;
//		int total=0;
//		double error=0;
//		double sumv=0,sumy=0,sumvv=0,sumyy=0,sumvy=0;
//
//		while(true){
//			String line=input.readLine();
//			if(line==null)
//				break;
//
//			StringTokenizer st=new StringTokenizer(line," \t\n\r\f:");
//
//			double target=atof(st.nextToken());
//			int m=st.countTokens()/2;
//			svm_node[] x=new svm_node[m];
//			for(int j=0;j<m;j++){
//				x[j]=new svm_node();
//				x[j].index=atoi(st.nextToken());
//				x[j].value=atof(st.nextToken());
//			}
//			double v=svm.svm_predict(model,x);
//			if(v==target)
//				++correct;
//			error+=(v-target)*(v-target);
//			sumv+=v;
//			sumy+=target;
//			sumvv+=v*v;
//			sumyy+=target*target;
//			sumvy+=v*target;
//			++total;
//
////			output.writeBytes(v+"\n");
//		}
//		log.debug("Accuracy = "+(double)correct/total*100+"% ("+correct+"/"+total+
//		") (classification)\n");
//		log.debug("Mean squared error = "+error/total+" (regression)\n");
//		log.debug("Squared correlation coefficient = "+
//				((total*sumvy-sumv*sumy)*(total*sumvy-sumv*sumy))/
//				((total*sumvv-sumv*sumv)*(total*sumyy-sumy*sumy))+" (regression)\n");
//
//		double[] rvalues=new double[3];
//		rvalues[0]=(double)correct/(double)total;
//		rvalues[1]=error/(double)total;
//		rvalues[2]=
//			((total*sumvy-sumv*sumy)*(total*sumvy-sumv*sumy))/
//			((total*sumvv-sumv*sumv)*(total*sumyy-sumy*sumy));
//
//		return rvalues;
//
//	}

//	private double[] prediction(String argv[]) throws IOException{
//		if(argv.length!=3){
//			System.err.print("usage: svm-predict test_file model_file output_file\n");
//			System.exit(1);
//		}
//
//		BufferedReader input=new BufferedReader(new FileReader(argv[0]));
//		DataOutputStream output=new DataOutputStream(new FileOutputStream(argv[2]));
//		svm_model model=svm.svm_load_model(argv[1]);
//		return predict(input,output,model);
//	}

//	private static double atof(String s){
//		return Double.valueOf(s).doubleValue();
//	}
//
//	private static int atoi(String s){
//		return Integer.parseInt(s);
//	}

}
