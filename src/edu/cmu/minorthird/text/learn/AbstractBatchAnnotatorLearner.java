package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.ui.*;
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
 * Learn an annotation model using a sequence dataset and some sort of
 * batch learner.
 *
 * @author William Cohen
 */

public abstract class AbstractBatchAnnotatorLearner implements AnnotatorLearner
{
	private static Logger log = Logger.getLogger(AbstractBatchAnnotatorLearner.class);
	private static final boolean DEBUG = false;

	protected SpanFeatureExtractor fe;
	protected String annotationType = "_prediction";
	protected int historySize;
	protected SequenceDataset seqData;
	protected Extraction2TaggingReduction reduction;

	public AbstractBatchAnnotatorLearner()
	{
		this(3,new Recommended.TokenFE(),new InsideOutsideReduction());
	}
	public AbstractBatchAnnotatorLearner(int history,SpanFeatureExtractor fe,Extraction2TaggingReduction reduction)
	{
    this.historySize = history;
		this.reduction = reduction;
		this.fe = fe;
    seqData = new SequenceDataset();
    seqData.setHistorySize(historySize);
	}

	public void reset() {	seqData = new SequenceDataset(); seqData.setHistorySize(historySize); }

  /** History to remember, for a sequential learner */
	public int getHistorySize() { return historySize; }
	public void setHistorySize(int historySize) { this.historySize=historySize; }

  /** Scheme for reducing extraction to a token-classification problem */
	public Extraction2TaggingReduction getTaggingReduction() { return reduction; }
	public void setTaggingReduction(Extraction2TaggingReduction reduction) { this.reduction = reduction; }

  /** Feature extractor used for tokens */
	public SpanFeatureExtractor getSpanFeatureExtractor()	{	return fe; }
	public void setSpanFeatureExtractor(SpanFeatureExtractor fe) {this.fe = fe;	}

	/** The spanType of the annotation produced by the learned annotator. */
	public void setAnnotationType(String s) { annotationType=s; }
	public String getAnnotationType() { return annotationType; }

  //
  // buffer data
  //

	// temporary storage
	private Span.Looper documentLooper;

	/** Accept the pool of unlabeled documents. */
	public void setDocumentPool(Span.Looper documentLooper) {	this.documentLooper = documentLooper; }

	/** Ask for labels on every document. */
	public boolean hasNextQuery() {	return documentLooper.hasNext();}

	/** Return the next unlabeled document. */
	public Span nextQuery() {	return documentLooper.nextSpan();	}

	/** Accept the answer to the last query. */
	public void setAnswer(AnnotationExample answeredQuery)
	{
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
}
