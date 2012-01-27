package edu.cmu.minorthird.text.learn;

import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.Token;
import edu.cmu.minorthird.ui.Recommended;

/**
 * Learn an annotation model using a sequence dataset and some sort of
 * batch learner.
 *
 * @author William Cohen
 */

public abstract class AbstractBatchAnnotatorLearner extends AnnotatorLearner
{
	private static Logger log = Logger.getLogger(AbstractBatchAnnotatorLearner.class);

	protected SpanFeatureExtractor fe;
	protected String annotationType = "_prediction";
	protected SequenceDataset seqData;
	protected Extraction2TaggingReduction reduction;

	public AbstractBatchAnnotatorLearner() {
		this(new Recommended.TokenFE(),new InsideOutsideReduction());
	}
	public AbstractBatchAnnotatorLearner(SpanFeatureExtractor fe,Extraction2TaggingReduction reduction) {
		this.reduction = reduction;
		this.fe = fe;
		seqData = new SequenceDataset();
	}

	@Override
	public void reset() { 
		seqData = new SequenceDataset(); 
	}


	/** Scheme for reducing extraction to a token-classification problem */
	public Extraction2TaggingReduction getTaggingReduction() { return reduction; }
	public void setTaggingReduction(Extraction2TaggingReduction reduction) { this.reduction = reduction; }

	public String getTaggingReductionHelp() { return "Scheme for reducing extraction to a token-classification problem"; }

	/** Feature extractor used for tokens */
	@Override
	public SpanFeatureExtractor getSpanFeatureExtractor()	{	return fe; }
	@Override
	public void setSpanFeatureExtractor(SpanFeatureExtractor fe) {this.fe = fe;	}

	/** The spanType of the annotation produced by the learned annotator. */
	@Override
	public void setAnnotationType(String s) { annotationType=s; }
	@Override
	public String getAnnotationType() { return annotationType; }

	//
	// buffer data
	//

	// temporary storage
	private Iterator<Span> documentLooper;

	/** Accept the pool of unlabeled documents. */
	@Override
	public void setDocumentPool(Iterator<Span> documentLooper) { this.documentLooper = documentLooper; }

	/** Ask for labels on every document. */
	@Override
	public boolean hasNextQuery() {	return documentLooper.hasNext();}

	/** Return the next unlabeled document. */
	@Override
	public Span nextQuery() {	return documentLooper.next();	}

	/** Accept the answer to the last query. */
	@Override
	public void setAnswer(AnnotationExample answeredQuery){
		reduction.reduceExtraction2Tagging(answeredQuery);
		TextLabels answerLabels=reduction.getTaggedLabels();
		Span document=answeredQuery.getDocumentSpan();
		Example[] sequence=new Example[document.size()];
		for (int i=0; i<document.size(); i++) {
			Token tok=document.getToken(i);
			String value=answerLabels.getProperty(tok,reduction.getTokenProp());
			if (value!=null) {
				ClassLabel classLabel = new ClassLabel(value);
				Span tokenSpan = document.subSpan(i,1);
				Example example = new Example(fe.extractInstance(answeredQuery.getLabels(),tokenSpan), classLabel);
				sequence[i] = example;
			} else {
				log.warn("ignoring "+document.getDocumentId()+" because token "+i+" is not labeled");
				return;
			}
		}
		seqData.addSequence(sequence);
	}

	/** Return the learned annotator.
	 */
	@Override
	abstract public Annotator getAnnotator();

	/** Get the constructed sequence data.
	 */
	public SequenceDataset getSequenceDataset()
	{
		return seqData;
	}
}
