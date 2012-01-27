package edu.cmu.minorthird.ui;

import java.io.IOException;
import java.io.Serializable;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.experiments.FixedTestSetSplitter;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.learn.experiments.ExtractionEvaluation;
import edu.cmu.minorthird.text.learn.experiments.TextLabelsExperiment;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;


/**
 * Do a train/test experiment for named-entity extractors.
 *
 * @author William Cohen
 */

public class TrainTestExtractor extends UIMain
{
	static Logger log = Logger.getLogger(TrainTestExtractor.class);

	// private data needed to train a extractor

	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ExtractionSignalParams signal = new CommandLineUtil.ExtractionSignalParams(base);
	private CommandLineUtil.TrainExtractorParams train = new CommandLineUtil.TrainExtractorParams();
	private CommandLineUtil.SplitterParams trainTest = new CommandLineUtil.SplitterParams();    
	private Object result = null;

	public String getTrainTestExtractorHelp() {
		return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/TrainTestExtractor%20Tutorial.htm\">TrainTestExtractor Tutorial</A></html>";
	}

	// for command-line ui
	@Override
	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,save,signal,train,trainTest});
	}

	// for GUI
	public CommandLineUtil.SaveParams getSaveParameters() { return save; }
	public void setSaveParameters(CommandLineUtil.SaveParams save) { this.save=save; }
	public CommandLineUtil.ExtractionSignalParams getSignalParameters() { return signal; }
	public void setSignalParameters(CommandLineUtil.ExtractionSignalParams signal) { this.signal=signal; }
	public CommandLineUtil.TrainExtractorParams getTrainingParameters() { return train; }
	public void setTrainingParameters(CommandLineUtil.TrainExtractorParams train) { this.train=train; }
	public CommandLineUtil.SplitterParams getSplitterParameters() { return trainTest; }
	public void setSplitterParameters(CommandLineUtil.SplitterParams trainTest) { this.trainTest=trainTest; }

	//
	// do the experiment
	// 

	@Override
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
		if(trainTest.labels!=null){
			if(signal.spanPropString!=null){
				CommandLineUtil.createSpanProp(signal.spanPropString, trainTest.labels);
			}
			trainTest.splitter = new FixedTestSetSplitter<Span>( trainTest.labels.getTextBase().documentSpanIterator() );
			System.out.println("splitter for test size "+trainTest.labels.getTextBase().size()+" is "+trainTest.splitter);
		}
		TextLabelsExperiment expt = new TextLabelsExperiment(
				base.labels,
				trainTest.splitter,
				trainTest.labels,
				train.learner,
				signal.spanType,
				signal.spanProp,
				train.output );
		expt.doExperiment();
		ExtractionEvaluation evaluation = expt.getEvaluation();

		if (trainTest.showTestDetails) result = expt;
		else result = evaluation;

		if (base.showResult) new ViewerFrame("Experimental Result",new SmartVanillaViewer(result));

		if (save.saveAs!=null) {
			try {
				IOUtil.saveSerialized(evaluation,save.saveAs);
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}

	}

	@Override
	public Object getMainResult() { return result; }

	public static void main(String args[])
	{
		new TrainTestExtractor().callMain(args);
	}

}
