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

public class TestExtractor 
{
  private static Logger log = Logger.getLogger(TestExtractor.class);

	// private data needed to train a extractor

	private CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();
	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.TestExtractorParams test = new CommandLineUtil.TestExtractorParams();

	private CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{base,save,test});
	}

	//
	// do the experiment
	// 

	public void testExtractor()
	{
		// check that inputs are valid
		if (base.labels==null) 
			throw new IllegalArgumentException("-labels must be specified");

		// echo the input
		Viewer vl = new SmartVanillaViewer();
		vl.setContent(base.labels);
		if (base.showLabels) new ViewerFrame("Textbase",vl);

		// load the annotator
		Annotator ann = null;
		try {
			ann = (Annotator)IOUtil.loadSerialized(test.loadFrom);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load annotator from "+test.loadFrom);
		}

		if (test.showExtractor) {
			Viewer vx = new SmartVanillaViewer();
			vx.setContent(ann);
			new ViewerFrame("Annotator",vx);
		}

		// echo the labels after annotation
		if (base.showLabels) {
			ann.annotate(base.labels);
			Viewer va = new SmartVanillaViewer();
			va.setContent(base.labels);
			new ViewerFrame("Textbase",va);
		}
		
	}

	public static void main(String args[])
	{
		TestExtractor main = new TestExtractor();
		main.getCLP().processArguments(args);
		main.testExtractor();
	}
}
