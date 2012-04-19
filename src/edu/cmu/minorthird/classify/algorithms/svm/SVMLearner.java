package edu.cmu.minorthird.classify.algorithms.svm;

import java.io.IOException;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BatchClassifierLearner;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.ExampleSchema;

/**
 * Wraps the svm.svm_train algorithm from libsvm
 * (http://www.csie.ntu.edu.tw/~cjlin/libsvm/)
 * <p/>
 * Parameterization is done via an SVM object (see libsvm docs for examples/info).
 * <p/>
 * There are a few setParameterXXX methods to do some changes.  Use these after calling new SVMLearner()
 * and before starting training.
 *
 * @author ksteppe, Frank Lin
 */

public class SVMLearner extends BatchClassifierLearner{

	static Logger logger=Logger.getLogger(SVMLearner.class);
	
	private svm_parameter parameters;
	private ExampleSchema schema;

	/**
	 * Construct learner using given params
	 *
	 * @param parameters parameters to the SVM
	 */
	
	public SVMLearner(svm_parameter parameters){
		this.parameters=parameters;
	}

	/**
	 * default constructor
	 */
	public SVMLearner(){
		this(getDefaultParameters());
	}
	
	/**
	 * sets the default parameters for the svm
	 * <p/>
	 * use the setParameterXXX methods to adjust them
	 */
	protected static svm_parameter getDefaultParameters(){
		svm_parameter p=new svm_parameter();
		// default values
		p.svm_type=svm_parameter.C_SVC;
		p.kernel_type=svm_parameter.LINEAR;
		p.degree=3;
		p.gamma=0; // 1/k
		p.coef0=0;
		p.nu=0.5;
		p.cache_size=40;
		p.C=1;
		p.eps=1e-3;
		p.p=0.1;
		p.shrinking=1;
		p.nr_weight=0;
		p.weight_label=new int[0];
		p.weight=new double[0];
		p.probability=0;
		return p;
	}
	
	@Override
	public void setSchema(ExampleSchema schema){
		this.schema=schema;
	}
	
	@Override
	public ExampleSchema getSchema(){
		return schema;
	}

	/**
	 * Train a classifier using the given dataset.
	 * An svm_problem object is created from the dataset.  A svm_model is generated
	 * by the svm library.  That model is held by the returned Classifier.
	 *
	 * @param dataset Dataset representing all usable training data
	 * @return a SVMClassifier object which wraps the libsvm prediction code
	 */
	@Override
	public Classifier batchTrain(Dataset dataset){
			// train the svm on the dataset
			svm_problem problem=SVMUtils.convertToSVMProblem(dataset);
			svm_model model=svm.svm_train(problem,parameters);
			// why do we save a model here when debugging?
			if(logger.isDebugEnabled()){
				try{
				svm.svm_save_model("./modelTest.mdl",model);
				}
				catch(IOException ioe){
					ioe.printStackTrace();
				}
			}
			// construct a Classifier out of the svm_model
			return new SVMClassifier(model,dataset.getSchema(),dataset.getFeatureFactory());
	}

	public void setParameterSVMType(int type){
		parameters.svm_type=type;
	}

	public int getParameterSVMType(){
		return parameters.svm_type;
	}

	public static String parameterSVMTypeHelp="Set the SVM type to use.";

	public String getParameterSVMTypeHelp(){
		return parameterSVMTypeHelp;
	}

	public void setKernelType(int type){
		parameters.kernel_type=type;
	}

	public int getKernelType(){
		return parameters.kernel_type;
	}

	public static String kernelTypeHelp="Set the type of kernel function.";

	public String getKernelTypeHelp(){
		return kernelTypeHelp;
	}

	public void setDegree(int deg){
		parameters.degree=deg;
	}

	public int getDegree(){
		return parameters.degree;
	}

	public static String degreeHelp="Set the degree in kernel function.";

	public String getDegreeHelp(){
		return degreeHelp;
	}

	public void setGamma(double g){
		parameters.gamma=g;
	}

	public double getGamma(){
		return parameters.gamma;
	}

	public static String gammaHelp="Set the gamma in kernel function.";

