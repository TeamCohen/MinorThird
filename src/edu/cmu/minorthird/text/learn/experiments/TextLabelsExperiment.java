package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.experiments.Expt;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.AnnotatorTeacher;
import edu.cmu.minorthird.text.learn.TextLabelsAnnotatorTeacher;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/** Run an annotation-learning experiment based on a pre-labeled
 * text .
 *
 * @author William Cohen
*/

public class TextLabelsExperiment
{
	private static final boolean SHOW_CURVE = false;

	private TextLabels labels;
	private Splitter splitter;
	private AnnotatorLearner learner;
	private String inputLabel;
	private MonotonicTextLabels fullTestLabels;
	private String outputLabel;
	private Annotator[] annotators;
  private static Logger log = Logger.getLogger(TextLabelsExperiment.class);

  /**
   * NB: William, this desperately needs some javadoc
   * @param labels The labels and base to be annotated in the example
   *               These are the training examples
   * @param splitter Splitter on the documents in the labels to create test vs. train
   * @param learner AnnotatorLearner algorithm object to use
   * @param inputLabel
   * @param outputLabel
   */
	public TextLabelsExperiment(TextLabels labels,Splitter splitter,AnnotatorLearner learner,String inputLabel,String outputLabel) {
		this.labels = labels;
		this.splitter = splitter;
		this.learner = learner;
		this.inputLabel = inputLabel;
		this.outputLabel = outputLabel;
		learner.setAnnotationType( outputLabel );
	}

	public void doExperiment() 
	{
		splitter.split( labels.getTextBase().documentSpanIterator() );

		annotators = new Annotator[ splitter.getNumPartitions() ];
		Set allTestDocuments = new TreeSet();
		for (int i=0; i<splitter.getNumPartitions(); i++) {
			for (Iterator j=splitter.getTest(i); j.hasNext(); ) 
				allTestDocuments.add( j.next() );
		}

		SubTextBase fullTestBase = new SubTextBase( labels.getTextBase(), allTestDocuments.iterator() );
		fullTestLabels = new NestedTextLabels( new SubTextLabels( fullTestBase, labels ) );

		for (int i=0; i<splitter.getNumPartitions(); i++) {
			log.info("For partition "+(i+1)+" of "+splitter.getNumPartitions());
			log.info("Creating teacher and train partition...");

			SubTextBase trainBase = new SubTextBase( labels.getTextBase(), splitter.getTrain(i) );
			SubTextLabels trainLabels = new SubTextLabels( trainBase, labels );
			AnnotatorTeacher teacher = new TextLabelsAnnotatorTeacher( trainLabels, inputLabel );

      log.info("Training annotator...");
			annotators[i] = teacher.train( learner );

      log.info("annotators["+i+"]="+annotators[i]);
			log.info("Creating test partition...");
			SubTextBase testBase = new SubTextBase( labels.getTextBase(), splitter.getTest(i) );
			for (Iterator j=splitter.getTest(i); j.hasNext(); )
      { allTestDocuments.add( j.next() ); }
			MonotonicTextLabels ithTestLabels = new MonotonicSubTextLabels( testBase, fullTestLabels );

      log.info("Labeling test partition...");
			annotators[i].annotate( ithTestLabels );

			log.info("Evaluating test partition...");
			measurePrecisionRecall( ithTestLabels );
		}
		log.info("Overall performance measure");
		measurePrecisionRecall( fullTestLabels );
	}

