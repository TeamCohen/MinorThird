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

	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ExtractionSignalParams signal = new CommandLineUtil.ExtractionSignalParams(base);
	private CommandLineUtil.TrainExtractorParams train = new CommandLineUtil.TrainExtractorParams();
	private CommandLineUtil.SplitterParams trainTest = new CommandLineUtil.SplitterParams();    
	private Object result = null;
	
	// for command-line ui
	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,save,signal,train,trainTest});
	}

	// for GUI
	public CommandLineUtil.SaveParams getSaveParameters() { return save; }
	public void setSaveParameters(CommandLineUtil.SaveParams base) { this.save=save; }
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

		//no longer needed
		//if (train.fe != null) {
		//System.out.println("setting fe to "+train.fe);
		//train.learner.setSpanFeatureExtractor(train.fe);
		//}
			
		// set up the splitter
		if (trainTest.labels!=null) {
			trainTest.splitter = new FixedTestSetSplitter( trainTest.labels.getTextBase().documentSpanIterator() );
			System.out.println("splitter for test size "+trainTest.labels.getTextBase().size()+" is "+trainTest.splitter);
		}
		TextLabelsExperiment 
			expt = new TextLabelsExperiment( base.labels,trainTest.splitter,trainTest.labels,train.learner,
							 signal.spanType,signal.spanProp,train.output );
		expt.doExperiment();
		ExtractionEvaluation evaluation = expt.getEvaluation();

		if (trainTest.showTestDetails) result = expt;
		else result = evaluation;

		if (base.showResult) new ViewerFrame("Experimental Result",new SmartVanillaViewer(result));

		if (save.saveAs!=null) {
			try {
				IOUtil.saveSerialized((Serializable)evaluation,save.saveAs);
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}

	}

	public Object getMainResult() { return result; }

	public static void main(String args[])
	{
		new TrainTestExtractor().callMain(args);
	}

}
