package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.knn.KnnLearner;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.SampleFE;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.*;
import jwf.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Top-level GUI interface for the text annotation/learning stuff.
 *
 * @author William Cohen
 */

public class WizardUI
{
	private static JFileChooser myFileChooser;
	static {
		myFileChooser = new JFileChooser();
		myFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
	};
		

	/** A special type selector.
	 */
	private static class MyTypeSelector extends TypeSelector
	{
		private static Class[] myClasses = {
			// loaders
			TextBaseLoader.class,
			// learners
			NaiveBayes.class,
			VotedPerceptron.class,
			PoissonLearner.class,
			LogisticRegressor.class,
			AdaBoost.class,
			DecisionTreeLearner.class,
			SVMLearner.class,
			KnnLearner.class,
			BatchVersion.class,
			StackedLearner.class,
			// splitters
			RandomSplitter.class,
			CrossValSplitter.class,
			// feature extractors
			SampleFE.BagOfWordsFE.class,
			SampleFE.BagOfLowerCaseWordsFE.class,
		};
		public MyTypeSelector(Class rootClass) { super(myClasses,rootClass); }
	}


	//
	// define the steps of the wizard
	//

	/** pick the task to perform. */
	private static class PickTask extends RadioWizard
	{
		public PickTask(Map viewerContext)
		{
			super("task",viewerContext,"Select Task","Please select a task:");
			addButton("Perform a text categorization experiment",
								new PickLoader(viewerContext).getWizardPanel(),true);
//			addButton("Perform an extraction experiment",
//								new PickLoader(viewerContext).getWizardPanel(),true);
		}
	}

	//
	// doing a text cat experiment
	//


	/** pick a TextBaseLoader. */
	private static class PickLoader extends SimpleViewerWizard
	{
		public PickLoader(Map viewerContext)
		{
			super("textBaseLoader",viewerContext,
						"Configure TextBaseLoader","Define the data format being used:",
						new PickTrainDataFile(viewerContext));
			MyTypeSelector selector = new MyTypeSelector(TextBaseLoader.class);
			selector.setContent(new TextBaseLoader());
			addViewer(selector);
		}
	}

	/** Pick a training-data file. */
	private static class PickTrainDataFile extends FileChooserWizard
	{
		public PickTrainDataFile(Map viewerContext)
		{
			super("trainDataFile",viewerContext,
						true,"Select Training Data","Where is the training data?",
						myFileChooser);
		}
		public WizardPanel next() { return new PickLabelFile(viewerContext); }
	}

	/** Pick a label file. */
	private static class PickLabelFile extends FileChooserWizard
	{
		public PickLabelFile(Map viewerContext)
		{
			super("labelsFile",viewerContext,
						true,"Select Annotation File","Where are annotations stored?",
						myFileChooser);
		}
		public WizardPanel next() { return new PickLearner(viewerContext).getWizardPanel(); }
	}

	/** pick a learning algorithm. */
	private static class PickLearner extends SimpleViewerWizard
	{
		public PickLearner(Map viewerContext)
		{
			super("learner",viewerContext,
						"Configure Learner","Choose a classification learning algorithm:",
						new PickFeatureExtractor(viewerContext).getWizardPanel());
			MyTypeSelector selector = new MyTypeSelector(ClassifierLearner.class);
			selector.setContent(new AdaBoost());
			addViewer(selector);
		}
	}

	/** pick a feature extractor. */
	private static class PickFeatureExtractor extends SimpleViewerWizard
	{
		public PickFeatureExtractor(Map viewerContext)
		{
			super("fe",viewerContext,
						"Configure Feature Extractor","Choose a featore extraction scheme:",
						new PickTestMethod(viewerContext));
			MyTypeSelector selector = new MyTypeSelector(SpanFeatureExtractor.class);
			selector.setContent(new SampleFE.BagOfLowerCaseWordsFE());
			addViewer(selector);
		}
	}

	/** pick a test scheme algorithm. */
	private static class PickTestMethod extends RadioWizard
	{
		public PickTestMethod(Map viewerContext)
		{
			super("testMethod",viewerContext,"Test method","Choose a testing scheme:");
			addButton( "Cross validation or random split", new PickSplitter(viewerContext).getWizardPanel(), true );
			// adding this button causes a stack overflow, why?
			addButton( "Fixed test set", new PickTestDataFile(viewerContext), false );
		}
	}

	/** Pick a training-data file. */
	private static class PickTestDataFile extends FileChooserWizard
	{
		public PickTestDataFile(Map viewerContext)
		{
			super("testDataFile",viewerContext,
						true,"Select Testing Data","Where is the test data?",
						myFileChooser);
		}
		public WizardPanel next() { return new PickTargetClass(viewerContext); }
	}


	/** pick a cross-validation scheme. */
	private static class PickSplitter extends SimpleViewerWizard
	{
		public PickSplitter(Map viewerContext)
		{
			super("splitter",viewerContext,
						"Cross Validation","Choose a cross-validation scheme:",
						new PickTargetClass(viewerContext));
			MyTypeSelector selector = new MyTypeSelector(Splitter.class);
			selector.setContent(new CrossValSplitter(5));
			addViewer(selector);
		}
	}
	/** pick a cross-validation scheme. */
	private static class PickTargetClass extends NullWizardPanel
	{
		private Map viewerContext;
		private JTextField textField;
		public PickTargetClass(Map viewerContext)
		{
			this.viewerContext = viewerContext;
			setBorder(new TitledBorder("Target Class"));
			add(new JLabel("What class do you want to try to learn?"));
			textField = new JTextField(10);
			add(textField);
		}
		public boolean validateNext(java.util.List list) {
			list.add("You need to pick a target class.");
			return textField.getText().trim().length() > 0;
		}
		public boolean hasNext() { return true; }
		public WizardPanel next()
		{
			viewerContext.put("targetClass",textField.getText().trim());
			return new RunExperiment(viewerContext);
		}
	}

