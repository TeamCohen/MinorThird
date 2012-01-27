package edu.cmu.minorthird.classify.transform;

import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

public class TransformingClassifier implements Classifier,Visible,Serializable{

	static final long serialVersionUID=20080201L;

	private Classifier classifier;

	private InstanceTransform transformer;

	public TransformingClassifier(Classifier classifier,
			InstanceTransform transformer){
		this.classifier=classifier;
		this.transformer=transformer;
	}

	@Override
	public ClassLabel classification(Instance instance){
		return classifier.classification(transformer.transform(instance));
	}

	@Override
	public String explain(Instance instance){
		Instance transformedInstance=transformer.transform(instance);
		return "Transformed instance: "+transformedInstance+"\n"+
				classifier.explain(transformedInstance)+"\n";
	}

	@Override
	public Explanation getExplanation(Instance instance){
		Explanation.Node top=
				new Explanation.Node("TransformingClassifier Explanation");
		Explanation.Node transformedEx=
				classifier.getExplanation(transformer.transform(instance)).getTopNode();
		top.add(transformedEx);

		Explanation ex=new Explanation(top);
		return ex;
	}

	@Override
	public Viewer toGUI(){
		Viewer gui=new ComponentViewer(){
			static final long serialVersionUID=20080201L;
			@Override
			public JComponent componentFor(Object o){
				TransformingClassifier tc=(TransformingClassifier)o;
				JPanel panel=new JPanel();
				panel.setBorder(new TitledBorder("TransformingClassifier"));
				panel.add(new JLabel(tc.transformer.toString()));
				SmartVanillaViewer subview=new SmartVanillaViewer(tc.classifier);
				subview.setSuperView(this);
				panel.add(subview);
				return panel;
			}
		};
		gui.setContent(this);
		return gui;
	}
}
