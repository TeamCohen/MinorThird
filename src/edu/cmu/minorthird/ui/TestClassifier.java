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
 * Test an existing text classifier.
 *
 * @author William Cohen
 */

public class TestClassifier 
{
  private static Logger log = Logger.getLogger(TestClassifier.class);

	// private data needed to train a classifier

	private CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();
	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.TestClassifierParams test = new CommandLineUtil.TestClassifierParams();

	private CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{base,save,test});
	}

	//
	// load and test a classifier
	// 

	public void testClassifier()
	{
		// check that inputs are valid
		if (base.labels==null) 
			throw new IllegalArgumentException("-labels must be specified");

		// load the classifier
		ClassifierAnnotator ann = null;
		try {
			ann = (ClassifierAnnotator)IOUtil.loadSerialized(test.loadFrom);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load annotator from "+test.loadFrom);
		}

		// echo the labels after annotation
		if (base.showLabels) {
			ann.annotate(base.labels);
			Viewer vl = new SmartVanillaViewer();
			vl.setContent(base.labels);
			new ViewerFrame("Textbase",vl);
		}

		// do the testing and show the result
		Dataset d = CommandLineUtil.toDataset(base.labels,ann.getFE(),ann.getSpanProperty(),ann.getSpanType());
		Evaluation v = null;
		if (test.showData &&  !test.showClassifier) {
			new ViewerFrame("Dataset", d.toGUI());
		} else if (test.showClassifier && !test.showData) {
			Viewer vc = new SmartVanillaViewer();
			vc.setContent(ann.getClassifier());
			new ViewerFrame("Dataset", vc);
		} 
		if (test.showClassifier && test.showData) {
			ClassifiedDataset cd = new ClassifiedDataset(ann.getClassifier(), d);
			Viewer cdv = new SmartVanillaViewer();
			cdv.setContent(cd);
			new ViewerFrame("Classified Dataset", cdv);			
		} 
		v = new Evaluation(d.getSchema());
		v.extend( ann.getClassifier(), d, 0 );
		if (base.showResult) {
			new ViewerFrame("Evaluation", v.toGUI());
		}

		if (save.saveAs!=null) {
			try {
				IOUtil.saveSerialized((Serializable)v,save.saveAs);
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
		CommandLineUtil.summarizeEvaluation(v);
	}

	public static void main(String args[])
	{
		TestClassifier main = new TestClassifier();
		main.getCLP().processArguments(args);
		main.testClassifier();
	}
}
