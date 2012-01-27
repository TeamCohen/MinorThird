/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.algorithms.knn.KnnLearner;
import edu.cmu.minorthird.classify.algorithms.linear.MaxEntLearner;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;
import edu.cmu.minorthird.classify.experiments.ClassifiedDataset;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.classify.experiments.StratifiedCrossValSplitter;
import edu.cmu.minorthird.classify.multi.InstanceFromPrediction;
import edu.cmu.minorthird.classify.multi.MultiClassLabel;
import edu.cmu.minorthird.classify.multi.MultiClassifiedDataset;
import edu.cmu.minorthird.classify.multi.MultiClassifier;
import edu.cmu.minorthird.classify.multi.MultiDataset;
import edu.cmu.minorthird.classify.multi.MultiEvaluation;
import edu.cmu.minorthird.classify.multi.MultiExample;
import edu.cmu.minorthird.classify.sequential.ClassifiedSequenceDataset;
import edu.cmu.minorthird.classify.sequential.CollinsPerceptronLearner;
import edu.cmu.minorthird.classify.sequential.GenericCollinsLearner;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.classify.transform.FrequencyBasedTransformLearner;
import edu.cmu.minorthird.classify.transform.InfoGainTransformLearner2;
import edu.cmu.minorthird.classify.transform.T1InstanceTransformLearner;
import edu.cmu.minorthird.classify.transform.TFIDFTransformLearner;
import edu.cmu.minorthird.classify.transform.TransformingBatchLearner;
import edu.cmu.minorthird.util.BasicCommandLineProcessor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Console;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TypeSelector;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Main UI program for the 'classify' package. 
 *
 * @author William Cohen
 */

public class Test
{
	private static Logger log = Logger.getLogger(UI.class);

	private static final Class<?>[] SELECTABLE_TYPES = new Class[]{
		DataClassificationTask.class,
		//ClassifyCommandLineUtil.SimpleTrainParams.class, ClassifyCommandLineUtil.MultiTrainParams.class, ClassifyCommandLineUtil.SeqTrainParams.class,
		ClassifyCommandLineUtil.SimpleTestParams.class, ClassifyCommandLineUtil.MultiTestParams.class, ClassifyCommandLineUtil.SeqTestParams.class,
		ClassifyCommandLineUtil.Learner.SequentialLearner.class, ClassifyCommandLineUtil.Learner.ClassifierLearner.class,
		KnnLearner.class, NaiveBayes.class,
		VotedPerceptron.class,	SVMLearner.class,
		DecisionTreeLearner.class, AdaBoost.class,
		BatchVersion.class, TransformingBatchLearner.class,
		MaxEntLearner.class,
		// transformations
		FrequencyBasedTransformLearner.class, InfoGainTransformLearner2.class,
		T1InstanceTransformLearner.class, TFIDFTransformLearner.class,
		// sequential learner
		CollinsPerceptronLearner.class, GenericCollinsLearner.class,
		// splitters
		CrossValSplitter.class, RandomSplitter.class, StratifiedCrossValSplitter.class,
	};

	//private static final Set<String> LEGAL_OPS = new HashSet<String>(Arrays.asList(new String[]{"train","test","trainTest"}));

	public static class DataClassificationTask implements CommandLineProcessor.Configurable,/*Saveable,*/ Console.Task
	{
		private ClassifyCommandLineUtil.TestParams testParams = new ClassifyCommandLineUtil.TestParams();

		public Object resultToShow;
		public boolean useGUI;
		public Console.Task main;

		// for gui
		public ClassifyCommandLineUtil.TestParams getTestParameters() { return testParams; }
		public void setTestParameters(ClassifyCommandLineUtil.TestParams p) { testParams=p; }
		public String getTestParamsHelp() { return "Define what type of experiment you would like to run: <br>" +
			"Simple - Standard classify experiment <br> " +
			"Multi  - Classify Experiment with Multiple labels per example <br>" +
			"Seq    - Classify experiment with a Sequential Dataset, where each example has a history, <br> " +
			"          and uses a Sequential Learner"; }

		protected class GUIParams extends BasicCommandLineProcessor {
			public void gui() { 
				useGUI=true; 
				if(ClassifyCommandLineUtil.TestParams.type != null)
					testParams = ClassifyCommandLineUtil.TestParams.type;
				else testParams = new ClassifyCommandLineUtil.SimpleTestParams();
			}
			@Override
			public void usage() {
				System.out.println("presentation parameters:");
				System.out.println(" -gui                     use graphic interface to set parameters");
				System.out.println();
			}
		}
		public String getDatasetFilename() { return testParams.testDataFilename; }

