package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.AnnotationExample;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.util.ProgressCounter;

/**
 * Learn a InsideOutsideAnnotator from examples.
 *
 * This is a no-history version of the SequenceAnnotatorLearner. 
 * @author William Cohen
 */

public class BatchInsideOutsideLearner implements AnnotatorLearner
{
	private static double INSIDE_LABEL = +1;
	private static double OUTSIDE_LABEL = -1;

	private SpanFeatureExtractor fe;
	private ClassifierLearner classifierLearner;
	private String annotationType = "_prediction";
	private static final boolean SAVE_DATA = true;
  private Dataset tmpData = SAVE_DATA ? new BasicDataset() : null;

	public BatchInsideOutsideLearner(SpanFeatureExtractor fe,ClassifierLearner classifierLearner)
	{
		this.fe = fe;
		this.classifierLearner = classifierLearner;
	}
	public String getAnnotationType() { return annotationType; }
	public void setAnnotationType(String s) { annotationType=s; }

	// temporary storage
	private Span.Looper documentLooper;

	public void reset() {
		classifierLearner.reset();
	}

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
		for (int i=0; i<document.size(); i++) {
			Token tok = document.getToken(i);
			String value = answerLabels.getProperty(tok,"insideOutside");
			if (AnnotationExample.INSIDE.equals(value)) {
				Span tokenSpan = document.subSpan(i,1);
				BinaryExample example = new BinaryExample( fe.extractInstance(answerLabels,tokenSpan), INSIDE_LABEL);
				tmpData.add( example );
				classifierLearner.addExample( example );				
			} else if (AnnotationExample.OUTSIDE.equals( value )) {
				Span tokenSpan = document.subSpan(i,1);
				BinaryExample example = new BinaryExample( fe.extractInstance(answerLabels,tokenSpan), OUTSIDE_LABEL);
				tmpData.add( example );
				classifierLearner.addExample( example );				
			}
		}
	}

  public SpanFeatureExtractor getSpanFeatureExtractor()  { return fe; }

	/** Return the learned annotator
	 */
	public Annotator getAnnotator()
	{
		if (SAVE_DATA) {
			try { 
				DatasetLoader.save(tmpData, new java.io.File("tmpData.txt") );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new InsideOutsideAnnotator( (BinaryClassifier)classifierLearner.getClassifier(), fe, annotationType );
	}

	private static class InsideOutsideAnnotator extends AbstractAnnotator
	{
		private BinaryClassifier classifier;
		private String annotationType;
		private SpanFeatureExtractor fe;
		// an expression to merge together adjacent tokens classified as "inside"
		private static Mixup mergeExpr;
		static {
			try {
				mergeExpr = new Mixup("...[L insideOutside:inside+ R]...");
			} catch (Mixup.ParseException e) {
				throw new IllegalStateException("static init error"+e);
			}
		}

		public InsideOutsideAnnotator( BinaryClassifier classifier, SpanFeatureExtractor fe, String annotationType ) {
			this.classifier = classifier;
			this.fe = fe;
			this.annotationType = annotationType;
		}

		protected void doAnnotate(MonotonicTextLabels labels) {
			Span.Looper i = labels.getTextBase().documentSpanIterator();
			//ProgressCounter pc = new ProgressCounter("annotating", "document", i.estimatedSize() );
			while (i.hasNext() ) {
				Span s = i.nextSpan();
				for (int j=0; j<s.size()-1; j++) {
					Span tokenSpan = s.subSpan(j,1);
					String predicted = classifier.score( fe.extractInstance(labels,tokenSpan) )>0 ? "inside" : "outside";
					labels.setProperty( tokenSpan.getToken(0), "insideOutside", predicted );
				}
			}
			for (Span.Looper j=mergeExpr.extract(labels, labels.getTextBase().documentSpanIterator()); j.hasNext(); ) {
				labels.addToType( j.nextSpan(), annotationType );
			}
		}
		public String explainAnnotation(TextLabels labels,Span documentSpan) {
			StringBuffer buf = new StringBuffer("");
			Span.Looper i = labels.getTextBase().documentSpanIterator();
			while (i.hasNext() ) {
				Span s = i.nextSpan();
				for (int j=0; j<s.size()-1; j++) {
					Span tokenSpan = s.subSpan(j,1);
					Instance instance = fe.extractInstance(labels,tokenSpan);
					double score = classifier.score( instance );
					buf.append("'"+tokenSpan.asString()+"' ==> "+score+"\n");
					buf.append(" --- "+classifier.explain(instance)+"\n");
				}
			}
			return buf.toString();
		}
		public String toString()
		{
			return "[InsideOutsideAnnotator "+annotationType+":\n"+classifier+"]"; 
		}
	}
}
