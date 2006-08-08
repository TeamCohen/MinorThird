package edu.cmu.minorthird.classify.algorithms.svm;

/**
 * modificaiton made to wrap the idFactory in the classifier
 */

import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.FeatureIdFactory;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.BinaryClassifier;
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
 *
 * @author ksteppe
 */
public class SVMClassifier extends BinaryClassifier
{
    private svm_model model;
    private FeatureIdFactory idFactory;

    public double score(Instance instance) {
        //need the nodeArray
        svm_node[] nodeArray = SVMUtils.instanceToNodeArray(instance, idFactory);
        double prediction;

        // If the model is set to calculate probability estimates (aka confidences) then
        //   create an array of doubles of length 2 (because this is a binary classifier)
        //   and use the predict_probability method which returns that class and fills in 
        //   the probability array passed in.
        if (svm.svm_check_probability_model(model) == 1) {
            double[] probs = new double[2];
            prediction = svm.svm_predict_probability(model, nodeArray, probs);
            
            // We want to return the probability estimates embedded in the prediction.  The actual
            //   value will go into the ClassLabel as the labels weight and since this is a binary 
            //   classifier the probability estimate of the other class is 1 - |prediction|.
            // Also, the svm_predict_* methods return 1 or -1 for the binary case so all we need to
            //   do to is return the svm_predict result multiplied by the highest probability.
            if (probs[0] > probs[1])
                prediction = prediction * probs[0];
            else
                prediction = prediction * probs[1];
        }
        // Otherwise just call the predict method, which simply returns the class.  This
        //   method is faster than predict_probability.
        else {
            prediction = svm.svm_predict(model, nodeArray);
        }

        return prediction;
    }

    public String explain(Instance instance) {
        return "I have no idea how I came up with this answer";
    }

    public Explanation getExplanation(Instance instance) {
	Explanation ex = new Explanation(explain(instance));
	return ex;
    }

    public SVMClassifier(svm_model model, FeatureIdFactory idFactory)
    {
        this.model = model;
        this.idFactory = idFactory;
    }
}
