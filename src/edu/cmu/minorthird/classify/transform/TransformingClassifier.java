package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.gui.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;

public class TransformingClassifier implements Classifier,Visible,Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

	private Classifier classifier;
	private InstanceTransform transformer;

	public TransformingClassifier(Classifier classifier,InstanceTransform transformer)
	{
		this.classifier = classifier;
		this.transformer = transformer;
	}

	public ClassLabel classification(Instance instance) {
		return classifier.classification( transformer.transform(instance) );
	}

	public String explain(Instance instance) 
	{
		Instance transformedInstance = transformer.transform(instance);
		return
			"Transformed instance: "+transformedInstance+"\n"+
			classifier.explain(transformedInstance)+"\n";
	}

	public Viewer toGUI()
	{
		Viewer gui =  new ComponentViewer() {
				public JComponent componentFor(Object o) {
					TransformingClassifier tc = (TransformingClassifier)o;
					JPanel panel = new JPanel(); 
					panel.setBorder(new TitledBorder("TransformingClassifier"));
					panel.add(new JLabel(tc.transformer.toString()));
					SmartVanillaViewer subview = new SmartVanillaViewer(tc.classifier);
					subview.setSuperView(this);
					panel.add(subview);
					return panel;
				}
			};
		gui.setContent( this );
		return gui;
	}
}
