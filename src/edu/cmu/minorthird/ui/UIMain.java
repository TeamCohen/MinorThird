package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/** Main UI program.  With enough support to make it configurable
 * interactively, by command lines, or by a file.
 */

public abstract class UIMain implements CommandLineProcessor.Configurable
{
	private static final Class[] SELECTABLE_TYPES = new Class[]
	{
		//
		// bunches of parameters
		//
		CommandLineUtil.BaseParams.class, CommandLineUtil.SaveParams.class, 
		CommandLineUtil.ClassificationSignalParams.class, CommandLineUtil.TrainClassifierParams.class, 
		CommandLineUtil.TestClassifierParams.class, CommandLineUtil.TestExtractorParams.class, 
		CommandLineUtil.LoadAnnotatorParams.class, CommandLineUtil.SplitterParams.class,
		CommandLineUtil.ExtractionSignalParams.class, CommandLineUtil.TrainExtractorParams.class,
		CommandLineUtil.TestClassifierParams.class, 
		//
		// main routines
		//
		ApplyAnnotator.class, TestExtractor.class, TrainClassifier.class, 
		TrainExtractor.class,	TrainTestClassifier.class, 
		TrainTestExtractor.class,
		//
		// recommended classification learners
		//
		Recommended.KnnLearner.class, Recommended.NaiveBayes.class,
		Recommended.VotedPerceptronLearner.class,	Recommended.SVMLearner.class,
		Recommended.DecisionTreeLearner.class, Recommended.BoostedDecisionTreeLearner.class,
		Recommended.BoostedStumpLearner.class, 
		//
		// recommended annotator learners
		//
		Recommended.VPHMMLearner.class, Recommended.VPCMMLearner.class, 
		Recommended.MEMMLearner.class, Recommended.SVMLearner.class, 
		//
		// recommend feature extractors
		//
		Recommended.DocumentFE.class, Recommended.TokenFE.class, Recommended.MultitokenSpanFE.class,
		//
		// splitters
		//
		CrossValSplitter.class, RandomSplitter.class, StratifiedCrossValSplitter.class,
	};

	protected boolean useGUI=false;
	protected class GUIParams extends BasicCommandLineProcessor {
		public void gui() { useGUI=true; }
		public void usage() {
			System.out.println("presentation parameters:");
			System.out.println(" -gui                     use graphic interface to set parameters");
			System.out.println();
		}
	}

	/** Do the main action, after setting all parameters. */
	abstract public void doMain();

	/** Return the result of the action. */
	abstract public Object getMainResult(); 

	/** Implements CommandLineProcessor.Configurable. */
	abstract public CommandLineProcessor getCLP();
			
	/** Helper to handle command-line processing. */
	protected void callMain(final String[] args) 
	{
		try {
			getCLP().processArguments(args);
			if (!useGUI) doMain();
			else {
				final Viewer v = new ComponentViewer() {
						public JComponent componentFor(Object o) 
						{
							Viewer ts = new TypeSelector(SELECTABLE_TYPES, UIMain.class);
							ts.setContent(o);								

							// we'll put the type selector in a nice panel
							JPanel panel = new JPanel();
							panel.setBorder(new TitledBorder(StringUtil.toString(args,"Command line: ",""," ")));

							panel.setLayout(new GridBagLayout());
							GridBagConstraints gbc;

							JPanel subpanel1 = new JPanel();
							subpanel1.setBorder(new TitledBorder("Parameter modification"));
							//subpanel1.add(new JLabel("Use the edit button to change the parameters given in the command line"));
							subpanel1.add( ts );
							gbc = Viewer.fillerGBC(); gbc.weighty=0; 
							panel.add( subpanel1, gbc  ); 

							JPanel subpanel2 = new JPanel();
							// a button to show the results
							final JButton viewButton = new JButton(new AbstractAction("View the results") {
									public void actionPerformed(ActionEvent event) {
										Viewer rv = new SmartVanillaViewer();
										rv.setContent( getMainResult() );
										ViewerFrame f = new ViewerFrame("Result", rv);
									}
								});
							viewButton.setEnabled(false);
							
							// now a button to start this thread
							JButton goButton = new JButton(new AbstractAction("Start the task") {
									public void actionPerformed(ActionEvent event) {
										Thread thread = new Thread() { 
												public void run() { 
													viewButton.setEnabled(false);
													doMain(); 
													viewButton.setEnabled(true);
												}
											};
										thread.start();
									}
								});

							subpanel2.add( goButton );
							subpanel2.add( viewButton );
							subpanel2.setBorder(new TitledBorder("Execution controls"));
							gbc = Viewer.fillerGBC();	gbc.weighty=0; gbc.gridy=1;
							panel.add(subpanel2, gbc );

							// now some progress bars
							JProgressBar progressBar1 = new JProgressBar();
							JProgressBar progressBar2 = new JProgressBar();
							JProgressBar progressBar3 = new JProgressBar();
							ProgressCounter.setGraphicContext(new JProgressBar[]{progressBar1, progressBar2,progressBar3});
							gbc = Viewer.fillerGBC();	gbc.weighty=0; gbc.gridy=2;
							panel.add(progressBar1, gbc);
							gbc = Viewer.fillerGBC(); gbc.weighty=0; gbc.gridy=3;
							panel.add(progressBar2, gbc);
							gbc = Viewer.fillerGBC(); gbc.weighty=0; gbc.gridy=4;
							panel.add(progressBar3, gbc);
							return panel;
						}
					};
				v.setContent(this);
				ViewerFrame f = new ViewerFrame("CommandLineUI Parameters",v);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Use option -help for help");
		}
	}
}