	private void measurePrecisionRecall(TextLabels labels)
	{
		SpanDifference sd =
			new SpanDifference( labels.instanceIterator(outputLabel),
													labels.instanceIterator(inputLabel),
													labels.closureIterator(inputLabel) );
		System.out.println("Precision: "+sd.tokenPrecision()+" Recall: "+sd.tokenRecall());
		//
		// build a precision-recall curve
		//
		if (SHOW_CURVE) {
			TreeSet predictions = new TreeSet();
			int totalPosTokens = 0;
			for (SpanDifference.Looper i = sd.differenceIterator(); i.hasNext(); ) {
				Span s = i.nextSpan();
				Span g = i.getOriginalGuessSpan();
				int status = i.getStatus();
				if (g==null && status!=SpanDifference.FALSE_NEG) {
					System.out.println("g = "+g+" for span "+s+" status="+status);
				}
				Details d = g==null ? null : labels.getDetails( g, outputLabel);
				double conf = (d==null) ? 1.0 : d.getConfidence();
				predictions.add( new PredictionKey(s, status, conf) );
				if (status==SpanDifference.FALSE_NEG || status==SpanDifference.TRUE_POS)
					totalPosTokens += s.size();
			}
			double precision=1.0, recall=0;
			double[] count = new double[SpanDifference.MAX_STATUS];
			String[] status = new String[SpanDifference.MAX_STATUS];
			status[SpanDifference.TRUE_POS] = "tp";
			status[SpanDifference.FALSE_POS] = "fp";
			status[SpanDifference.FALSE_NEG] = "fn";
			System.out.println("#recall\tprec\tscore\tstat\tspan");
			for (Iterator i=predictions.iterator(); i.hasNext(); ) {
				PredictionKey k = (PredictionKey) i.next();
				count[ k.status ] += k.s.size();
				if (totalPosTokens > 0) {
					recall = count[SpanDifference.TRUE_POS] / totalPosTokens;
				}
				if (count[SpanDifference.TRUE_POS]+count[SpanDifference.FALSE_POS] > 0 ) {
					precision = count[SpanDifference.TRUE_POS]/
											(count[SpanDifference.TRUE_POS]+count[SpanDifference.FALSE_POS]);
				}
			//System.out.println("tp: "+count[SpanDifference.TRUE_POS]+" fp: "+count[SpanDifference.FALSE_POS]
			//+" fn: "+count[SpanDifference.FALSE_NEG]);
				System.out.println(recall+"\t"+precision+"\t"+k.confidence+"\t"+status[k.status]+" "+k.s);
			}
		}
	}
	private class PredictionKey implements Comparable {
		public Span s;
		int status;
		double confidence;
		public PredictionKey(Span s, int status, double confidence) {
			this.s = s; this.status = status; this.confidence = confidence;
		}
		public int compareTo(Object o) {
			double score = ((PredictionKey)o).confidence - confidence;
			return score>0 ? +1 : (score<0? -1 : ((PredictionKey)o).s.compareTo(s) );
		}
	}

	public TextLabels getTestLabels() { return fullTestLabels; }

	public static void main(String[] args) 
	{
		TextLabels labels=null;
		Splitter splitter=new RandomSplitter();
		AnnotatorLearner learner=null;
		String inputLabel=null, outputLabel="guess", saveFileName=null;
		try {
			int pos = 0;
			while (pos<args.length) {
				String opt = args[pos++];
				if (opt.startsWith("-lab")) {
					labels = FancyLoader.loadTextLabels(args[pos++]);
				} else if (opt.startsWith("-s")) {
					splitter = Expt.toSplitter(args[pos++]);
				} else if (opt.startsWith("-lea")) {
					learner = FancyLoader.loadAnnotatorLearner(args[pos++]);
				} else if (opt.startsWith("-i")) {
					inputLabel = args[pos++];
				} else if (opt.startsWith("-o")) {
					outputLabel = args[pos++];
				} else if (opt.startsWith("-f")) {
					saveFileName = args[pos++];
				} else {
					usage();
				}
			}
			if (labels==null || learner==null || splitter==null|| inputLabel==null || outputLabel==null) usage();

      TextLabelsExperiment expt = new TextLabelsExperiment(labels,splitter,learner,inputLabel,outputLabel);
			expt.doExperiment();
			if (saveFileName!=null) {
				new TextLabelsLoader().saveTypesAsOps( expt.getTestLabels(), new File(saveFileName) );
			}
			TextBaseViewer.view( expt.getTestLabels() );
		} catch (Exception e) {
			e.printStackTrace();
			usage();
		}
	}
	private static void usage() {
		System.out.println("usage: -label labelsKey learnerKey -i inputLabel -o outputLabel [-s splitter -l] [-f saveFile]");
	}
}

