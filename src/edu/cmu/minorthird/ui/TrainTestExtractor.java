package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
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
 * Do a train/test experiment for named-entity extractors.
 *
 * @author William Cohen
 */

public class TrainTestExtractor extends UIMain
{
  private static Logger log = Logger.getLogger(TrainTestExtractor.class);

	// private data needed to train a extractor

	private CommandLineUtil.ExtractionSignalParams signal = new CommandLineUtil.ExtractionSignalParams(base);
	private CommandLineUtil.TrainExtractorParams train = new CommandLineUtil.TrainExtractorParams();
	private CommandLineUtil.SplitterParams trainTest = new CommandLineUtil.SplitterParams();
	TextLabelsExperiment expt; // the main result
	
	// for command-line ui
	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,signal,train,trainTest});
	}

	// for GUI
	public CommandLineUtil.ExtractionSignalParams getSignalParameters() { return signal; }
  public void setSignalParameters(CommandLineUtil.ExtractionSignalParams signal) { this.signal=signal; }
	public CommandLineUtil.TrainExtractorParams getTrainingParameters() { return train; }
	public void setTrainingParameters(CommandLineUtil.TrainExtractorParams train) { this.train=train; }
	public CommandLineUtil.SplitterParams getSplitterParameters() { return trainTest; }
	public void setSplitterParameters(CommandLineUtil.SplitterParams trainTest) { this.trainTest=trainTest; }

	//
	// do the experiment
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (train.learner==null) throw new IllegalArgumentException("-learner must be specified");
		if (signal.spanProp==null && signal.spanType==null) 
			throw new IllegalArgumentException("one of -spanProp or -spanType must be specified");
		if (signal.spanProp!=null && signal.spanType!=null) 
			throw new IllegalArgumentException("only one of -spanProp or -spanType can be specified");

		if (train.fe != null) train.learner.setSpanFeatureExtractor(train.fe);

		// set up the splitter
		if (trainTest.labels!=null) {
			// experiment doesn't support this now
			System.out.println("The -test option is not yet supported for TrainTestExtractor, but");
			System.out.println("you can get the same effect with TrainExtractor then TestExtractor.");
			return;
			//trainTest.splitter = new FixedTestSetSplitter( trainTest.labels.getTextBase().documentSpanIterator() );
			//System.out.println("splitter for test size "+trainTest.labels.getTextBase().size()+" is "+trainTest.splitter);
		}
		expt = new TextLabelsExperiment( base.labels,trainTest.splitter,train.learner,
																		 signal.spanType,signal.spanProp,"_predicted" );
		expt.doExperiment();
		if (base.showResult) {
			new ViewerFrame("Experimental Result",expt.toGUI());
		}
	}

	public Object getMainResult() { return expt; }

	public static void main(String args[])
	{
		new TrainTestExtractor().callMain(args);
	}

}
