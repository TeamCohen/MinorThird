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

public class TrainExtractor extends UIMain
{
  private static Logger log = Logger.getLogger(TrainExtractor.class);

	// private data needed to train a extractor

	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ExtractionSignalParams signal = new CommandLineUtil.ExtractionSignalParams(base);
	private CommandLineUtil.TrainExtractorParams train = new CommandLineUtil.TrainExtractorParams();
	private Annotator ann = null;

	// for gui
	public CommandLineUtil.SaveParams getSaveParameters() { return save; }
	public void setSaveParameters(CommandLineUtil.SaveParams p) { save=p; }
	public CommandLineUtil.TrainExtractorParams getAdditionalParameters() { return train; } 
	public void setAdditionalParameters(CommandLineUtil.TrainExtractorParams p) { train=p; } 
	public CommandLineUtil.ExtractionSignalParams getSignalParameters() { return signal; }
	public void setSignalParameters(CommandLineUtil.ExtractionSignalParams newSignal) { this.signal=newSignal; }


	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,save,signal,train});
	}

	//
	// do the experiment
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (train.learner==null) throw new IllegalArgumentException("-learner must be specified");
		if (signal.spanType==null) throw new IllegalArgumentException("-spanType must be specified");

		if (train.fe != null) train.learner.setSpanFeatureExtractor(train.fe);
		train.learner.setAnnotationType( train.output );

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
		new TrainExtractor().callMain(args);
	}
}
