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


/**
 * Train a named-entity extractor.
 *
 * @author William Cohen
 */

public class TrainExtractor 
{
  private static Logger log = Logger.getLogger(TrainExtractor.class);

	// private data needed to train a extractor

	private CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();
	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ExtractionSignalParams signal = new CommandLineUtil.ExtractionSignalParams();
	private CommandLineUtil.TrainExtractorParams train = new CommandLineUtil.TrainExtractorParams();

	private CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{base,save,signal,train});
	}

	//
	// do the experiment
	// 

	public void trainExtractor()
	{
		// check that inputs are valid
		if (base.labels==null) 
			throw new IllegalArgumentException("-labels must be specified");
		if (train.learner==null) 
			throw new IllegalArgumentException("-learner must be specified");
		if (signal.spanType==null) 
			throw new IllegalArgumentException("-spanType must be specified");

		if (train.fe != null) train.learner.setSpanFeatureExtractor(train.fe);

		// echo the input
		Viewer vl = new SmartVanillaViewer();
		vl.setContent(base.labels);
		if (base.showLabels) new ViewerFrame("Textbase",vl);

		AnnotatorTeacher teacher = new TextLabelsAnnotatorTeacher( base.labels, signal.spanType );
		Annotator ann = teacher.train( train.learner );

		if (base.showResult) {
			Viewer av = new SmartVanillaViewer();
			av.setContent(ann);
			new ViewerFrame("Extractor",av); 
		}

		if (save.saveAs!=null) {
			try {
				IOUtil.saveSerialized((Serializable)ann,save.saveAs);
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
	}

	public static void main(String args[])
	{
		TrainExtractor main = new TrainExtractor();
		main.getCLP().processArguments(args);
		main.trainExtractor();
	}
}