	public String getGammaHelp(){
		return gammaHelp;
	}

	public void setCoef0(double c){
		parameters.coef0=c;
	}

	public double getCoef0(){
		return parameters.coef0;
	}

	public static String coef0Help="Set the coef0 in kernel function.";

	public String getCoef0Help(){
		return coef0Help;
	}

	public void setNu(double n){
		parameters.nu=n;
	}

	public double getNu(){
		return parameters.nu;
	}

	public static String nuHelp="Set the parameter nu. (For nu-SVC, one-class SVM, and nu-SVR only)";

	public String getNuHelp(){
		return nuHelp;
	}

	public void setCacheSize(double s){
		parameters.cache_size=s;
	}

	public double getCacheSize(){
		return parameters.cache_size;
	}

	public static String cacheSizeHelp="Set the cache memory size in MB.";

	public String getCacheSizeHelp(){
		return cacheSizeHelp;
	}

	public void setCParameter(double c){
		parameters.C=c;
	}

	public double getCParameter(){
		return parameters.C;
	}

	public static String cParameterHelp="Set the parameter C. (For C-SVC, epsilon-SVR, and nu-SVR only)";

	public String getCParameterHelp(){
		return cParameterHelp;
	}

	public void setStoppingCriteria(double c){
		parameters.eps=c;
	}

	public double getStoppingCriteria(){
		return parameters.eps;
	}

	public static String stoppingCriteriaHelp="Set the tolerance of termination criterion.";

	public String getStoppingCriteriaHelp(){
		return stoppingCriteriaHelp;
	}

	public void setLossFunctionEpsilon(double l){
		parameters.p=l;
	}

	public double getLossFunctionEpsilon(){
		return parameters.p;
	}

	public static String lossFunctionEpsilonHelp="Set the epsilon in the loss function of epsilon-SVR.";

	public String getLossFunctionEpsilonHelp(){
		return lossFunctionEpsilonHelp;
	}

	public void setUseShrinkingHeuristics(boolean flag){
		if(flag){
			parameters.shrinking=1;
		}
		else{
			parameters.shrinking=0;
		}
	}

	public boolean getUseShrinkingHeuristics(){
		return parameters.shrinking>0;
	}

	public static String useShrinkingHeuristicsHelp="Whether or not to use shrinking heuristics.";

	public String getUseShrinkingHeuristicsHelp(){
		return useShrinkingHeuristicsHelp;
	}

	public void setCParameterWeight(int w){
		parameters.nr_weight=w;
	}

	public int getCParameterWeight(){
		return parameters.nr_weight;
	}

	public static String cParameterWeightHelp="Set the parameter C of class i to weight*C for C-SVC.";

	public String getCParameterWeightHelp(){
		return cParameterWeightHelp;
	}

	/**
	 * Tell the learner to train a classifier capable of computing probability estimates
	 * for each class. Default to False.  Turning this option on will cause the training 
	 * to take a longer time.
	 *
	 * @param flag Boolean value telling the learner whether or not to compute probability estimates
	 */
	public void setDoProbabilityEstimates(boolean flag){
		if(flag){
			parameters.probability=1;
		}
		else{
			parameters.probability=0;
		}
	}

	public boolean getDoProbabilityEstimates(){
		return parameters.probability>0;
	}

	public static String doProbabilityEstimatesHelp="Whether to train for probability estimates. (For SVC and SVR models only).";

	public String getDoProbabilityEstimatesHelp(){
		return doProbabilityEstimatesHelp;
	}

	// C, gamma, kernel_type

	/**
	 * Default kernel type is linear
	 *
	 * @param type integer from the svm_parameter class
	 */
	public void setParameterKernelType(int type){
		parameters.kernel_type=type;
	}

	/**
	 * The default for Gamma is 0, which works for a linear kernel, but not for
	 * other types of kernels
	 *
	 * @param gamma double to be used as the gamma parameter.  Default is 0
	 */
	public void setParameterGamma(double gamma){
		parameters.gamma=gamma;
	}

	/**
	 * @param c double to be used as the C parameter.  Default is 1
	 */
	public void setParameterC(double c){
		parameters.C=c;
	}

}
