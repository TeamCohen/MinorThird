/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Expt;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ViewerFrame;

import java.io.File;
import java.util.Iterator;

/** Test a CMM-based classifier.
 *
 * @author William Cohen
 */

public class CMMTester
{
	private int historySize = 2;

	/** Do some sort of hold-out experiment, as determined by the splitter */
	public Evaluation evaluate(ClassifierLearner learner,SequenceDataset d,Splitter splitter)
	{
		Evaluation v = new Evaluation(d.getSchema()); 
		Dataset.Split s = d.split(splitter);
		ProgressCounter pc = new ProgressCounter("train/test", "experiments", s.getNumPartitions());
		for (int k=0; k<s.getNumPartitions(); k++) {
			SequenceDataset trainData = (SequenceDataset)s.getTrain(k);
			SequenceDataset testData = (SequenceDataset)s.getTest(k);
			trainData.setHistorySize(historySize);
			testData.setHistorySize(historySize);
			Classifier c = new DatasetClassifierTeacher(trainData).train(learner);
			eval( v, c, testData );
			pc.progress();
		}
		pc.finished();
		return v;
	}

	/** Do a train and test experiment */
	public Evaluation evaluate(ClassifierLearner learner,SequenceDataset trainData,SequenceDataset testData)
	{
		trainData.setHistorySize(historySize);
		testData.setHistorySize(historySize);
		Evaluation v = new Evaluation(trainData.getSchema()); 
		Classifier c = new DatasetClassifierTeacher(trainData).train(learner);
		eval( v, c, testData );
		return v;
	}

	private void eval(Evaluation v, Classifier c, SequenceDataset testData)
	{ 
		ProgressCounter pc = new ProgressCounter("evaluating CMM","sequence",testData.numberOfSequences());
		CMM cmm = new CMM(c,historySize,ExampleSchema.BINARY_EXAMPLE_SCHEMA);
		for (Iterator i=testData.sequenceIterator(); i.hasNext(); ) {
			Example[] seq = (Example[]) i.next();
			ClassLabel[] pred = cmm.classification(seq);
			for (int j=0; j<seq.length; j++) {
				v.extend( pred[j],  seq[j] );
			}
			pc.progress();
		}
		pc.finished();
	}

	// clone of classify.experiments.Expt
	public static void main(String[] argv)
	{
		try {
			SequenceDataset train = DatasetLoader.loadSequence(new File(argv[0]));
			ClassifierLearner learner = Expt.toLearner(argv[1]);
			Splitter splitter = Expt.toSplitter(argv[2]);
			File saveFile = argv.length>3 ? new File(argv[3]) : null;
			System.out.println("save: "+saveFile);

			CMMTester tester = new CMMTester();
			Evaluation v = tester.evaluate( learner, train, splitter );
			v.setProperty("train",argv[0]);
			v.setProperty("learner",argv[1]);
			v.setProperty("splitter",argv[2]);
			if (saveFile!=null) {
				v.setProperty("file",saveFile.getName());
				IOUtil.saveSerialized(v, saveFile);
			}
			ViewerFrame f = new ViewerFrame("CMMTester "+StringUtil.toString(argv), v.toGUI());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: seqDataFile learner splitter [saveFile]");
		}
	}
}
