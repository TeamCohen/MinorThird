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
 * Apply a serialized annotator.
 *
 * @author William Cohen
 */

public class ApplyAnnotator extends UIMain
{
  private static Logger log = Logger.getLogger(ApplyAnnotator.class);

	// private data needed to test a classifier

	private CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();
	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.LoadAnnotatorParams load = new CommandLineUtil.LoadAnnotatorParams();
	private Evaluation result = null;

	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{base,save,load});
	}

	//
	// load and test a classifier
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (base.labels==null) throw new IllegalArgumentException("-labels must be specified");
		if (load.loadFrom==null) throw new IllegalArgumentException("-loadFrom must be specified");

		// echo the labels 
		if (base.showLabels) {
			Viewer vl = new SmartVanillaViewer();
			vl.setContent(base.labels);
			new ViewerFrame("Textbase",vl);
		}

		// load the classifier
		ClassifierAnnotator ann = null;
		try {
			ann = (ClassifierAnnotator)IOUtil.loadSerialized(load.loadFrom);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load annotator from "+load.loadFrom);
		}

		// do the annotation
		TextLabels annLabels = ann.annotatedCopy(base.labels);

		// echo the annotated labels 
		if (base.showResult) {
			Viewer vl = new SmartVanillaViewer();
			vl.setContent(annLabels);
			new ViewerFrame("Annotated Textbase",vl);
		}
		
		if (save.saveAs!=null) {
			try {
				new TextLabelsLoader().saveTypesAsOps( annLabels, save.saveAs );
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
	}

	public Object getMainResult() { return result; }

	public static void main(String args[])
	{
		new ApplyAnnotator().callMain(args);
	}
}
