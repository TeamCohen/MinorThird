package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.learn.*;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/** Run an annotation-learning experiment based on pre-labeled text.
 *
 * @author William Cohen
*/

public class ClassifyExperiment
{
	private SpanFeatureExtractor fe = new SampleFE.BagOfLowerCaseWordsFE();

	private TextLabels labels;
	private Splitter splitter;
	private ClassifierLearner learner;
	private String inputLabel;
  private static Logger log = Logger.getLogger(ClassifyExperiment.class);

  /**
   * @param labels The labels and base to be annotated in the example
   *               These are the training examples
   * @param splitter splitter for the documents in the labels to create test vs. train
   * @param learnerName ClassifierLearner algorithm object to use
   * @param inputLabel spanType in the TextLabels to treat as 'positive'.
   */
	public ClassifyExperiment(
		TextLabels labels,Splitter splitter,String learnerName,String inputLabel) 
	{
		this.labels = labels;
		this.splitter = splitter;
		this.inputLabel = inputLabel;
		this.learner = Expt.toLearner(learnerName);
	}

	public ClassifyExperiment(
		TextLabels labels,Splitter splitter,ClassifierLearner learner,String inputLabel)
	{
		this.labels = labels;
		this.splitter = splitter;
		this.inputLabel = inputLabel;
		this.learner = learner;
	}

	public Dataset asDataset() 
	{
		Dataset dataset = new BasicDataset();
		for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
			Span s = i.nextSpan();
			double classLabel = labels.hasType(s,inputLabel) ? +1 : -1;
			dataset.add( new BinaryExample( fe.extractInstance(labels,s), classLabel) );
		}
		return dataset;
	}

	public Evaluation evaluation() 
	{
		return Tester.evaluate( learner, asDataset(), splitter );
	}

	public CrossValidatedDataset crossValidatedDataset() 
	{
		return new CrossValidatedDataset( learner, asDataset(), splitter );
	}

	public static void main(String[] args) 
	{
		Splitter splitter=new RandomSplitter(0.7);
		String learnerName="new BatchVersion(new VotedPerceptron())";
		TextLabels labels=null;
		String inputLabel=null;
		String show=null;
		try {
			int pos = 0;
			while (pos<args.length) {
				String opt = args[pos++];
				if (opt.startsWith("-lab")) {
					labels = FancyLoader.loadTextLabels(args[pos++]);
				} else if (opt.startsWith("-split")) {
					splitter = Expt.toSplitter(args[pos++]);
				} else if (opt.startsWith("-lea")) {
					learnerName = args[pos++];
				} else if (opt.startsWith("-in")) {
					inputLabel = args[pos++];
				} else if (opt.startsWith("-show")) {
					show = args[pos++];
				} else {
					usage();
				}
			}
			if (labels==null || learnerName==null || splitter==null|| inputLabel==null) {
				usage();
			}
      ClassifyExperiment expt = new ClassifyExperiment(labels,splitter,learnerName,inputLabel);
			if ("all+".equals(show)) {
				ViewerFrame f = new ViewerFrame("Experiment Result", expt.crossValidatedDataset().toGUI() );				
			}	else if (show!=null) {
				ViewerFrame f = new ViewerFrame("Experiment Result", expt.evaluation().toGUI() );
			}
		} catch (Exception e) {
			e.printStackTrace();
			usage();
		}
	}
	private static void usage() {
		System.out.println(
			"usage: -label labelsKey -learn learner -in inputLabel -split splitter");
		System.exit(-1);
	}
}

