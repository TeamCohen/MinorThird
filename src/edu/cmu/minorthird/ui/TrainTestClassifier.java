package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;


// to do:
//  show labels should be a better viewer
//  -baseType type

/**
 * Do a train/test experiment on a text classifier.
 *
 * @author William Cohen
 */

public class TrainTestClassifier extends UIMain
{
  private static Logger log = Logger.getLogger(TrainTestClassifier.class);

	// private data needed to train a classifier

	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ClassificationSignalParams signal = new CommandLineUtil.ClassificationSignalParams(base);
	private CommandLineUtil.TrainClassifierParams train = new CommandLineUtil.TrainClassifierParams();
	private CommandLineUtil.SplitterParams trainTest = new CommandLineUtil.SplitterParams();
	private Object result = null;

	// for GUI
	public CommandLineUtil.SaveParams getSaveParameters() { return save; }
	public void setSaveParameters(CommandLineUtil.SaveParams base) { this.save=save; }
	public CommandLineUtil.ClassificationSignalParams getSignalParameters() { return signal; }
	public void setSignalParameters(CommandLineUtil.ClassificationSignalParams base) { this.signal=signal; }
	public CommandLineUtil.TrainClassifierParams getTrainingParameters() { return train; }
	public void setTrainingParameters(CommandLineUtil.TrainClassifierParams train) { this.train=train; }
	public CommandLineUtil.SplitterParams getSplitterParameters() { return trainTest; }
	public void setSplitterParameters(CommandLineUtil.SplitterParams trainTest) { this.trainTest=trainTest; }

	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(
			new CommandLineProcessor[]{new GUIParams(),base,save,signal,train,trainTest});
	}

	//
	// do the experiment
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (train.learner==null) 
			throw new IllegalArgumentException("-learner must be specified");
		if (signal.spanProp==null && signal.spanType==null) 
			throw new IllegalArgumentException("one of -spanProp or -spanType must be specified");
		if (signal.spanProp!=null && signal.spanType!=null) 
			throw new IllegalArgumentException("only one of -spanProp or -spanType can be specified");

		// construct the dataset
		Dataset d = 
			CommandLineUtil.toDataset(base.labels,train.fe,signal.spanProp,signal.spanType,signal.candidateType);

		// show the data if necessary
		if (train.showData) new ViewerFrame("Dataset", d.toGUI());

		// construct the splitter, if necessary
		if (trainTest.labels!=null) {
			Dataset testData = 
				CommandLineUtil.toDataset(trainTest.labels,train.fe,signal.spanProp,signal.spanType,signal.candidateType);
			trainTest.splitter = new FixedTestSetSplitter(testData.iterator());
		}

		// do the experiment
		CrossValidatedDataset cvd = null;
		Evaluation evaluation = null;
		if (trainTest.showTestDetails) {
			cvd = new CrossValidatedDataset(train.learner,d,trainTest.splitter);
			evaluation = cvd.getEvaluation();
			result = cvd;
		} else {
			cvd = null;
			evaluation = Tester.evaluate(train.learner,d,trainTest.splitter);
			result = evaluation;
		}

		if (base.showResult) {
			new ViewerFrame("Result", new SmartVanillaViewer(result));
		}

		if (save.saveAs!=null) {
			try {
				IOUtil.saveSerialized((Serializable)evaluation,save.saveAs);
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}

		evaluation.summarize();
	}

	public Object getMainResult() { return result; }

	public static void main(String args[])
	{
		new TrainTestClassifier().callMain(args);
	}
}
