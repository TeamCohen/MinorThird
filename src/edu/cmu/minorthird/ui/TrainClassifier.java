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
 * Train a text classifier.
 *
 * @author William Cohen
 */

public class TrainClassifier implements CommandLineUtil.UIMain
{
  private static Logger log = Logger.getLogger(TrainClassifier.class);

	// private data needed to train a classifier

	private CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();
	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ClassificationSignalParams signal = new CommandLineUtil.ClassificationSignalParams();
	private CommandLineUtil.TrainClassifierParams train = new CommandLineUtil.TrainClassifierParams();
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
		if (signal.spanProp==null && signal.spanType==null) 
			throw new IllegalArgumentException("one of -spanProp or -spanType must be specified");
		if (signal.spanProp!=null && signal.spanType!=null) 
			throw new IllegalArgumentException("only one of -spanProp or -spanType can be specified");

		// echo the input
		if (base.showLabels) {
			Viewer vl = new SmartVanillaViewer();
			vl.setContent(base.labels);
			new ViewerFrame("Textbase",vl);
		}

		// construct the dataset
		Dataset d = CommandLineUtil.toDataset(base.labels,train.fe,signal.spanProp,signal.spanType,signal.candidateType);
		if (train.showData) new ViewerFrame("Dataset", d.toGUI());

		// train the classifier
		Classifier c = new DatasetClassifierTeacher(d).train(train.learner);

		if (base.showResult) {
			Viewer cv = new SmartVanillaViewer();
			cv.setContent(c);
			new ViewerFrame("Classifier",cv); 
		}

		if (save.saveAs!=null) {
			String type = signal.getOutputType(train.output);
			String prop = signal.getOutputProp(train.output);
			ann = new ClassifierAnnotator(train.fe,c,type,prop,signal.candidateType);
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
			TrainClassifier main = new TrainClassifier();
			main.getCLP().processArguments(args);
			main.doMain();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("use option -help for help");
		}
	}
}
