/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
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

import edu.cmu.minorthird.classify.ClassifyCommandLineUtil.MultiTrainTestParams;
import edu.cmu.minorthird.classify.ClassifyCommandLineUtil.SeqTrainTestParams;
import edu.cmu.minorthird.classify.algorithms.knn.KnnLearner;
import edu.cmu.minorthird.classify.algorithms.linear.MaxEntLearner;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.experiments.CrossValidatedDataset;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.classify.experiments.StratifiedCrossValSplitter;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.classify.multi.InstanceFromPrediction;
import edu.cmu.minorthird.classify.multi.MultiClassLabel;
import edu.cmu.minorthird.classify.multi.MultiClassifier;
import edu.cmu.minorthird.classify.multi.MultiCrossValidatedDataset;
import edu.cmu.minorthird.classify.multi.MultiDataset;
import edu.cmu.minorthird.classify.multi.MultiEvaluation;
import edu.cmu.minorthird.classify.multi.MultiExample;
import edu.cmu.minorthird.classify.sequential.CollinsPerceptronLearner;
import edu.cmu.minorthird.classify.sequential.CrossValidatedSequenceDataset;
import edu.cmu.minorthird.classify.sequential.GenericCollinsLearner;
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

public class UI{

	private static Logger log=Logger.getLogger(UI.class);

	private static final Class<?>[] SELECTABLE_TYPES=
			new Class[]{DataClassificationTask.class,
					ClassifyCommandLineUtil.TrainParams.class,
					ClassifyCommandLineUtil.TestParams.class,
					ClassifyCommandLineUtil.TrainTestParams.class,
					ClassifyCommandLineUtil.Learner.SequentialLearner.class,
					ClassifyCommandLineUtil.Learner.ClassifierLearner.class,KnnLearner.class,
					NaiveBayes.class,VotedPerceptron.class,SVMLearner.class,
					DecisionTreeLearner.class,AdaBoost.class,BatchVersion.class,
					TransformingBatchLearner.class,
					MaxEntLearner.class,
					// transformations
					FrequencyBasedTransformLearner.class,InfoGainTransformLearner2.class,
					T1InstanceTransformLearner.class,TFIDFTransformLearner.class,
					// sequential learner
					CollinsPerceptronLearner.class,GenericCollinsLearner.class,
					// splitters
					CrossValSplitter.class,RandomSplitter.class,
					StratifiedCrossValSplitter.class,};

//	private static final Set LEGAL_OPS=
//			new HashSet(Arrays.asList(new String[]{"train","test","trainTest"}));

