/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.gui.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.Serializable;

/** 
 * A conditional markov model classifier.
 * 
 * @author William Cohen
*/

public class CMM implements SequenceClassifier,SequenceConstants,Visible,Serializable
{
	private static final long serialVersionUID = 1;

	static private Logger log = Logger.getLogger(CMM.class);
	static private final boolean DEBUG = false;


	private BeamSearcher searcher;
	private int historySize; 
	private String[] possibleClassLabels;
	private Classifier classifier;
	private int beamSize = 10;

	public CMM(Classifier classifier,int historySize,ExampleSchema schema)
	{
		this.searcher = new BeamSearcher(classifier,historySize,schema);
		this.classifier = classifier;
		this.historySize = historySize;
		this.possibleClassLabels = schema.validClassNames();
	}		
	public Classifier getClassifier()
	{
		return classifier;
	}

	public ClassLabel[] classification(Instance[] sequence)
	{
		return searcher.bestLabelSequence(sequence);
	}

	public String explain(Instance[] sequence)
	{
		return searcher.explain(sequence);
	}

	public Viewer toGUI()
	{
		Viewer v = new ComponentViewer() {
				public JComponent componentFor(Object o) {
					CMM cmm = (CMM)o;
					JPanel mainPanel = new JPanel();
					mainPanel.setLayout(new BorderLayout());
					mainPanel.add(
						new JLabel("CMM: historySize="+cmm.historySize+" beamSize="+beamSize),
						BorderLayout.NORTH);
					Viewer subView = new SmartVanillaViewer(cmm.classifier);
					subView.setSuperView(this);
					mainPanel.add(subView,BorderLayout.SOUTH);
					mainPanel.setBorder(new TitledBorder("Conditional Markov Model"));
					return new JScrollPane(mainPanel);
				}
			};
		v.setContent(this);
		return v;
	}
}
