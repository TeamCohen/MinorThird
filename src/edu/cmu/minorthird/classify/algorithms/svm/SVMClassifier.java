package edu.cmu.minorthird.classify.algorithms.svm;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Instance;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import java.io.*;

/**
 * SVMClassifier wrapps the prediction code from the libsvm library
 * It implements the Classifier interface so that using libsvm should be identical
 * to any other Classifer.  A SVMClassifier must be built from a model, using
 * the svm_model class from libsvm.  This is best done by running the learner.
 * @author ksteppe
 */
public class SVMClassifier implements Classifier
{
  private svm_model model;

  public ClassLabel classification(Instance instance)
  {
    //need the nodeArray
    svm_node[] nodeArray = SVMUtils.instanceToNodeArray(instance);

    double prediction = svm.svm_predict(model, nodeArray);
    return ClassLabel.binaryLabel(prediction);
  }

  public String explain(Instance instance)
  {
    return "I have no idea how I came up with this answer";
  }

  public SVMClassifier(svm_model model)
  {
    this.model = model;
  }
}
