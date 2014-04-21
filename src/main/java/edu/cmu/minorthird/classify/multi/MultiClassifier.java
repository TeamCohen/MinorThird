package edu.cmu.minorthird.classify.multi;

import java.awt.Component;
import java.io.Serializable;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Interface for a multi label classifier.
 *
 * @author Cameron Williams
 */

public class MultiClassifier implements Classifier,Visible,Serializable{
	
	static final long serialVersionUID=20080130L;

	public Classifier[] classifiers;

	public MultiClassifier(Classifier[] classifiers){
		this.classifiers=classifiers;
	}

	/** Returqn a predicted type for each element of the sequence. */
	public MultiClassLabel multiLabelClassification(Instance instance){
		ClassLabel[] labels=new ClassLabel[classifiers.length];
		for(int i=0;i<classifiers.length;i++){
			labels[i]=classifiers[i].classification(instance);
		}
		MultiClassLabel multiLabel=new MultiClassLabel(labels);
		return multiLabel;
	}

	public int getNumDim(){
		return classifiers.length;
	}

	/** Give you the class label for the first dimension */
	@Override
	public ClassLabel classification(Instance instance){
		ClassLabel classLabel=classifiers[0].classification(instance);
		return classLabel;
	}

	@Override
	public String explain(Instance instance){
		StringBuffer buf=new StringBuffer("");
		for(int i=0;i<classifiers.length;i++){
			buf.append(classifiers[i].explain(instance));
			buf.append("\n");
		}
		buf.append("classification = "+classification(instance).toString());
		return buf.toString();
	}

	@Override
	public Explanation getExplanation(Instance instance){
		Explanation.Node top=new Explanation.Node("MultiClassifier Explanation");

		for(int i=0;i<classifiers.length;i++){
			Explanation.Node classEx=
					classifiers[i].getExplanation(instance).getTopNode();
			top.add(classEx);
		}
		Explanation.Node score=
				new Explanation.Node("classification = "+
						classification(instance).toString());
		top.add(score);
		Explanation ex=new Explanation(top);
		return ex;
	}

	public Classifier[] getClassifiers(){
		return classifiers;
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("[MultiClassifier:\n");
		for(int i=0;i<classifiers.length;i++){
			buf.append(classifiers[i]+"\n");
		}
		buf.append("end MultiClassifier]\n");
		return buf.toString();
	}

	@Override
	public Viewer toGUI(){
		final Viewer v=new ComponentViewer(){
			
			static final long serialVersionUID=20080130L;

			@Override
			public JComponent componentFor(Object o){
				MultiClassifier c=(MultiClassifier)o;
				JPanel panel=new JPanel();
				panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
				for(int i=0;i<c.classifiers.length;i++){
					JLabel label=new JLabel("Dimension: "+i);
					label.setAlignmentX(Component.CENTER_ALIGNMENT);
					panel.add(label);
					Viewer subView=new SmartVanillaViewer();
					subView.setContent(c.classifiers[i]);
					subView.setSuperView(this);
					panel.add(subView);
				}
				return new JScrollPane(panel);
			}
		};
		v.setContent(this);
		return v;
	}
}
