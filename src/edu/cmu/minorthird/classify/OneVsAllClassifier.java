/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.util.gui.*;
import javax.swing.*;
import java.io.Serializable;


/** A Classifier composed of a bunch of binary classifiers, each of
 * which separates one class from the others.
 *
 * @author William Cohen
 */

public class OneVsAllClassifier implements Classifier,Visible,Serializable
{
	static private int serialVersionUID = 1;
	private final int CURRENT_SERIAL_VERSION = 1;

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

	public ClassLabel classification(Instance instance) 
	{
		ClassLabel classLabel = new ClassLabel();
		for (int i=0; i<classNames.length; i++) {
			classLabel.add(classNames[i], binaryClassifiers[i].classification(instance).posWeight());
		}
		return classLabel;
	}

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

	public String[] getClassNames() { return classNames; }

	public String toString() {
		StringBuffer buf = new StringBuffer("[OneVsAllClassifier:\n");
		for (int i=0; i<classNames.length; i++) {
			buf.append(classNames[i]+": "+binaryClassifiers[i]+"\n");
		}
		buf.append("end OneVsAllClassifier]\n");
		return buf.toString();
	}

	public Viewer toGUI()
	{
		final Viewer v = new ComponentViewer() {
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

