package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.util.ProgressCounter;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.border.*;
import java.io.Serializable;

/**
 * Learn an annotation model using a sequence dataset and a
 * BatchSequenceClassifierLearner.  This class reduces learning a
 * single type T to sequential classification of tokens as 'inside a
 * span of type T' or 'outside a span of type T'.
 *
 * @author William Cohen
 */

public class SequenceAnnotatorLearner implements AnnotatorLearner
{
	private static Logger log = Logger.getLogger(SequenceAnnotatorLearner.class);
	private static final boolean DEBUG = false;

	protected static double INSIDE_LABEL = +1;
	protected static double OUTSIDE_LABEL = -1;
	protected SpanFeatureExtractor fe;
	protected String annotationType = "_prediction";
	protected int historySize = 3;
	protected SequenceDataset seqData = new SequenceDataset();
	protected BatchSequenceClassifierLearner seqLearner;

	public SequenceAnnotatorLearner(BatchSequenceClassifierLearner seqLearner,SpanFeatureExtractor fe,int historySize)
	{
		this.seqLearner = seqLearner;
		this.fe = fe;
		this.historySize = historySize;
		seqData.setHistorySize(historySize);
	}

  /**
   * This constructor creates a sequence learner backed by a CollinsPerceptronLearner
   * and the SampleFE.ExtractionFE feature extractor.
   *
   * It was created to allow the wizard to get a 'default' instance of the class
   */
  public SequenceAnnotatorLearner()
  {
    fe = new SampleFE.ExtractionFE();
    seqLearner = new CollinsPerceptronLearner();
  }

	public void reset() {
		seqData = new SequenceDataset();
	}

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
		TextLabels oldAnswerLabels = answeredQuery.getLabels();
		TextLabels answerLabels = answeredQuery.labelTokensInsideOutside("insideOutside");
		Span document = answeredQuery.getDocumentSpan();
		Example[] sequence = new Example[document.size()];
		for (int i=0; i<document.size(); i++) {
			Token tok = document.getToken(i);
			String value = answerLabels.getProperty(tok,"insideOutside");
			if (AnnotationExample.INSIDE.equals(value)) {
				Span tokenSpan = document.subSpan(i,1);
				BinaryExample example = new BinaryExample( fe.extractInstance(oldAnswerLabels,tokenSpan), INSIDE_LABEL);
				sequence[i] = example;
			} else if (AnnotationExample.OUTSIDE.equals( value )) {
				Span tokenSpan = document.subSpan(i,1);
				BinaryExample example = new BinaryExample( fe.extractInstance(oldAnswerLabels,tokenSpan), OUTSIDE_LABEL);
				sequence[i] = example;
			} else {
				log.warn("token "+tok+" not labeled in "+document.getDocumentId()+" - ignoring test of document");
				//System.out.println("input type "+answeredQuery.getInputType());
				return;
			}
		}
		seqData.addSequence( sequence );
	}

	/** Return the learned annotator.
	 */
	public Annotator getAnnotator()
	{
		seqLearner.setSchema(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
		SequenceClassifier seqClassifier = seqLearner.batchTrain(seqData);
		if (DEBUG) log.debug("learned classifier: "+seqClassifier);
		return new SequenceAnnotatorLearner.SequenceAnnotator( seqClassifier, fe, annotationType );
	}

	/** Get the constructed sequence data.
	 */
	public SequenceDataset getSequenceDataset()
	{
		return seqData;
	}

	public SpanFeatureExtractor getSpanFeatureExtractor()
	{
		return fe;
	}

	public void setSpanFeatureExtractor(SpanFeatureExtractor fe)
	{
		this.fe = fe;
	}

	public static class SequenceAnnotator extends AbstractAnnotator implements Serializable,Visible
	{
		private static final long serialVersionUID = 1;
		private static Mixup mergeExpr;
		static {
			try {
				mergeExpr = new Mixup("...[L insideOutside:inside+ R]...");
			} catch (Mixup.ParseException e) {
				throw new IllegalStateException("static init error"+e);
			}
		}
		private SequenceClassifier seqClassifier;
		private String annotationType;
		private SpanFeatureExtractor fe;

		public SequenceAnnotator( SequenceClassifier seqClassifier, SpanFeatureExtractor fe, String annotationType )
		{
			this.seqClassifier = seqClassifier;
			this.fe = fe;
			this.annotationType = annotationType;
		}

		protected void doAnnotate(MonotonicTextLabels labels)
		{
			Span.Looper i = labels.getTextBase().documentSpanIterator();
			ProgressCounter pc = new ProgressCounter("annotating", "document", i.estimatedSize() );
			while (i.hasNext() ) {
				Span s = i.nextSpan();
				Instance[] sequence = new Instance[s.size()];
				for (int j=0; j<s.size(); j++) {
					Span tokenSpan = s.subSpan(j,1);
					sequence[j] = fe.extractInstance(labels,tokenSpan);
				}
				ClassLabel[] classLabels = seqClassifier.classification( sequence );
				for (int j=0; j<classLabels.length; j++) {
					boolean inside = classLabels[j].isPositive();
					labels.setProperty( s.getToken(j), "insideOutside", (inside?"inside":"outside") );
					if (DEBUG && inside) log.debug("inside: '"+s.getToken(j).getValue()+"'");
				}
				pc.progress();
			}
			for (Span.Looper j=mergeExpr.extract(labels, labels.getTextBase().documentSpanIterator()); j.hasNext(); ) {
				Span s = j.nextSpan();
				labels.addToType( s, annotationType );
				if (DEBUG) log.debug(annotationType+": '"+s.asString()+"'");
			}
			pc.finished();
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
}
