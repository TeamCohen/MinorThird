/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;


/** A Classifier composed of a bunch of binary classifiers, each of
 * which separates one class from the others.
 *
 * @author William Cohen
 */

public class OneVsAllClassifier implements Classifier,Visible,Serializable{
	
	//private static Logger log=Logger.getLogger(OneVsAllClassifier.class);
	
	static private final long serialVersionUID = 1;

	private String[] classNames;
	private Classifier[] binaryClassifiers;

	/** Create a OneVsAllClassifier.
	 */
	public OneVsAllClassifier(String[] classNames,Classifier[] binaryClassifiers) {
		if (classNames.length!=binaryClassifiers.length) {
			throw new IllegalArgumentException("arrays must be parallel");
		}
		this.classNames = classNames;
		this.binaryClassifiers = binaryClassifiers;
	}

	public Classifier[] getBinaryClassifiers() { return binaryClassifiers; }

	@Override
	public ClassLabel classification(Instance instance){
		
		ClassLabel classLabel = new ClassLabel();
		for (int i=0; i<classNames.length; i++){
			classLabel.add(classNames[i],binaryClassifiers[i].classification(instance).posWeight());
		}
		return classLabel;
		
	}

	@Override
	public String explain(Instance instance) 
	{
		StringBuffer buf = new StringBuffer("");
		for (int i=0; i<binaryClassifiers.length; i++) {
			buf.append("score for "+classNames[i]+": ");
			buf.append( binaryClassifiers[i].explain(instance) );
			buf.append( "\n" );
		}
		buf.append( "classification = "+classification(instance).toString() );
		return buf.toString();
	}

	@Override
	public Explanation getExplanation(Instance instance) {
		Explanation.Node top = new Explanation.Node("OneVsAll Explanation");
		for (int i=0; i<binaryClassifiers.length; i++) {
			Explanation.Node binClassifierNode = new Explanation.Node(classNames[i] + " Tree");
			Explanation.Node explanation = binaryClassifiers[i].getExplanation(instance).getTopNode();
			binClassifierNode.add(explanation);
			top.add(binClassifierNode);
		}
		Explanation ex = new Explanation(top);
		return ex;
	}

	public String[] getClassNames() { return classNames; }

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer("[OneVsAllClassifier:\n");
		for (int i=0; i<classNames.length; i++) {
			buf.append(classNames[i]+": "+binaryClassifiers[i]+"\n");
		}
		buf.append("end OneVsAllClassifier]\n");
		return buf.toString();
	}

	@Override
	public Viewer toGUI()
	{
		final Viewer v = new ComponentViewer() {
			static final long serialVersionUID=20071015;
			@Override
			public JComponent componentFor(Object o) {
				OneVsAllClassifier c = (OneVsAllClassifier)o;
				JPanel panel = new JPanel();
				for (int i=0; i<c.classNames.length; i++) {
					panel.add(new JLabel(c.classNames[i]));
					Viewer subView = new SmartVanillaViewer();
					subView.setContent( c.binaryClassifiers[i] );
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

