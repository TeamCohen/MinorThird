package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.ui.*;
import edu.cmu.minorthird.util.gui.*;

/** Run an annotation-learning experiment based on pre-labeled text
 * , using a sequence learning method, and showing the
 * result of evaluation of the sequence-classification level.
 *
 * @author William Cohen
*/

public class SequenceAnnotatorExpt
{
	private TextLabels labels;
	private Splitter splitter;
	private SequenceClassifierLearner learner;
	private String inputLabel;
	private String tokPropFeats;
	private SequenceDataset sequenceDataset;

	public SequenceAnnotatorExpt(TextLabels labels,Splitter splitter,SequenceClassifierLearner learner,String inputLabel)
	{
		this(labels,splitter,learner,inputLabel,null);
	}

	public 
	SequenceAnnotatorExpt(
		TextLabels labels,Splitter splitter,SequenceClassifierLearner learner,String inputLabel,String tokPropFeats)
	{
		this.labels = labels;
		this.splitter = splitter;
		this.learner = learner;
		this.inputLabel = inputLabel;
		this.tokPropFeats = tokPropFeats;
		AnnotatorTeacher teacher = new TextLabelsAnnotatorTeacher(labels,inputLabel);
		Recommended.TokenFE fe = new Recommended.TokenFE();
		if (tokPropFeats!=null) fe.setTokenPropertyFeatures(tokPropFeats);
		final int size = learner.getHistorySize();
		BatchSequenceClassifierLearner dummyLearner = new BatchSequenceClassifierLearner() {
				public void setSchema(ExampleSchema schema) {}
				public SequenceClassifier batchTrain(SequenceDataset dataset) {return null;}
				public int getHistorySize() { return size; }
			};
		SequenceAnnotatorLearner dummy = new SequenceAnnotatorLearner(dummyLearner,fe) {
				public Annotator getAnnotator() { return null; }
			};
		teacher.train(dummy);
		sequenceDataset = dummy.getSequenceDataset();
	}

	public CrossValidatedSequenceDataset crossValidatedDataset()
	{
		return new CrossValidatedSequenceDataset( learner, sequenceDataset, splitter );
	}

	public Evaluation evaluation()
	{
		Evaluation e = Tester.evaluate( learner, sequenceDataset, splitter );
		return e;
	}

	static public SequenceClassifierLearner toSeqLearner(String learnerName)
	{
		try {
			bsh.Interpreter interp = new bsh.Interpreter();
			interp.eval("import edu.cmu.minorthird.classify.*;");
			interp.eval("import edu.cmu.minorthird.classify.experiments.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
			interp.eval("import edu.cmu.minorthird.classify.sequential.*;");
			interp.eval("import edu.cmu.minorthird.classify.transform.*;");
			return (SequenceClassifierLearner)interp.eval(learnerName);
		} catch (bsh.EvalError e) {
			throw new IllegalArgumentException("error parsing learnerName '"+learnerName+"':\n"+e);
		}
	}

	public static void main(String[] args) 
	{
		TextLabels labels=null;
		Splitter splitter=new RandomSplitter();
		SequenceClassifierLearner learner=null;
		String inputLabel=null;
		String tokPropFeats=null;
		String toShow = "eval";
		try {
			int pos = 0;
			while (pos<args.length) {
				String opt = args[pos++];
				if (opt.startsWith("-lab")) {
					labels = FancyLoader.loadTextLabels(args[pos++]);
				} else if (opt.startsWith("-sp")) {
					splitter = Expt.toSplitter(args[pos++]);
				} else if (opt.startsWith("-lea")) {
					learner = toSeqLearner(args[pos++]);
				} else if (opt.startsWith("-i")) {
					inputLabel = args[pos++];
				} else if (opt.startsWith("-p")) {
					tokPropFeats = args[pos++];
				} else if (opt.startsWith("-sh")) {
					toShow = args[pos++];
				} else {
					usage();
				}
			}
			if (labels==null || learner==null || splitter==null|| inputLabel==null) usage();
			SequenceAnnotatorExpt expt = new SequenceAnnotatorExpt(labels,splitter,learner,inputLabel,tokPropFeats);
			Visible v = null; 
			if (toShow.startsWith("ev")) v = expt.evaluation();
			else if (toShow.startsWith("all")) v = expt.crossValidatedDataset();
			else usage(); 
			ViewerFrame f = new ViewerFrame("Evaluation",v.toGUI());
		} catch (Exception e) {
			e.printStackTrace();
			usage();
		}
	}
	private static void usage() {
		System.out.println("usage: -labels labelsKey -learn learner -input inputLabel -split splitter -show all|eval");
	}
}

