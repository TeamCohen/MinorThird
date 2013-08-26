package edu.cmu.minorthird.classify.algorithms.svm;

import java.io.Serializable;

import javax.swing.JComponent;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.FeatureFactory;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * SVMClassifier wrapps the prediction code from the libsvm library for binary or multi-class problems.
 * A SVMClassifier must be built from a model, using the svm_model class from libsvm.  
 * This is best done by running the learner. <br>
 * <br>
 * Note that due to the way libsvm computes probabilities you may get different
 * predictions for the same instance if you turn on probabilities compared to
 * when you leave it turned off.  See the libsvm home page for more details.
 *
 * @author qcm, Frank Lin
 */

public class SVMClassifier implements Classifier,Serializable,Visible{

//	private static Logger log=Logger.getLogger(SVMClassifier.class);
	
	static final long serialVersionUID=20071130L;

	private svm_model model;
	private ExampleSchema schema;
	private FeatureFactory featureFactory;

	//this is a hack for displaying explanations
	private VisibleSVM vSVM=null;

	public SVMClassifier(svm_model model,ExampleSchema schema,FeatureFactory featureFactory){
		this.model=model;
		this.schema=schema;
		this.featureFactory=featureFactory;
	}

	@Override
	public String explain(Instance instance){		
		if(vSVM==null){
			vSVM=new VisibleSVM(model,featureFactory);
		}
		int numHyperplanes=schema.getNumberOfClasses()-1;
		StringBuilder b=new StringBuilder();
		for(int i=0;i<numHyperplanes;i++){
			b.append("Hyperplane "+i+":");
			b.append(vSVM.getHyperplane(i).explain(instance));
		}
		return b.toString();
	}

	@Override
	public Explanation getExplanation(Instance instance){
		if(vSVM==null){
			vSVM=new VisibleSVM(model,featureFactory);
		}
		int numHyperplanes=schema.getNumberOfClasses()-1;
		Explanation.Node top=new Explanation.Node("Hyperplanes");
		for(int i=0;i<numHyperplanes;i++){
			Explanation ex=vSVM.getHyperplane(i).getExplanation(instance);
			ex.getTopNode().setUserObject("Hyperplane "+i);
			top.add(ex.getTopNode());
		}
		return new Explanation(top);
	}

	public svm_model getSVMModel(){
		return model;
	}

	public ExampleSchema getSchema(){
		return schema;
	}

	public FeatureFactory getFeatureFactory(){
		return featureFactory;
	}

	@Override
	public ClassLabel classification(Instance instance){
		
		// make sure to compress the instance first, otherwise things go to crap
		instance=featureFactory.compress(instance);

		// convert compressed instance to node array
		svm_node[] nodeArray=SVMUtils.instanceToNodeArray(instance);

		double prediction;
		ClassLabel label=new ClassLabel();

		if(svm.svm_check_probability_model(model)>0){
			/* If the model is set to calcualte probabilities then create an array
			 * to store them and call the appropriate prediction method.
			 */
			if(schema.equals((ExampleSchema.BINARY_EXAMPLE_SCHEMA))){
				/* For the binary case, definitely more complicated than it needs to be;
				 * create an array of doubles of length 2 (because this is a binary classifier)
				 * and use the predict_probability method which returns that class and fills in 
				 * the probability array passed in.
				 */
				double[] probs=new double[2];
				prediction=svm.svm_predict_probability(model,nodeArray,probs);
				/* We want to return the probability estimates embedded in the prediction.  The actual
				 * value will go into the ClassLabel as the labels weight and since this is a binary 
				 * classifier the probability estimate of the other class is 1 - |prediction|.
				 * Also, the svm_predict_* methods return 1 or -1 for the binary case, but we need the 
				 * probability of the prediction (given in the prob[]), then we need to convert this
				 * probability into logits (logit = p/1-p).  Finally we need to multiply by the
				 * prediction (1 or -1) to embedd the predicted class into the weight.
				 */
				if(probs[0]>probs[1]){
					prediction=prediction*(Math.log(probs[0]/(1-probs[0])));
				}
				else{
					prediction=prediction*(Math.log(probs[1]/(1-probs[1])));
				}
				/* Score results in label
				 */
				if(prediction>=0){
					label=ClassLabel.positiveLabel(prediction);
				}
				else{
					label=ClassLabel.negativeLabel(prediction);
				}
			}
			else{				
				// For the multi-class case
				double[] probs=new double[svm.svm_get_nr_class(model)];
				svm.svm_predict_probability(model,nodeArray,probs);
				// get the labels
				int[] labels=new int[svm.svm_get_nr_class(model)];
				svm.svm_get_labels(model,labels);
				// update ClassLabel object with labels and probabilities.
				for(int i=0;i<labels.length;i++){
					// wanted to use log-odds as specified in ClassLabel, but test code doesn't like it - frank
					// double logOdds=Math.log(probs[i]/(1.0-probs[i]));
					double logOdds=probs[i];
					label.add(schema.getClassName(labels[i]),logOdds);
				}
			}
		}
		else{
			/* Otherwise just call the predict method, which simply returns the class.
			 * This method is faster than predict_probability.
			 */
			double[] predValues=new double[schema.getNumberOfClasses()];
			prediction=svm.svm_predict_values(model,nodeArray,predValues);
//			log.info("===");
//			log.info("svm labels: "+Arrays.toString(model.label));
//			log.info("svm pred val: "+Arrays.toString(predValues));
			if(schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA)){
				double diff=Math.max(predValues[0],predValues[1])-Math.min(predValues[0],predValues[1]);
				if(prediction<0){
					label.add(ExampleSchema.NEG_CLASS_NAME,diff);
				}
				else{
					label.add(ExampleSchema.POS_CLASS_NAME,diff);
				}
//				log.info("svm pred dist: "+label.bestWeight());
			}
			else{
				label.add(schema.getClassName((int)prediction),predValues[(int)prediction]);
			}
			
//			log.info(label);
			
		}

		return label;
	}

	/**
	 * GUI stuff
	 */

	@Override
	public Viewer toGUI(){
		SVMViewer svmViewer=new SVMViewer();
		svmViewer.setContent(this);
		return svmViewer;
	}

	private static class SVMViewer extends ComponentViewer{

		static final long serialVersionUID=20071130L;

		@Override
		public boolean canReceive(Object o){
			return o instanceof SVMClassifier;
		}

		@Override
		public JComponent componentFor(Object o){
			final SVMClassifier svmClassifier=(SVMClassifier)o;
			// transform to visible SVM
			VisibleSVM vsSVMtemp=new VisibleSVM(svmClassifier.getSVMModel(),svmClassifier.getFeatureFactory());
			return vsSVMtemp.toGUI();
		}

	}

}
