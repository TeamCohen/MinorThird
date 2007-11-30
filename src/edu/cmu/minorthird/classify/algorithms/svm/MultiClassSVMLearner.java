package edu.cmu.minorthird.classify.algorithms.svm;

import edu.cmu.minorthird.classify.FeatureIdFactory;
import edu.cmu.minorthird.classify.BatchClassifierLearner;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.ExampleSchema;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import org.apache.log4j.Logger;

/**
 * Wraps the svm.svm_train algorithm from libsvm for multi-class problems
 * (http://www.csie.ntu.edu.tw/~cjlin/libsvm/)
 * <p/>
 * Parameterization is done via a svm_parameter object (see initParameters or libsvm
 * docs for examples/info).
 * <p/>
 * There are a few setParameterXXX methods to do some changes.  Use these after calling new SVMLearner()
 * and before starting training.
 *
 * @author qcm
 */
public class MultiClassSVMLearner extends BatchClassifierLearner{

	private svm_model model;

	private FeatureIdFactory idFactory;

	private svm_parameter parameters;

	private ExampleSchema exampleSchema;

	Logger log=Logger.getLogger(SVMLearner.class);

	/**
	 * construct learner using given params
	 *
	 * @param params svm_parameter
	 */
	public MultiClassSVMLearner(svm_parameter params){
		parameters=params;
	}

	/**
	 * default constructor
	 */
	public MultiClassSVMLearner(){
		initParameters();
	}

	final public void setSchema(ExampleSchema schema){
		exampleSchema=schema;
	}

	/**
	 * Train a classifier using the given dataset.
	 * An svm_problem object is created from the dataset.  A svm_model is generated
	 * by the svm library.  That model is held by the returned Classifier.
	 *
	 * @param dataset Dataset representing all usable training data
	 * @return a SVMClassifier object which wraps the libsvm prediction code
	 */
	public Classifier batchTrain(Dataset dataset){
		try{
			//train up the svm on the dataset
			idFactory=new FeatureIdFactory(dataset);
			svm_problem problem=
					SVMUtils.convertToMultiClassSVMProblem(dataset,idFactory,dataset
							.getSchema());
			model=svm.svm_train(problem,parameters);

		}catch(Exception e){
			log.error(e,e);
		}

		//now I need to construct a Classifier out of the svm_model
		return new MultiClassSVMClassifier(model,idFactory,dataset.getSchema());
	}

	/**
	 * sets the default parameters for the svm
	 * <p/>
	 * use the setParameterXXX methods to adjust them
	 */
	protected void initParameters(){
		parameters=new svm_parameter();
		// default values
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
	}

	// Stuff for the GUI

	/**
	 * @param type integer from the svm_parameter class
	 */
	public void setParameterSVMType(int type){
		parameters.svm_type=type;
	}

	public int getParameterSVMType(){
		return parameters.svm_type;
	}

	public String parameterSVMTypeHelp=new String("Set the SVM type to use.");

	public String getParameterSVMTypeHelp(){
		return parameterSVMTypeHelp;
	}

	public void setKernelType(int type){
		parameters.kernel_type=type;
	}

	public int getKernelType(){
		return parameters.kernel_type;
	}

	public String kernelTypeHelp=new String("Set the type of kernel function.");

	public String getKernelTypeHelp(){
		return kernelTypeHelp;
	}

	public void setDegree(int deg){
		parameters.degree=deg;
	}

	public int getDegree(){
		return parameters.degree;
	}

	public String degreeHelp=new String("Set the degree in kernel function.");

	public String getDegreeHelp(){
		return degreeHelp;
	}

	public void setGamma(double g){
		parameters.gamma=g;
	}

	public double getGamma(){
		return parameters.gamma;
	}

	public String gammaHelp=new String("Set the gamma in kernel function.");

	public String getGammaHelp(){
		return gammaHelp;
	}

	public void setCoef0(double c){
		parameters.coef0=c;
	}

	public double getCoef0(){
		return parameters.coef0;
	}

	public String coef0Help=new String("Set the coef0 in kernel function.");

	public String getCoef0Help(){
		return coef0Help;
	}

	public void setNu(double n){
		parameters.nu=n;
	}

	public double getNu(){
		return parameters.nu;
	}

	public String nuHelp=
			new String(
					"Set the parameter nu. (For nu-SVC, one-class SVM, and nu-SVR only)");

	public String getNuHelp(){
		return nuHelp;
	}

	public void setCacheSize(double s){
		parameters.cache_size=s;
	}

	public double getCacheSize(){
		return parameters.cache_size;
	}

	public String cacheSizeHelp=new String("Set the cache memory size in MB.");

	public String getCacheSizeHelp(){
		return cacheSizeHelp;
	}

	public void setCParameter(double c){
		parameters.C=c;
	}

	public double getCParameter(){
		return parameters.C;
	}

	public String cParameterHelp=
			new String(
					"Set the parameter C. (For C-SVC, epsilon-SVR, and nu-SVR only)");

	public String getCParameterHelp(){
		return cParameterHelp;
	}

	public void setStoppingCriteria(double c){
		parameters.eps=c;
	}

	public double getStoppingCriteria(){
		return parameters.eps;
	}

	public String stoppingCriteriaHelp=
			new String("Set the tolerance of termination criterion.");

	public String getStoppingCriteriaHelp(){
		return stoppingCriteriaHelp;
	}

	public void setLossFunctionEpsilon(double l){
		parameters.p=l;
	}

	public double getLossFunctionEpsilon(){
		return parameters.p;
	}

	public String lossFunctionEpsilonHelp=
			new String("Set the epsilon in the loss function of epsilon-SVR.");

	public String getLossFunctionEpsilonHelp(){
		return lossFunctionEpsilonHelp;
	}

	public void setUseShrinkingHeuristics(boolean flag){
		if(flag)
			parameters.shrinking=1;
		else
			parameters.shrinking=0;
	}

	public boolean getUseShrinkingHeuristics(){
		if(parameters.shrinking==0)
			return false;
		return true;
	}

	public String useShrinkingHeuristicsHelp=
			new String("Whether or not to use shrinking heuristics.");

	public String getUseShrinkingHeuristicsHelp(){
		return useShrinkingHeuristicsHelp;
	}

	public void setCParameterWeight(int w){
		parameters.nr_weight=w;
	}

	public int getCParameterWeight(){
		return parameters.nr_weight;
	}

	public String cParameterWeightHelp=
			new String("Set the parameter C of class i to weight*C for C-SVC.");

	public String getCParameterWeightHelp(){
		return cParameterWeightHelp;
	}

	public void setDoProbabilityEstimates(boolean flag){
		if(flag)
			parameters.probability=1;
		else
			parameters.probability=0;
	}

	public boolean getDoProbabilityEstimates(){
		if(parameters.probability==0)
			return false;
		return true;
	}

	public String doProbabilityEstimatesHelp=
			new String(
					"Whether to train for probability estimates. (For SVC and SVR models only).");

	public String getDoProbabilityEstimatesHelp(){
		return doProbabilityEstimatesHelp;
	}

	/**
	 * Get the underlying svm_model object.  See libsvm for documentation details
	 */
	public svm_model getModel(){
		return model;
	}

	public FeatureIdFactory getIdFactory(){
		return this.idFactory;
	}

}
