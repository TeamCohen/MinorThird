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

public class TestExtractor extends UIMain
{
  private static Logger log = Logger.getLogger(TestExtractor.class);

	// private data needed to train a extractor

	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ExtractionSignalParams signal = new CommandLineUtil.ExtractionSignalParams(base);
	private CommandLineUtil.TestExtractorParams test = new CommandLineUtil.TestExtractorParams();
	private ExtractionEvaluation evaluation = null;

	// for gui
	public CommandLineUtil.SaveParams getSaveParameters() { return save; }
	public void setSaveParameters(CommandLineUtil.SaveParams p) { save=p; }
	public CommandLineUtil.ExtractionSignalParams getSignalParameters() { return signal; } 
	public void setSignalParameters(CommandLineUtil.ExtractionSignalParams p) { signal=p; } 
	public CommandLineUtil.TestExtractorParams getAdditionalParameters() { return test; } 
	public void setAdditionalParameters(CommandLineUtil.TestExtractorParams p) { test=p; } 

	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,save,signal,test});
	}

	//
	// do the experiment
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (test.loadFrom==null) throw new IllegalArgumentException("-loadFrom must be specified");

		// load the annotator
		ExtractorAnnotator ann = null;
		try {
			ann = (ExtractorAnnotator)IOUtil.loadSerialized(test.loadFrom);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load annotator from "+test.loadFrom+": "+ex);
		}

		if (test.showExtractor) {
			Viewer vx = new SmartVanillaViewer();
			vx.setContent(ann);
			new ViewerFrame("Annotator",vx);
		}
		TextLabels annLabels = ann.annotatedCopy(base.labels);
		
		evaluation = new ExtractionEvaluation();
		SpanDifference sd = 
			new SpanDifference(
				annLabels.instanceIterator(signal.spanType),
				annLabels.instanceIterator(ann.getSpanType()),
				annLabels.closureIterator(signal.spanType));
		System.out.println("Compare "+ann.getSpanType()+" to "+signal.spanType+":");
		System.out.println(sd.toSummary());
		evaluation.extend("Overall performance", sd, true);

		// echo the labels after annotation
		if (base.showResult) {
			Viewer va = new SmartVanillaViewer();
			va.setContent(annLabels);
			new ViewerFrame("Annotated Textbase",va);
			new ViewerFrame("Performance Results", evaluation.toGUI());
		}
	}

	public Object getMainResult() { return evaluation; }

	public static void main(String args[])
	{
		new TestExtractor().callMain(args);
	}
}
