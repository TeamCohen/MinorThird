package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.util.ProgressCounter;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.border.*;
import java.io.Serializable;

/**
 * Learn an annotation model using a SlidingWindowDataset dataset and a
 * BatchSequenceClassifierLearner.  
 *
 * @author William Cohen
 */

public class SlidingWindowAnnotatorLearner implements AnnotatorLearner
{
	private static Logger log = Logger.getLogger(SlidingWindowAnnotatorLearner.class);
	private static final boolean DEBUG = false;

	protected String annotationType = "_prediction";
	protected SlidingWindowDataset dataset = new SlidingWindowDataset();
	protected BatchSequenceClassifierLearner learner;
	protected SpanFeatureExtractor fe;
	protected int windowSize;

	public SlidingWindowAnnotatorLearner(BatchSequenceClassifierLearner learner,SpanFeatureExtractor fe,int windowSize)
	{
		this.learner = learner;
		this.fe = fe;
		this.windowSize = windowSize;
	}
	public SlidingWindowAnnotatorLearner(BatchSequenceClassifierLearner learner,SpanFeatureExtractor fe)
	{
		this(learner,fe,4);
	}

	public void reset() 
	{
		dataset = new SlidingWindowDataset();
	}

	//
	// getters and setters
	//
	private boolean displayDatasetBeforeLearning=false;

	/** If set, try and pop up an interactive viewer of the sequential dataset before learning. */
	public boolean getDisplayDatasetBeforeLearning() { return displayDatasetBeforeLearning; }
	public void setDisplayDatasetBeforeLearning(boolean newDisplayDatasetBeforeLearning) {
		this.displayDatasetBeforeLearning = newDisplayDatasetBeforeLearning;
	}
	public int getHistorySize() { return 1; }
	public BatchSequenceClassifierLearner getSequenceClassifierLearner() { return learner; }
	public void setSequenceClassifierLearner(BatchSequenceClassifierLearner learner) { this.learner=learner; }
	public SpanFeatureExtractor getSpanFeatureExtractor()	{	return fe; }
	public void setSpanFeatureExtractor(SpanFeatureExtractor fe) {this.fe = fe;	}
	/** Specify the type of annotation produced by this annotator - that is, the
	 * type associated with spans produced by it. */
	public void setAnnotationType(String s) { annotationType=s; }
	public String getAnnotationType() { return annotationType; }

	// temporary storage
	private Span.Looper documentLooper;

	/** Accept the pool of unlabeled documents. */
	public void setDocumentPool(Span.Looper documentLooper) {
		this.documentLooper = documentLooper;
	}

	/** Ask for labels on every document. */
	public boolean hasNextQuery() {
		return documentLooper.hasNext();
	}

	/** Return the next unlabeled document. */
	public Span nextQuery() {
		return documentLooper.nextSpan();
	}

	/** Accept the answer to the last query. */
	public void setAnswer(AnnotationExample answeredQuery)
	{
		/*
		reduction.reduceExtraction2Tagging(answeredQuery);
		TextLabels answerLabels = reduction.getTaggedLabels();
		Span document = answeredQuery.getDocumentSpan();
		Example[] sequence = new Example[document.size()];
		for (int i=0; i<document.size(); i++) {
			Token tok = document.getToken(i);
			String value = answerLabels.getProperty(tok,reduction.getTokenProp());
			if (value!=null) {
				ClassLabel classLabel = new ClassLabel(value);
				Span tokenSpan = document.subSpan(i,1);
				Example example = new Example(fe.extractInstance(answeredQuery.getLabels(),tokenSpan), classLabel);
				sequence[i] = example;
			} else {
				log.warn("ignoring "+document.getDocumentId()+" because token "+i+" not labeled in "+document);
				return;
			}
		}
		seqData.addSequence( sequence );
		*/
	}

	/** Return the learned annotator.
	 */
	public Annotator getAnnotator()
	{
		/*
		learner.setSchema(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
		if (displayDatasetBeforeLearning) new ViewerFrame("Sequential Dataset", seqData.toGUI());
		SequenceClassifier seqClassifier = learner.batchTrain(seqData);
		if (DEBUG) log.debug("learned classifier: "+seqClassifier);
		return new SlidingWindowAnnotatorLearner.SequenceAnnotator( seqClassifier, fe, reduction, annotationType );
		*/
		return null;
	}

	//
	// learned annotator
	//

	/*
	public static class SequenceAnnotator extends AbstractAnnotator implements Serializable,Visible,ExtractorAnnotator
	{
		private static final long serialVersionUID = 2;
		private SequenceClassifier seqClassifier;
		private SpanFeatureExtractor fe;
		private Extraction2TaggingReduction reduction;
		private String annotationType;

		public SequenceAnnotator(SequenceClassifier seqClassifier,SpanFeatureExtractor fe,String annotationType )
		{
			this(seqClassifier,fe,new InsideOutsideReduction(),annotationType);
		}

		public SequenceAnnotator(
			SequenceClassifier seqClassifier,
			SpanFeatureExtractor fe,
			Extraction2TaggingReduction reduction,
			String annotationType )
		{
			this.seqClassifier = seqClassifier;
			this.fe = fe;
			this.reduction = reduction;
			this.annotationType = annotationType;
		}

		public String getSpanType() { return annotationType; }

		protected void doAnnotate(MonotonicTextLabels labels)
		{
			Span.Looper i = labels.getTextBase().documentSpanIterator();
			ProgressCounter pc = new ProgressCounter("tagging with classifier", "document", i.estimatedSize() );
			while (i.hasNext() ) {
				Span s = i.nextSpan();
				Instance[] sequence = new Instance[s.size()];
				for (int j=0; j<s.size(); j++) {
					Span tokenSpan = s.subSpan(j,1);
					sequence[j] = fe.extractInstance(labels,tokenSpan);
				}
				ClassLabel[] classLabels = seqClassifier.classification( sequence );
				for (int j=0; j<classLabels.length; j++) {
					labels.setProperty( s.getToken(j), reduction.getTokenProp(), classLabels[j].bestClassName() );
				}
				pc.progress();
			}
			pc.finished();
			reduction.extractFromTags( annotationType, labels );
		}
		public String explainAnnotation(TextLabels labels,Span documentSpan)
		{
			return "not implemented";
		}
		public String toString()
		{
			return "[SequenceAnnotator "+annotationType+":\n"+seqClassifier+"]";
		}
		public Viewer toGUI()
		{
			Viewer v = new ComponentViewer() {
					public JComponent componentFor(Object o) {
						SequenceAnnotator sa = (SequenceAnnotator)o;
						JPanel mainPanel = new JPanel();
						mainPanel.setBorder(new TitledBorder("Sequence Annotator"));
						Viewer subView = new SmartVanillaViewer(sa.seqClassifier);
						subView.setSuperView(this);
						mainPanel.add(subView);
						return new JScrollPane(mainPanel);
					}
				};
			v.setContent(this);
			return v;
		}
		
	}
	*/
}
