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

public class TestClassifier extends UIMain
{
  private static Logger log = Logger.getLogger(TestClassifier.class);

	// private data needed to test a classifier

	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ClassificationSignalParams signal = new CommandLineUtil.ClassificationSignalParams(base);
	private CommandLineUtil.TestClassifierParams test = new CommandLineUtil.TestClassifierParams();
	private Evaluation result = null;

	// for gui
	public CommandLineUtil.SaveParams getSaveParameters() { return save; }
	public void setSaveParameters(CommandLineUtil.SaveParams p) { save=p; }
	public CommandLineUtil.ClassificationSignalParams getSignalParameters() { return signal; } 
	public void setSignalParameters(CommandLineUtil.ClassificationSignalParams p) { signal=p; } 
	public CommandLineUtil.TestClassifierParams getAdditionalParameters() { return test; } 
	public void setAdditionalParameters(CommandLineUtil.TestClassifierParams p) { test=p; } 

	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,save,signal,test});
	}

	//
	// load and test a classifier
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (test.loadFrom==null) throw new IllegalArgumentException("-loadFrom must be specified");

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
		 new TestClassifier().callMain(args);
	}
}
