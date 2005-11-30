package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.multi.*;
import edu.cmu.minorthird.util.gui.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author Cameron Williams
 * Date: October 11, 2005
 * Transforms each instance with and InstanceTransform and then uses a multiClassifier
 * to classify the transformed instance
 */

public class TransformingMultiClassifier extends MultiClassifier implements Visible,Serializable
{
    static private final long serialVersionUID = 1;
    private final int CURRENT_VERSION_NUMBER = 1;

    private MultiClassifier multiClassifier;
    private AbstractInstanceTransform transformer;

    public TransformingMultiClassifier(MultiClassifier multiClassifier,AbstractInstanceTransform transformer)
    {
	super(multiClassifier.getClassifiers());
	this.multiClassifier = multiClassifier;
	this.transformer = transformer;
    }

    public AbstractInstanceTransform getTransform() { return transformer; }

    public Classifier[] getClassifiers() {
	TransformingClassifier[] tc = new TransformingClassifier[multiClassifier.getNumDim()];
	Classifier[] classifiers = multiClassifier.getClassifiers();
	for(int i=0; i<tc.length; i++) {
	    tc[i] = new TransformingClassifier(classifiers[i], transformer);
	}
	return tc;
    }

    public MultiClassLabel multiLabelClassification(Instance instance) {
	return super.multiLabelClassification( transformer.transform(instance) );
    }

    public ClassLabel classification(Instance instance) {
	return super.classification( transformer.transform(instance) );
    }

    public Viewer toGUI()
    {
	Viewer gui =  new ComponentViewer() {
		public JComponent componentFor(Object o) {
		    TransformingMultiClassifier tc = (TransformingMultiClassifier)o;
		    JPanel panel = new JPanel(); 
		    panel.setBorder(new TitledBorder("TransformingMultiClassifier"));
		    //panel.add(new JLabel(tc.transformer.toString()));
		    SmartVanillaViewer subview = new SmartVanillaViewer(tc.multiClassifier);
		    subview.setSuperView(this);
		    panel.add(subview);
		    return (new JScrollPane(panel));
		}
	    };
	gui.setContent( this );
	return gui;
    }
}