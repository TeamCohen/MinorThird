package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.learn.experiments.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;


/**
 * Do a train/test experiment for word taggers.
 *
 * @author William Cohen
 */

public class TrainTestTagger extends UIMain
{
  private static Logger log = Logger.getLogger(TrainTestTagger.class);

	// private data needed to train a extractor

	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.TaggerSignalParams signal = new CommandLineUtil.TaggerSignalParams(base);
	private CommandLineUtil.TrainTaggerParams train = new CommandLineUtil.TrainTaggerParams();
	private CommandLineUtil.SplitterParams trainTest = new CommandLineUtil.SplitterParams();
	Object result=null; // the main result
	
	// for command-line ui
	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,signal,train,trainTest});
	}

	// for GUI
	public CommandLineUtil.SaveParams getSaveParameters() { return save; }
	public void setSaveParameters(CommandLineUtil.SaveParams base) { this.save=save; }
	public CommandLineUtil.TaggerSignalParams getSignalParameters() { return signal; }
  public void setSignalParameters(CommandLineUtil.TaggerSignalParams signal) { this.signal=signal; }
	public CommandLineUtil.TrainTaggerParams getTrainingParameters() { return train; }
	public void setTrainingParameters(CommandLineUtil.TrainTaggerParams train) { this.train=train; }
	public CommandLineUtil.SplitterParams getSplitterParameters() { return trainTest; }
	public void setSplitterParameters(CommandLineUtil.SplitterParams trainTest) { this.trainTest=trainTest; }

	//
	// do the experiment
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (train.learner==null) throw new IllegalArgumentException("-learner must be specified");
		if (signal.tokenProp==null) throw new IllegalArgumentException("-tokenProp must be specified");

		SequenceDataset dataset = 
			CommandLineUtil.toSequenceDataset(base.labels,train.fe,train.learner.getHistorySize(),signal.tokenProp);
		if (train.showData) new ViewerFrame("Dataset", dataset.toGUI());

		// set up the splitter
		if (trainTest.labels!=null) {
			SequenceDataset testDataset = 
				CommandLineUtil.toSequenceDataset(
					trainTest.labels,train.fe,train.learner.getHistorySize(),signal.tokenProp);
			trainTest.splitter = new FixedTestSetSplitter( testDataset.iterator() );
		}

		//DatasetIndex index = new DatasetIndex(sequenceDataset);
		//System.out.println("Dataset: examples "+sequenceDataset.size()
		//+" features: "+index.numberOfFeatures()
		//+" avg features/examples: "+index.averageFeaturesPerExample());

		System.out.println("showData="+train.showData+" showResult="+base.showResult+" useGUI="+useGUI);
		CrossValidatedSequenceDataset cvd = null;
		Evaluation evaluation = null;
		if (trainTest.showTestDetails) {
			cvd = new CrossValidatedSequenceDataset( train.learner, dataset, trainTest.splitter );
			evaluation = cvd.getEvaluation();
			result = cvd;
		} else {
			cvd = null;
			evaluation = Tester.evaluate( train.learner,dataset,trainTest.splitter );
			result = evaluation;
		}

		if (base.showResult) new ViewerFrame("Result", new SmartVanillaViewer(result));

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
		new TrainTestTagger().callMain(args);
	}

}
