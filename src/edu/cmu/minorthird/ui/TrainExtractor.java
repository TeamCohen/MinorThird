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

public class TrainExtractor implements CommandLineUtil.UIMain
{
  private static Logger log = Logger.getLogger(TrainExtractor.class);

	// private data needed to train a extractor

	private CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();
	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ExtractionSignalParams signal = new CommandLineUtil.ExtractionSignalParams();
	private CommandLineUtil.TrainExtractorParams train = new CommandLineUtil.TrainExtractorParams();
	private Annotator ann = null;

	private CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{base,save,signal,train});
	}

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
		train.learner.setAnnotationType( train.output );

		// echo the input
		if (base.showLabels) {
			Viewer vl = new SmartVanillaViewer();
			vl.setContent(base.labels);
			new ViewerFrame("Textbase",vl);
		}

		// do the training
		AnnotatorTeacher teacher = new TextLabelsAnnotatorTeacher( base.labels, signal.spanType );
		ann = teacher.train( train.learner );

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

	public Object getMainResult() { return ann; }

	public static void main(String args[])
	{
		try {
			TrainExtractor main = new TrainExtractor();
			main.getCLP().processArguments(args);
			main.doMain();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("use option -help for help");
		}
	}
}