		@Override
		public CommandLineProcessor getCLP() {
			JointCommandLineProcessor jlpTest = new JointCommandLineProcessor(new CommandLineProcessor[]
			                                                                                           { new GUIParams(),testParams});
			return jlpTest;
		}


		/** Returns whether base.labels exits */
		@Override
		public boolean getLabels(){
			return (getDatasetFilename() != null);
		}

		public MultiDataset annotateData(MultiClassifier multiClassifier, MultiDataset md) {
			MultiDataset annotatedDataset = new MultiDataset();
			for(Iterator<MultiExample> i = md.multiIterator(); i.hasNext(); ) {
				MultiExample ex = i.next();		
				Instance instance = ex.asInstance();
				MultiClassLabel predicted = multiClassifier.multiLabelClassification(instance);
				Instance annotatedInstance = new InstanceFromPrediction(instance, predicted.bestClassName());
				MultiExample newEx = new MultiExample(annotatedInstance, ex.getMultiLabel(), ex.getWeight());
				annotatedDataset.addMulti(newEx);
			}
			return annotatedDataset;
		}

		// main action
		@Override
		public void doMain()
		{
			if (testParams.testData==null) {
				System.out.println("The testing data needs to be specified with the -test option.");
				return;
			}
			if ((testParams.typeString.equals("seq")) && (!(testParams.testData instanceof SequenceDataset))) {
				System.out.println("The training data should be a sequence dataset");
				return;
			}
			if (testParams.showData) new ViewerFrame("Training data",testParams.testData.toGUI());
			try {
				if (testParams.loadFrom==null) {
					System.out.println("The classifier to test needs to be specified with -classifierFile option.");
					return;
				}                    
				Object c;
				if (testParams.typeString.equals("seq")) {
					Evaluation e = new Evaluation(testParams.testData.getSchema());
					c = IOUtil.loadSerialized(testParams.loadFrom);
					e.extend((SequenceClassifier)c, (SequenceDataset)testParams.testData);
					e.summarize();
					testParams.resultToShow = testParams.resultToSave = e;
				} else if (testParams.typeString.equals("multi")) {
					MultiDataset md = (MultiDataset)testParams.testData;
					MultiEvaluation e = new MultiEvaluation(md.getMultiSchema());
					c = IOUtil.loadSerialized(testParams.loadFrom);
					if(testParams.crossDim) {
						md = annotateData((MultiClassifier)c, md);
						new ViewerFrame("Annotated data",md.toGUI());
					}
					e.extend((MultiClassifier)c, md);
					e.summarize();
					testParams.resultToShow = testParams.resultToSave = e;
				} else {
					Evaluation e = new Evaluation(testParams.testData.getSchema());
					c = IOUtil.loadSerialized(testParams.loadFrom);
					e.extend((Classifier)c, testParams.testData, 0);
					e.summarize();
					testParams.resultToShow = testParams.resultToSave = e;
				}                                        
				if (testParams.showTestDetails) {
					if (testParams instanceof ClassifyCommandLineUtil.SeqTestParams) {
						ClassifiedSequenceDataset cd =
							new ClassifiedSequenceDataset((SequenceClassifier)c, (SequenceDataset)testParams.testData);
						testParams.resultToShow = cd;
					} else if(testParams.multi >0) {
						MultiClassifiedDataset cd =
							new MultiClassifiedDataset((MultiClassifier)c, (MultiDataset)testParams.testData);
						testParams.resultToShow = cd;
					}else {
						ClassifiedDataset cd = new ClassifiedDataset((Classifier)c, testParams.testData);
						testParams.resultToShow = cd;
					}
				}
				resultToShow = testParams.resultToShow;
			} catch (IOException ex) {
				log.error("Can't load classifier from "+testParams.loadFromFilename+": "+ex);
				return;
			}          
			if (testParams.showResult) new ViewerFrame("Result", new SmartVanillaViewer(testParams.resultToShow));
			if (testParams.saveAs!=null) {
				if (IOUtil.saveSomehow(testParams.resultToSave,testParams.saveAs)) {
					log.info("Result saved in "+testParams.saveAs);
				} else {
					log.error("Can't save "+testParams.resultToSave.getClass()+" to "+testParams.saveAs);
				}
			}
		}
		@Override
		public Object getMainResult()
		{
			return resultToShow;
		}

		//
		// implements Saveable
		//
		/*public String[] getFormatNames() { return clp.getFormatNames(); }
        public String getExtensionFor(String format) { return clp.getExtensionFor(format); }
        public void saveAs(File file, String format) throws IOException { clp.saveAs(file,format); }
        public Object restore(File file) throws IOException
        {
        DataClassificationTask task = new DataClassificationTask();
        task.clp.config(file.getAbsolutePath());
        return task;
        }*/

