package edu.cmu.minorthird.classify.algorithms.svm;

import edu.cmu.minorthird.classify.BatchBinaryClassifierLearner;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import libsvm.*;
import org.apache.log4j.Logger;

/**
 * Wraps the svm.svm_train algorithm from libsvm
 * (http://www.csie.ntu.edu.tw/~cjlin/libsvm/)
 *
 * Parameterization is done via a svm_parameter object (see initParameters or libsvm
 * docs for examples/info).
 *
 * There are a few setParameterXXX methods to do some changes.  Use these after calling new SVMLearner()
 * and before starting training.
 *
 * @author ksteppe
 */
public class SVMLearner extends BatchBinaryClassifierLearner
{
  private svm_model model;
  private svm_parameter parameters;
  Logger log = Logger.getLogger(SVMLearner.class);

  /**
   * construct learner using given params
   * @param params svm_parameter
   */
  public SVMLearner(svm_parameter params)
  {
    parameters = params;
  }

  /**
   * default constructor
   */
  public SVMLearner()
  {
    initParameters();
  }

  /**
   * Train a classifier using the given dataset.
   * An svm_problem object is created from the dataset.  A svm_model is generated
   * by the svm library.  That model is held by the returned Classifier.
   * @param dataset Dataset representing all usable training data
   * @return a SVMClassifier object which wraps the libsvm prediction code
   */
  public Classifier batchTrain(Dataset dataset)
	{
    try
    { //train up the svm on the dataset
      svm_problem problem = SVMUtils.convertToSVMProblem(dataset);
      model = svm.svm_train(problem, parameters);

      if (log.isDebugEnabled())
        svm.svm_save_model("./modelTest.mdl", model);
    }
    catch (Exception e)
    {
      log.error(e, e);
    }

    //now I need to construct a Classifier out of the svm_model
		return new SVMClassifier(model);
	}


  /**
   * sets the default parameters for the svm
   *
   * use the setParameterXXX methods to adjust them
   */
  protected void initParameters()
  {
    parameters = new svm_parameter();
    // default values
    parameters.svm_type = svm_parameter.C_SVC;
    parameters.kernel_type = svm_parameter.LINEAR;
    parameters.degree = 3;
    parameters.gamma = 0;	// 1/k
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
  }


  //C, gamma, kernel_type
  /**
   * @param type integer from the svm_parameter class
   */
  public void setParameterSVMType(int type)
  { parameters.svm_type = type; }

  /**
   * Default kernel type is linear
   * @param type integer from the svm_parameter class
   */
  public void setParameterKernelType(int type)
  { parameters.kernel_type = type; }

  /**
   * The default for Gamma is 0, which works for a linear kernel, but not for
   * other types of kernels
   * @param gamma double to be used as the gamma parameter.  Default is 0
   */
  public void setParameterGamma(double gamma)
  { parameters.gamma = gamma; }

  /**
   * @param c double to be used as the C parameter.  Default is 1
   */
  public void setParameterC(double c)
  { parameters.C = c; }

  /**
   * Get the underlying svm_model object.  See libsvm for documentation details
   * @return
   */
  public svm_model getModel()
  { return model; }
}
