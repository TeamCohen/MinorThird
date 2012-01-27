package edu.cmu.minorthird.classify.transform;

import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.multi.MultiClassLabel;
import edu.cmu.minorthird.classify.multi.MultiClassifier;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * @author Cameron Williams
 * Date: October 11, 2005
 * Transforms each instance with and InstanceTransform and then uses a multiClassifier
 * to classify the transformed instance
 */

public class TransformingMultiClassifier extends MultiClassifier implements
		Visible,Serializable{

	static final long serialVersionUID=20080201L;

	private MultiClassifier multiClassifier;

	private AbstractInstanceTransform transformer;

	public TransformingMultiClassifier(MultiClassifier multiClassifier,
			AbstractInstanceTransform transformer){
		super(multiClassifier.getClassifiers());
		this.multiClassifier=multiClassifier;
		this.transformer=transformer;
	}

	public AbstractInstanceTransform getTransform(){
		return transformer;
	}

	@Override
	public Classifier[] getClassifiers(){
		TransformingClassifier[] tc=
				new TransformingClassifier[multiClassifier.getNumDim()];
		Classifier[] classifiers=multiClassifier.getClassifiers();
		for(int i=0;i<tc.length;i++){
			tc[i]=new TransformingClassifier(classifiers[i],transformer);
		}
		return tc;
	}

	@Override
	public MultiClassLabel multiLabelClassification(Instance instance){
		return super.multiLabelClassification(transformer.transform(instance));
	}

	@Override
	public ClassLabel classification(Instance instance){
		return super.classification(transformer.transform(instance));
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
		Classifier[] classifiers=getClassifiers();

		for(int i=0;i<classifiers.length;i++){
			Explanation.Node classEx=
					classifiers[i].getExplanation(instance).getTopNode();
			top.add(classEx);
		}
		Explanation.Node score=
				new Explanation.Node("classification = "+
						classification(transformer.transform(instance)).toString());
		top.add(score);
		Explanation ex=new Explanation(top);
		return ex;
	}

	@Override
	public Viewer toGUI(){
		Viewer gui=new ComponentViewer(){
			static final long serialVersionUID=20080201L;
			@Override
			public JComponent componentFor(Object o){
				TransformingMultiClassifier tc=(TransformingMultiClassifier)o;
				JPanel panel=new JPanel();
				panel.setBorder(new TitledBorder("TransformingMultiClassifier"));
				//panel.add(new JLabel(tc.transformer.toString()));
				SmartVanillaViewer subview=new SmartVanillaViewer(tc.multiClassifier);
				subview.setSuperView(this);
				panel.add(subview);
				return(new JScrollPane(panel));
			}
		};
		gui.setContent(this);
		return gui;
	}
}