		// gui around main action
		public void callMain(final String[] args)
		{

			try {
				getCLP().processArguments(args);
				if (!useGUI) {
					doMain();
				}
				else {
					main = this;
					final Viewer v = new ComponentViewer() {

						static final long serialVersionUID=20071015;
						@Override
						public JComponent componentFor(Object o)
						{
							Viewer ts = new TypeSelector(SELECTABLE_TYPES, "selectableTypes.txt", DataClassificationTask.class);
							ts.setContent(o);

							// we'll put the type selector in a nice panel
							JPanel panel = new JPanel();
							panel.setBorder(new TitledBorder(StringUtil.toString(args,"Command line: ",""," ")));
							panel.setLayout(new GridBagLayout());
							GridBagConstraints gbc;

							// another panel to allow parameter modifications
							JPanel subpanel1 = new JPanel();
							subpanel1.setBorder(new TitledBorder("Parameter modification"));
							//subpanel1.add(new JLabel("Use the edit button to change the parameters given in the command line"));
							subpanel1.add( ts );
							gbc = Viewer.fillerGBC(); gbc.weighty=0;
							panel.add( subpanel1, gbc  );

							// a control panel for controls
							JPanel subpanel2 = new JPanel();
							subpanel2.setBorder(new TitledBorder("Execution controls"));
							// a button to show the results
							final JButton viewButton = new JButton(new AbstractAction("View results") {
								static final long serialVersionUID=20071015;
								@Override
								public void actionPerformed(ActionEvent event) {
									Viewer rv = new SmartVanillaViewer();
									rv.setContent( getMainResult() );
									new ViewerFrame("Result", rv);
								}
							});
							viewButton.setEnabled(false);

							// another panel for error messages and other outputs
							JPanel errorPanel = new JPanel();
							errorPanel.setBorder(new TitledBorder("Error messages and output"));
							final Console console = new Console(main, getDatasetFilename() != null, viewButton);
							errorPanel.add(console.getMainComponent());

							// a button to start this thread
							JButton goButton = new JButton(new AbstractAction("Start task") {
								static final long serialVersionUID=20071015;
								@Override
								public void actionPerformed(ActionEvent event) {
									console.start();
								}
							});
							// and a button to show the current labels
							JButton showLabelsButton = new JButton(new AbstractAction("Show train data") {
								static final long serialVersionUID=20071015;
								@Override
								public void actionPerformed(ActionEvent ev) {
									new ViewerFrame("Labeled TextBase", new SmartVanillaViewer(testParams.testData));
								}
							});
							// and a button to clear the errorArea
							JButton clearButton = new JButton(new AbstractAction("Clear window") {
								static final long serialVersionUID=20071015;
								@Override
								public void actionPerformed(ActionEvent ev) {
									console.clear();
								}
							});
							// and a button for help
							JButton helpParamsButton = new JButton(new AbstractAction("Parameters") {
								static final long serialVersionUID=20071015;
								@Override
								public void actionPerformed(ActionEvent ev) {
									PrintStream oldSystemOut = System.out;
									ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
									System.setOut(new PrintStream(outBuffer));
									getCLP().usage();
									console.append(outBuffer.toString());
									System.setOut(oldSystemOut);
								}
							});
							subpanel2.add( goButton );
							subpanel2.add( viewButton );
							subpanel2.add( showLabelsButton );
							subpanel2.add( clearButton );
							subpanel2.add( new JLabel("Help:") );
							subpanel2.add( helpParamsButton );
							gbc = Viewer.fillerGBC();	gbc.weighty=0; gbc.gridy=1;
							panel.add(subpanel2, gbc );

							gbc = Viewer.fillerGBC(); gbc.weighty=1; gbc.gridy=2;
							panel.add(errorPanel, gbc);

							// now some progress bars
							JProgressBar progressBar1 = new JProgressBar();
							JProgressBar progressBar2 = new JProgressBar();
							JProgressBar progressBar3 = new JProgressBar();
							ProgressCounter.setGraphicContext(new JProgressBar[]{progressBar1, progressBar2,progressBar3});
							gbc = Viewer.fillerGBC();	gbc.weighty=0; gbc.gridy=3;
							panel.add(progressBar1, gbc);
							gbc = Viewer.fillerGBC(); gbc.weighty=0; gbc.gridy=4;
							panel.add(progressBar2, gbc);
							gbc = Viewer.fillerGBC(); gbc.weighty=0; gbc.gridy=5;
							panel.add(progressBar3, gbc);

							return panel;
						}
					};
					v.setContent(this);
					String className = this.getClass().toString().substring("class ".length());
					new ViewerFrame(className,v);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Use option -help for help");
			}
		}
	}

	public static void main(String[] args) {
		new DataClassificationTask().callMain(args);
	}
}