	public static class DataClassificationTask implements
			CommandLineProcessor.Configurable,/*Saveable,*/Console.Task{

		private ClassifyCommandLineUtil.TrainTestParams trainTestParams=
				new ClassifyCommandLineUtil.TrainTestParams();

		public Object resultToShow;

		public boolean useGUI;

		public Console.Task main;

		// for gui
		public ClassifyCommandLineUtil.TrainTestParams getTrainTestParameters(){
			return trainTestParams;
		}

		public void setTrainTestParameters(ClassifyCommandLineUtil.TrainTestParams p){
			trainTestParams=p;
		}

		protected class GUIParams extends BasicCommandLineProcessor{

			public void gui(){
				useGUI=true;
			}

			@Override
			public void usage(){
				System.out.println("presentation parameters:");
				System.out
						.println(" -gui                     use graphic interface to set parameters");
				System.out.println();
			}
		}

		public String getDatasetFilename(){
			return trainTestParams.trainDataFilename;
		}

		@Override
		public CommandLineProcessor getCLP(){
			JointCommandLineProcessor jlpTrainTest=
					new JointCommandLineProcessor(new CommandLineProcessor[]{
							new GUIParams(),trainTestParams});
			return jlpTrainTest;
		}

		/** Returns whether base.labels exits */
		@Override
		public boolean getLabels(){
			return(getDatasetFilename()!=null);
		}

		public MultiDataset annotateData(MultiClassifier multiClassifier,
				MultiDataset md){
			MultiDataset annotatedDataset=new MultiDataset();
			for(Iterator<MultiExample> i=md.multiIterator();i.hasNext();){
				MultiExample ex=i.next();
				Instance instance=ex.asInstance();
				MultiClassLabel predicted=
						multiClassifier.multiLabelClassification(instance);
				Instance annotatedInstance=
						new InstanceFromPrediction(instance,predicted.bestClassName());
				MultiExample newEx=
						new MultiExample(annotatedInstance,ex.getMultiLabel(),ex
								.getWeight());
				annotatedDataset.addMulti(newEx);
			}
			return annotatedDataset;
		}

		// main action
		@Override
		public void doMain(){
			//Work in more tests
			if(trainTestParams.getTrainData()==null){
				System.out
						.println("The training data needs to be specified with the -data option.");
				return;
			}
			if(trainTestParams instanceof ClassifyCommandLineUtil.SeqTrainTestParams&&
					(!(trainTestParams.getTrainData() instanceof SequenceDataset))){
				System.out.println("The training data should be a sequence dataset");
				return;
			}
			if(trainTestParams.multi>0&&
					(!(trainTestParams.getTrainData() instanceof MultiDataset))){
				System.out.println("The training data should be a multi dataset");
				return;
			}
			if(trainTestParams.showData)
				new ViewerFrame("Training data",trainTestParams.getTrainData().toGUI());

			if(trainTestParams.showTestDetails&&
					trainTestParams.typeString.equals("seq")){
				CrossValidatedSequenceDataset cvd=
						new CrossValidatedSequenceDataset(
								trainTestParams.seqLnr.seqLearner,
								(SequenceDataset)trainTestParams.getTrainData(),
								((SeqTrainTestParams)trainTestParams).splitter);
				trainTestParams.resultToShow=cvd;
				trainTestParams.resultToSave=cvd.getEvaluation();
				((Evaluation)trainTestParams.resultToSave).summarize();
			}else if(!trainTestParams.showTestDetails&&
					trainTestParams.typeString.equals("seq")){
				Evaluation e=
						Tester.evaluate(trainTestParams.seqLnr.seqLearner,
								(SequenceDataset)trainTestParams.getTrainData(),
								trainTestParams.sequenceSplitter);
				trainTestParams.resultToShow=trainTestParams.resultToSave=e;
				((Evaluation)trainTestParams.resultToSave).summarize();
			}else if(trainTestParams.showTestDetails&&
					trainTestParams.typeString.equals("multi")){
				MultiCrossValidatedDataset cvd=
						new MultiCrossValidatedDataset(trainTestParams.clsLnr.clsLearner,
								(MultiDataset)trainTestParams.getTrainData(),
								((MultiTrainTestParams)trainTestParams).splitter);
				trainTestParams.resultToShow=cvd;
				trainTestParams.resultToSave=cvd.getEvaluation();
				((MultiEvaluation)trainTestParams.resultToSave).summarize();
			}else if(!trainTestParams.showTestDetails&&
					trainTestParams.typeString.equals("multi")){
				//Evaluation e = Tester.evaluate(trainTestParams.clsLearner,trainTestParams.getTrainData(),trainTestParams.splitter);
				MultiEvaluation e=
						Tester.multiEvaluate(trainTestParams.clsLnr.clsLearner,
								(MultiDataset)trainTestParams.getTrainData(),
								((MultiTrainTestParams)trainTestParams).splitter);
				trainTestParams.resultToShow=trainTestParams.resultToSave=e;
				((MultiEvaluation)trainTestParams.resultToSave).summarize();
			}else if(trainTestParams.showTestDetails&&
					!(trainTestParams.equals("seq"))){
				//CrossValidatedDataset cvd = new CrossValidatedDataset(trainTestParams.clsLearner, trainTestParams.getTrainData(), trainTestParams.splitter);
				CrossValidatedDataset cvd=
						new CrossValidatedDataset(trainTestParams.clsLnr.clsLearner,
								trainTestParams.getTrainData(),trainTestParams.splitter);
				trainTestParams.resultToShow=cvd;
				trainTestParams.resultToSave=cvd.getEvaluation();
				((Evaluation)trainTestParams.resultToSave).summarize();
			}else if(!trainTestParams.showTestDetails&&
					!(trainTestParams.typeString.equals("seq"))){
				//Evaluation e = Tester.evaluate(trainTestParams.clsLearner,trainTestParams.getTrainData(),trainTestParams.splitter);
				Evaluation e=
						Tester.evaluate(trainTestParams.clsLnr.clsLearner,trainTestParams
								.getTrainData(),trainTestParams.splitter);
				trainTestParams.resultToShow=trainTestParams.resultToSave=e;
				((Evaluation)trainTestParams.resultToSave).summarize();
			}

			resultToShow=trainTestParams.resultToShow;
			// attach all the command-line arguments to the resultToSave, as properties
			/*for (Iterator i=clp.propertyList().iterator(); i.hasNext(); ) {
			  String prop = (String)i.next();
			  ((Evaluation)resultToSave).setProperty(prop,clp.propertyValue(prop));
			  }*/

			if(trainTestParams.showResult)
				new ViewerFrame("Result",new SmartVanillaViewer(
						trainTestParams.resultToShow));
			if(trainTestParams.saveAs!=null){
				if(IOUtil.saveSomehow(trainTestParams.resultToSave,
						trainTestParams.saveAs)){
					log.info("Result saved in "+trainTestParams.saveAs);
				}else{
					log.error("Can't save "+trainTestParams.resultToSave.getClass()+
							" to "+trainTestParams.saveAs);
				}
			}
		}

		@Override
		public Object getMainResult(){
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
		public void callMain(final String[] args){

			try{
				getCLP().processArguments(args);
				if(!useGUI){
					doMain();
				}else{
					main=this;
					final Viewer v=new ComponentViewer(){
						static final long serialVersionUID=20080128L;
						@Override
						public JComponent componentFor(Object o){
							Viewer ts=
									new TypeSelector(SELECTABLE_TYPES,"selectableTypes.txt",
											DataClassificationTask.class);
							ts.setContent(o);

							// we'll put the type selector in a nice panel
							JPanel panel=new JPanel();
							panel.setBorder(new TitledBorder(StringUtil.toString(args,
									"Command line: ",""," ")));
							panel.setLayout(new GridBagLayout());
							GridBagConstraints gbc;

							// another panel to allow parameter modifications
							JPanel subpanel1=new JPanel();
							subpanel1.setBorder(new TitledBorder("Parameter modification"));
							//subpanel1.add(new JLabel("Use the edit button to change the parameters given in the command line"));
							subpanel1.add(ts);
							gbc=Viewer.fillerGBC();
							gbc.weighty=0;
							panel.add(subpanel1,gbc);

							// a control panel for controls
							JPanel subpanel2=new JPanel();
							subpanel2.setBorder(new TitledBorder("Execution controls"));
							// a button to show the results
							final JButton viewButton=
									new JButton(new AbstractAction("View results"){
										static final long serialVersionUID=20080128L;
										@Override
										public void actionPerformed(ActionEvent event){
											Viewer rv=new SmartVanillaViewer();
											rv.setContent(getMainResult());
											new ViewerFrame("Result",rv);
										}
									});
							viewButton.setEnabled(false);

							// another panel for error messages and other outputs
							JPanel errorPanel=new JPanel();
							errorPanel
									.setBorder(new TitledBorder("Error messages and output"));
							final Console console=
									new Console(main,getDatasetFilename()!=null,viewButton);
							errorPanel.add(console.getMainComponent());

							// a button to start this thread
							JButton goButton=new JButton(new AbstractAction("Start task"){
								static final long serialVersionUID=20080128L;
								@Override
								public void actionPerformed(ActionEvent event){
									console.start();
								}
							});
							// and a button to show the current labels
							JButton showLabelsButton=
									new JButton(new AbstractAction("Show train data"){
										static final long serialVersionUID=20080128L;
										@Override
										public void actionPerformed(ActionEvent ev){
											new ViewerFrame("Labeled TextBase",
													new SmartVanillaViewer(trainTestParams.trainData));
										}
									});
							// and a button to clear the errorArea
							JButton clearButton=
									new JButton(new AbstractAction("Clear window"){
										static final long serialVersionUID=20080128L;
										@Override
										public void actionPerformed(ActionEvent ev){
											console.clear();
										}
									});
							// and a button for help
							JButton helpParamsButton=
									new JButton(new AbstractAction("Parameters"){
										static final long serialVersionUID=20080128L;
										@Override
										public void actionPerformed(ActionEvent ev){
											PrintStream oldSystemOut=System.out;
											ByteArrayOutputStream outBuffer=
													new ByteArrayOutputStream();
											System.setOut(new PrintStream(outBuffer));
											//clp.usage();
											console.append(outBuffer.toString());
											System.setOut(oldSystemOut);
										}
									});
							subpanel2.add(goButton);
							subpanel2.add(viewButton);
							subpanel2.add(showLabelsButton);
							subpanel2.add(clearButton);
							subpanel2.add(new JLabel("Help:"));
							subpanel2.add(helpParamsButton);
							gbc=Viewer.fillerGBC();
							gbc.weighty=0;
							gbc.gridy=1;
							panel.add(subpanel2,gbc);

							gbc=Viewer.fillerGBC();
							gbc.weighty=1;
							gbc.gridy=2;
							panel.add(errorPanel,gbc);

							// now some progress bars
							JProgressBar progressBar1=new JProgressBar();
							JProgressBar progressBar2=new JProgressBar();
							JProgressBar progressBar3=new JProgressBar();
							ProgressCounter.setGraphicContext(new JProgressBar[]{
									progressBar1,progressBar2,progressBar3});
							gbc=Viewer.fillerGBC();
							gbc.weighty=0;
							gbc.gridy=3;
							panel.add(progressBar1,gbc);
							gbc=Viewer.fillerGBC();
							gbc.weighty=0;
							gbc.gridy=4;
							panel.add(progressBar2,gbc);
							gbc=Viewer.fillerGBC();
							gbc.weighty=0;
							gbc.gridy=5;
							panel.add(progressBar3,gbc);

							return panel;
						}
					};
					v.setContent(this);
					String className=
							this.getClass().toString().substring("class ".length());
					new ViewerFrame(className,v);
				}
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("Use option -help for help");
			}
		}
	}

	public static void main(String[] args){
		new DataClassificationTask().callMain(args);
	}
}
