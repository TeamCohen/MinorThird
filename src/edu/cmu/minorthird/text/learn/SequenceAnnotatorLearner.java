package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.util.ProgressCounter;
import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * Learn an annotation model using a sequence dataset.
 *
 * @author William Cohen
 */

public abstract class SequenceAnnotatorLearner implements AnnotatorLearner
{
	private static Logger log = Logger.getLogger(SequenceAnnotatorLearner.class);

	protected static double INSIDE_LABEL = +1;
	protected static double OUTSIDE_LABEL = -1;
	protected SpanFeatureExtractor fe;
	protected String annotationType = "_prediction";
	protected int historySize = 1;
	protected SequenceDataset seqData = new SequenceDataset();

	public SequenceAnnotatorLearner(SpanFeatureExtractor fe,int historySize)
	{
		this.fe = fe;
		this.historySize = historySize;
		seqData.setHistorySize(historySize);
	}

	public String getAnnotationType() { return annotationType; }

	public void setAnnotationType(String s) { annotationType=s; }

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
		TextLabels answerLabels = answeredQuery.labelTokensInsideOutside("insideOutside");
		Span document = answeredQuery.getDocumentSpan();
		Example[] sequence = new Example[document.size()];
		for (int i=0; i<document.size(); i++) {
			Token tok = document.getToken(i);
			String value = answerLabels.getProperty(tok,"insideOutside");
			if (AnnotationExample.INSIDE.equals(value)) {
				Span tokenSpan = document.subSpan(i,1);
				BinaryExample example = new BinaryExample( fe.extractInstance(answerLabels,tokenSpan), INSIDE_LABEL);
				sequence[i] = example;
			} else if (AnnotationExample.OUTSIDE.equals( value )) {
				Span tokenSpan = document.subSpan(i,1);
				BinaryExample example = new BinaryExample( fe.extractInstance(answerLabels,tokenSpan), OUTSIDE_LABEL);
				sequence[i] = example;
			} else {
				log.warn("some tokens not labeled in "+document.getDocumentId());
				return;
			}
		}
		seqData.addSequence( sequence );
	}

	/** Return the learned annotator.
	 */
	abstract public Annotator getAnnotator();

	/** Get the constructed sequence data.
	 */
	public SequenceDataset getSequenceDataset()
	{
		return seqData;
	}

	public static class SequenceAnnotator extends AbstractAnnotator implements Serializable
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
					if (inside) log.debug("inside: '"+s.getToken(j).getValue()+"'");
				}
				pc.progress();
			}
			for (Span.Looper j=mergeExpr.extract(labels, labels.getTextBase().documentSpanIterator()); j.hasNext(); ) {
				Span s = j.nextSpan();
				labels.addToType( s, annotationType );
				log.info(annotationType+": '"+s.asString()+"'");
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
	}
}