	private static class RunExperiment extends NullWizardPanel
	{
		private Map viewerContext;
		private Evaluation evaluation = null;
		private JButton resultButton;
		private JRadioButton someDetail,moreDetail,mostDetail;

		public RunExperiment(Map viewerContext)
		{ 
			this.viewerContext=viewerContext; 
			setBorder(new TitledBorder("Run Experiment"));

			JPanel detailPanel = new JPanel();
			detailPanel.setBorder(new TitledBorder("Result Format"));
			ButtonGroup group = new ButtonGroup();
			someDetail = new JRadioButton("Some detail",true);
			moreDetail = new JRadioButton("More detail",false);
			mostDetail = new JRadioButton("Most detail",false);
			JRadioButton[] buttons = new JRadioButton[]{someDetail,moreDetail,mostDetail};
			for (int i=0; i<buttons.length; i++) {
				detailPanel.add(buttons[i]);
				group.add(buttons[i]);
			}

			JPanel runPanel = new JPanel();
			runPanel.setBorder(new TitledBorder("Experiment Progress"));
			JProgressBar progressBar1 = new JProgressBar();
			JProgressBar progressBar2 = new JProgressBar();
			runPanel.add(new JButton(new AbstractAction("Start Experiment") {
					public void actionPerformed(ActionEvent ev) {
						new MyThread().start();
					}
				}));
			ProgressCounter.setGraphicContext(new JProgressBar[]{progressBar1,progressBar2});
			runPanel.add(progressBar1);
			runPanel.add(progressBar2);
			resultButton = new JButton(new AbstractAction("View Results") {
					public void actionPerformed(ActionEvent event) {
						if (evaluation!=null) {
							ViewerFrame f = new ViewerFrame("Experimental Results",evaluation.toGUI());
						}
					}
				});
			resultButton.setEnabled(false);
			runPanel.add( resultButton );
			add(runPanel);

			add(detailPanel);

		}
		private class MyThread extends Thread
		{
			public void run() {
				try {
					System.out.println(viewerContext.toString());
					TextBaseLoader loader = (TextBaseLoader)viewerContext.get("textBaseLoader");
					File trainDataFile = (File)viewerContext.get("trainDataFile");
					ClassifierLearner learner = (ClassifierLearner)viewerContext.get("learner");
					String testMethod = (String)viewerContext.get("testMethod");
					Splitter splitter = (Splitter)viewerContext.get("splitter");
					TextBase base = new BasicTextBase();
					SpanFeatureExtractor fe = (SpanFeatureExtractor)viewerContext.get("fe");
					String targetClass = (String)viewerContext.get("targetClass");
					File labelsFile = (File)viewerContext.get("labelsFile");
					loader.loadFile(base,trainDataFile);
					TextBase testBase = new BasicTextBase();
					File testDataFile = (File)viewerContext.get("testDataFile");
					if (testDataFile!=null) {
						loader.loadFile(testBase,testDataFile);
					}
					TextLabels labels = new TextLabelsLoader().loadOps(base,labelsFile);
					ProgressCounter pc = new ProgressCounter("creating train dataset","document",base.size());
					Dataset data = new BasicDataset();
					for (Span.Looper i=base.documentSpanIterator(); i.hasNext(); ) {
						Span s = i.nextSpan();
						double label = labels.hasType(s,targetClass) ? +1 : -1;
						data.add( new BinaryExample( fe.extractInstance(s), label ) );
						pc.progress();
					}
					pc.finished();
					Dataset testData = new BasicDataset();
					if (testDataFile!=null) {
						ProgressCounter pc2 = new ProgressCounter("creating test dataset","document",base.size());
						for (Span.Looper i=testBase.documentSpanIterator(); i.hasNext(); ) {
							Span s = i.nextSpan();
							double label = labels.hasType(s,targetClass) ? +1 : -1;
							testData.add( new BinaryExample( fe.extractInstance(s), label ) );
							pc.progress();
						}
						pc.finished();
						splitter = new FixedTestSetSplitter(testData.iterator());
					}
					Expt expt = new Expt(learner,data,splitter);
					if (someDetail.isSelected()) {
						final Evaluation e = Tester.evaluate(learner,data,splitter);
						resultButton.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ev) {
									ViewerFrame f = new ViewerFrame("Result",e.toGUI());
								}
							});
					} else {
						boolean saveTrainPartitions = mostDetail.isSelected();
						final CrossValidatedDataset cd = new CrossValidatedDataset(learner,data,splitter,saveTrainPartitions);
						resultButton.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ev) {
									ViewerFrame f = new ViewerFrame("Result", cd.toGUI());
								}
							});
					}
					resultButton.setEnabled(true);
				} catch (IOException e) {
					;
				}
			}
		}
	}


	//
	// main program to kick off the wizard
	//

	/** The entry point to the wizard. */
	public static void main(String args[])
	{
		Map viewerContext = new HashMap();
		edu.cmu.minorthird.util.gui.WizardFrame f =
			new edu.cmu.minorthird.util.gui.WizardFrame("Text Learning Wizard",new PickTask(viewerContext));
	}
}
