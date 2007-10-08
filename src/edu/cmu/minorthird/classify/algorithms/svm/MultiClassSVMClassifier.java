package edu.cmu.minorthird.classify.algorithms.svm;

import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.FeatureIdFactory;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.ExampleSchema;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import edu.cmu.minorthird.util.gui.Visible;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import java.io.*;
import javax.swing.JComponent;

/**
 * MultiClassSVMClassifier wrapps the prediction code from the libsvm library for 
 * multi-class models.  It implements the Classifier interface so that using 
 * it should be identical to any other Classifer.  A MultiClassSVMClassifier 
 * must be built from a model, using the svm_model class from libsvm.  This 
 * is best done by using {@link edu.cmu.minorthird.classify.algorithms.svm.MultiClassSVMLearner}.<br>
 * <br>
 * Note that due to the way libsvm computes probabilities you may get different 
 * predictions for the same instance if you turn on probabilities compared to 
 * when you leave it turned off.  See the libsvm home page for more details.
 *
 * @author qcm
 */
public class MultiClassSVMClassifier
  implements Classifier, 
             Serializable, 
             Visible
{   
    private FeatureIdFactory    m_featureIdFactory;
    
    private ExampleSchema       m_schema;
    
    private svm_model           m_svmModel;

    public MultiClassSVMClassifier(svm_model         model, 
				   FeatureIdFactory  idFactory, 
				   ExampleSchema     schema)
    {
        this.m_svmModel = model;
        
        this.m_featureIdFactory = idFactory;
        
        this.m_schema = schema;
    }
    
    public ClassLabel classification(Instance instance) {
    	
        //need the nodeArray
        svm_node[] nodeArray = SVMUtils.instanceToNodeArray(instance, m_featureIdFactory);
        double prediction;
        ClassLabel label = new ClassLabel();

        // If the model is set to calcualte probabilities then create an array 
        //   to store them and call the appropriate prediction method.
        if (svm.svm_check_probability_model(m_svmModel) == 1) {
            // Run the prediction saving the porbabilities of each class to probs[]
            double[] probs = new double[svm.svm_get_nr_class(m_svmModel)];
            prediction = svm.svm_predict_probability(m_svmModel, nodeArray, probs);

            // Get the list of labels for this model (they are all integers)
            int[] labels = new int[svm.svm_get_nr_class(m_svmModel)];
            svm.svm_get_labels(m_svmModel, labels);

            // Add the probability for each class to the ClassLabel instance
            for (int i=0;i<labels.length;i++) {
                label.add(m_schema.getClassName(labels[i]), probs[i]);
            }
        }
        // Otherwise just call the predict method, which simply returns the class.  This
        //   method is faster than predict_probability.
        else {
            // Calculate the prediction
            prediction = svm.svm_predict(m_svmModel, nodeArray);
            label.add(m_schema.getClassName((int)prediction), 1.0);
        }

        return label;
    }

    public String explain(Instance instance) {
        return "I have no idea how I came up with this answer";
    }

    public Explanation getExplanation(Instance instance) {
        Explanation ex = new Explanation(explain(instance));
        return ex;
    }

    public FeatureIdFactory getIdFactory() {
    	
    	return m_featureIdFactory;
    }
    
    public ExampleSchema getExampleSchema() {
    	
    	return m_schema;
    }
    
    public svm_model getSVMModel() {
    	
    	return m_svmModel;
    }
    
    /************************************************************************
     * GUI
     *************************************************************************/
    public Viewer toGUI() {
		
	MultiClassSVMViewer svmViewer = new MultiClassSVMViewer();
		
	svmViewer.setContent(this);
		
	return svmViewer;
		
    }
	
    static private class MultiClassSVMViewer 
	extends ComponentViewer {
	
	public boolean canReceive(Object o) {
			
	    return o instanceof SVMClassifier;
			
	}
	
	public JComponent componentFor(Object o) {
	
	    final MultiClassSVMClassifier mcSvmClassifier1 = (MultiClassSVMClassifier) o;
			
	    //transform to visible SVM
	    VisibleSVM vsSVMtemp = new VisibleSVM( mcSvmClassifier1.getSVMModel(), 
						   mcSvmClassifier1.getIdFactory(),
						   mcSvmClassifier1.getExampleSchema());
			
	    return vsSVMtemp.toGUI();
	
	}
    }
}
