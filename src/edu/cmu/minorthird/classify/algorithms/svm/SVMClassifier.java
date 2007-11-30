package edu.cmu.minorthird.classify.algorithms.svm;

import java.io.Serializable;

import javax.swing.JComponent;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.FeatureIdFactory;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * SVMClassifier wrapps the prediction code from the libsvm library for binary problems
 * A SVMClassifier must be built from a model, using the svm_model class from libsvm.  
 * This is best done by running the learner. <br>
 * <br>
 * Note that due to the way libsvm computes probabilities you may get different
 * predictions for the same instance if you turn on probabilities compared to
 * when you leave it turned off.  See the libsvm home page for more details.
 *
 * @author qcm
 */
public class SVMClassifier extends BinaryClassifier implements Visible,
		Serializable{

	static final long serialVersionUID=20071130L;
	
	private FeatureIdFactory m_featureIdFactory;

	private svm_model m_svmModel;

	public SVMClassifier(svm_model model,FeatureIdFactory idFactory){
		m_featureIdFactory=idFactory;
		m_svmModel=model;
	}

	public String explain(Instance instance){
		return "I have no idea how I came up with this answer";
	}

	public Explanation getExplanation(Instance instance){
		Explanation ex=new Explanation(explain(instance));
		return ex;
	}

	public FeatureIdFactory getIdFactory(){

		return m_featureIdFactory;
	}

	public svm_model getSVMModel(){

		return m_svmModel;
	}

	/**
	 * Computes the predicted weight for an instance.  The sign of the score indicates
	 * the class (>0 = POS and <0 = NEG) and the absolute value of the score is the
	 * weight in logits of this prediction.
	 */
	public double score(Instance instance){

		svm_node[] nodeArray=
				SVMUtils.instanceToNodeArray(instance,m_featureIdFactory);
		double prediction;

		// If the model is set to calculate probability estimates (aka confidences) then
		//   create an array of doubles of length 2 (because this is a binary classifier)
		//   and use the predict_probability method which returns that class and fills in 
		//   the probability array passed in.
		if(svm.svm_check_probability_model(m_svmModel)==1){
			double[] probs=new double[2];
			prediction=svm.svm_predict_probability(m_svmModel,nodeArray,probs);

			// We want to return the probability estimates embedded in the prediction.  The actual
			//   value will go into the ClassLabel as the labels weight and since this is a binary 
			//   classifier the probability estimate of the other class is 1 - |prediction|.
			// Also, the svm_predict_* methods return 1 or -1 for the binary case, but we need the 
			//   probability of the prediction (given in the prob[]), then we need to convert this
			//   probability into logits (logit = p/1-p).  Finally we need to multiply by the
			//   prediction (1 or -1) to embedd the predicted class into the weight.
			if(probs[0]>probs[1])
				prediction=prediction*(Math.log(probs[0]/(1-probs[0])));
			else
				prediction=prediction*(Math.log(probs[1]/(1-probs[1])));
		}
		// Otherwise just call the predict method, which simply returns the class.  This
		//   method is faster than predict_probability.
		else{
			prediction=svm.svm_predict(m_svmModel,nodeArray);
		}
		return prediction;
	}

	/************************************************************************
	 * GUI
	 *************************************************************************/
	public Viewer toGUI(){

		SVMViewer svmViewer0=new SVMViewer();

		svmViewer0.setContent(this);

		return svmViewer0;

	}

	static private class SVMViewer extends ComponentViewer{
		
		static final long serialVersionUID=20071130L;

		public boolean canReceive(Object o){

			return o instanceof SVMClassifier;
		}

		public JComponent componentFor(Object o){

			final SVMClassifier svmClassifier1=(SVMClassifier)o;

			// transform to visible SVM
			VisibleSVM vsSVMtemp=
					new VisibleSVM(svmClassifier1.getSVMModel(),svmClassifier1
							.getIdFactory());

			return vsSVMtemp.toGUI();
		}
	}
}
