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

/** Run a text-classification learning experiment based on pre-labeled text.
 *
 * @author William Cohen
*/

public class ClassifyExperiment
{
  private static Logger log = Logger.getLogger(ClassifyExperiment.class);
	private SampleFE.AnnotatedSpanFE fe = null;

	/** Subclass this to use a different feature extractor in the main
	 */
	protected SampleFE.AnnotatedSpanFE createFE() { return SampleFE.BAG_OF_LC_WORDS; }

	private TextLabels labels, testLabels;
	private Splitter splitter;
	private ClassifierLearner learner;
	private String inputLabel;

	/**
	 * Default constructor.
	 */
	public ClassifyExperiment() {;}

  /**
   * @param labels The labels and base to be annotated in the example
   *               These are the training examples
   * @param splitter splitter for the documents in the labels to create test vs. train
   * @param testLabels if non-null, ignore splitter and used a FixedTestSetSplitter
   * @param learnerName ClassifierLearner algorithm object to use
   * @param inputLabel spanType in the TextLabels to treat as 'positive'.
   */
	public ClassifyExperiment(
		TextLabels labels,Splitter splitter,String learnerName,String inputLabel,TextLabels testLabels) 
	{
		this.labels = labels;
		this.splitter = splitter;
		this.testLabels = testLabels;
		this.inputLabel = inputLabel;
		this.learner = Expt.toLearner(learnerName);
	}

	public ClassifyExperiment(
		TextLabels labels,Splitter splitter,ClassifierLearner learner,String inputLabel,TextLabels testLabels)
	{
		this.labels = labels;
		this.splitter = splitter;
		this.inputLabel = inputLabel;
		this.learner = learner;
		this.testLabels = testLabels;
	}

	public void setFE(SampleFE.AnnotatedSpanFE fe) 
	{ 
		this.fe = fe; 
		System.out.println("fe = "+fe);
	}
	public SampleFE.AnnotatedSpanFE getFE() { return fe; 	}


	private Dataset toDataset() 
	{
    return toDataset(labels, fe, inputLabel);
  }

  private Dataset toDataset(TextLabels textLabels, SpanFeatureExtractor featureExtractor,String inLabel)
  {
		if (textLabels.getTypes().contains(inLabel)) {
			Dataset dataset = new BasicDataset();
			for (Span.Looper i=textLabels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span s = i.nextSpan();
				double classLabel = textLabels.hasType(s,inLabel) ? +1 : -1;
				dataset.add( new BinaryExample( featureExtractor.extractInstance(textLabels,s), classLabel) );
			}
			return dataset;
		} else if (textLabels.getSpanProperties().contains(inLabel)) {
			Dataset dataset = new BasicDataset();
			for (Span.Looper i=textLabels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span s = i.nextSpan();
				String className = textLabels.getProperty(s,inLabel);
				if (className==null) {
					log.warn("no span property "+inLabel+" for document "+s.getDocumentId()+" - will be ignored");
				} else {
					dataset.add( new Example( featureExtractor.extractInstance(textLabels,s), new ClassLabel(className)) );
				}
			}
			return dataset;
		} else {
			throw new IllegalArgumentException("no span type or property '"+inLabel+"' found");
		}
  }

	public Evaluation evaluation()
	{
		if (testLabels!=null) {
			Dataset testData = toDataset(testLabels, fe, inputLabel);
			splitter = new FixedTestSetSplitter(testData.iterator());
		}
		return Tester.evaluate( learner, toDataset(), splitter );
	}

	public CrossValidatedDataset crossValidatedDataset() 
	{
		return new CrossValidatedDataset( learner, toDataset(), splitter );
	}

	public static void main(String[] args) 
	{
		new ClassifyExperiment().doMain(args);
	}

	protected void doMain(String[] args) 
	{
		Splitter splitter=null; //new RandomSplitter(0.7);
		String learnerName="new BatchVersion(new VotedPerceptron())";
		TextLabels labels=null;
    TextLabels testLabels=null;
		String inputLabel=null;
		String show=null;
		String annotationNeeded=null;
		try {
			int pos = 0;
			while (pos<args.length) {
				String opt = args[pos++];
				if (opt.startsWith("-lab")) {
					labels = FancyLoader.loadTextLabels(args[pos++]);
        } else if (opt.startsWith("-te")) {
          if (splitter!=null) throw new IllegalArgumentException("only one of splitter, testData allowed");
          testLabels = FancyLoader.loadTextLabels(args[pos++]);
				} else if (opt.startsWith("-split")) {
          if (testLabels !=null) throw new IllegalArgumentException("only one of splitter, testData allowed");
          splitter = Expt.toSplitter(args[pos++]);
				} else if (opt.startsWith("-lea")) {
					learnerName = args[pos++];
				} else if (opt.startsWith("-in")) {
					inputLabel = args[pos++];
				} else if (opt.startsWith("-show")) {
					show = args[pos++];
				} else if (opt.startsWith("-mix")) {
					annotationNeeded = args[pos++];
				} else {
					usage();
				}
			}
			if (splitter==null) splitter = new RandomSplitter(0.70);

			if (labels==null || learnerName==null || inputLabel==null) 
			{
				usage();
			}

      log.info("splitter: " + splitter);
      ClassifyExperiment expt = new ClassifyExperiment(labels,splitter,learnerName,inputLabel,testLabels);
			expt.setFE( this.createFE() );
			if (annotationNeeded!=null) {
				expt.getFE().setRequiredAnnotation(annotationNeeded);
				expt.getFE().setAnnotationProvider(annotationNeeded+".mixup");
				labels.require(annotationNeeded,annotationNeeded+".mixup");
			}
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
	protected static void usage() {
		System.out.println(
			"usage: -label labelsKey -learn learner -in inputLabel [-split splitter -test testLabels -show all]");
		System.exit(-1);
	}


}

