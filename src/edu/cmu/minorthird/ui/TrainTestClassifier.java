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


// to do:
//  show labels should be a better viewer
//  -baseType type

/**
 * Do a train/test experiment on a text classifier.
 *
 * @author William Cohen
 */

public class TrainTestClassifier 
{
  private static Logger log = Logger.getLogger(TrainClassifier.class);

	// private data needed to train a classifier

	private CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();
	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ClassificationSignalParams signal = new CommandLineUtil.ClassificationSignalParams();
	private CommandLineUtil.TrainClassifierParams train = new CommandLineUtil.TrainClassifierParams();
	private CommandLineUtil.SplitterParams trainTest = new CommandLineUtil.SplitterParams();
	private SpanFeatureExtractor fe = new SampleFE.BagOfLowerCaseWordsFE();

	private CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{base,save,signal,train,trainTest});
	}

	//
	// do the experiment
	// 

	public void trainTestClassifier()
	{
		// check that inputs are valid
		if (base.labels==null) 
			throw new IllegalArgumentException("-labels must be specified");
		if (train.learner==null) 
			throw new IllegalArgumentException("-learner must be specified");
		if (signal.spanProp==null && signal.spanType==null) 
			throw new IllegalArgumentException("one of -spanProp or -spanType must be specified");
		if (signal.spanProp!=null && signal.spanType!=null) 
			throw new IllegalArgumentException("only one of -spanProp or -spanType can be specified");
		if (trainTest.splitter==null && trainTest.labels==null) 
			throw new IllegalArgumentException("one of -splitter or -test must be specified");
		if (trainTest.splitter!=null && trainTest.labels!=null) 
			throw new IllegalArgumentException("only one of -splitter or -test can be specified");

		// echo the input
		Viewer vl = new SmartVanillaViewer();
		vl.setContent(base.labels);
		if (base.showLabels) new ViewerFrame("Textbase",vl);

		// construct the dataset
		Dataset d = CommandLineUtil.toDataset(base.labels,fe,signal.spanProp,signal.spanType);
		if (train.showData && !base.showResult) new ViewerFrame("Dataset", d.toGUI());

		// construct the splitter, if necessary
		if (trainTest.splitter==null) {
			Dataset testData = CommandLineUtil.toDataset(trainTest.labels,fe,signal.spanProp,signal.spanType);
			trainTest.splitter = new FixedTestSetSplitter(testData.iterator());
		}

		Evaluation v = null;
		CrossValidatedDataset cvd = null;
		System.out.println("showData="+train.showData+" showResult="+base.showResult);
		if (train.showData && base.showResult) {
			System.out.println("build cvd");
			cvd = new CrossValidatedDataset(train.learner,d,trainTest.splitter);
			v = cvd.getEvaluation();
		} else {
			System.out.println("build eval");
			v = Tester.evaluate(train.learner,d,trainTest.splitter);
		}

		if (base.showResult) {
			if (cvd!=null) {
				System.out.println("showe cvd=");
				new ViewerFrame("CrossValidatedDataset", cvd.toGUI());
			} else {
				System.out.println("showe v");
				new ViewerFrame("Evaluation", v.toGUI());
			}
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
		TrainTestClassifier main = new TrainTestClassifier();
		main.getCLP().processArguments(args);
		main.trainTestClassifier();
	}
}
