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

	private CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();
	private CommandLineUtil.ExtractionSignalParams signal = new CommandLineUtil.ExtractionSignalParams();
	private CommandLineUtil.TrainExtractorParams train = new CommandLineUtil.TrainExtractorParams();
	private CommandLineUtil.SplitterParams trainTest = new CommandLineUtil.SplitterParams();
	TextLabelsExperiment expt; // the main result
	
	// for command-line ui
	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,signal,train,trainTest});
	}

	// for GUI
	public CommandLineUtil.BaseParams getBaseParameters() { return base; }
	public void setBaseParameters(CommandLineUtil.BaseParams base) { this.base=base; }

	//
	// do the experiment
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (base.labels==null) throw new IllegalArgumentException("-labels must be specified");
		if (train.learner==null) throw new IllegalArgumentException("-learner must be specified");
		if (signal.spanType==null) throw new IllegalArgumentException("-spanType must be specified");

		if (train.fe != null) train.learner.setSpanFeatureExtractor(train.fe);

		// echo the input
		if (base.showLabels) {
			Viewer vl = new SmartVanillaViewer();
			vl.setContent(base.labels);
			new ViewerFrame("Textbase",vl);
		}

		// set up the splitter
		if (trainTest.labels!=null) {
			trainTest.splitter = new FixedTestSetSplitter( trainTest.labels.getTextBase().documentSpanIterator() );
		}

		expt = new TextLabelsExperiment( base.labels, trainTest.splitter, train.learner, signal.spanType, "_predicted");

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
