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

public class TestClassifier implements CommandLineUtil.UIMain
{
  private static Logger log = Logger.getLogger(TestClassifier.class);

	// private data needed to test a classifier

	private CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();
	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ClassificationSignalParams signal = new CommandLineUtil.ClassificationSignalParams();
	private CommandLineUtil.TestClassifierParams test = new CommandLineUtil.TestClassifierParams();
	private Evaluation result = null;

	private CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{base,save,signal,test});
	}

	//
	// load and test a classifier
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (base.labels==null) throw new IllegalArgumentException("-labels must be specified");
		if (test.loadFrom==null) throw new IllegalArgumentException("-loadFrom must be specified");

		// echo the labels after annotation
		if (base.showLabels) {
			Viewer vl = new SmartVanillaViewer();
			vl.setContent(base.labels);
			new ViewerFrame("Textbase",vl);
		}

		// load the classifier
		ClassifierAnnotator ann = null;
		try {
			ann = (ClassifierAnnotator)IOUtil.loadSerialized(test.loadFrom);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load annotator from "+test.loadFrom+": "+ex);
		}

		// do the testing and show the result
		Dataset d = 
			CommandLineUtil.toDataset(base.labels,ann.getFE(),signal.spanProp,signal.spanType,signal.candidateType);
		Evaluation result = null;
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
		result = new Evaluation(d.getSchema());
		result.extend( ann.getClassifier(), d, 0 );
		if (base.showResult) {
			new ViewerFrame("Evaluation", result.toGUI());
		}

		if (save.saveAs!=null) {
			try {
				IOUtil.saveSerialized((Serializable)result,save.saveAs);
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
		CommandLineUtil.summarizeEvaluation(result);
	}

	public Object getMainResult() { return result; }

	public static void main(String args[])
	{
		TestClassifier main = new TestClassifier();
		try {
			main.getCLP().processArguments(args);
			main.doMain();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Use option -help for help");
		}
	}
}
