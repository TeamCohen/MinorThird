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
 * Learn an annotation model using a sequence dataset and a
 * BatchSequenceClassifierLearner.  This class reduces extraction
 * learning to sequential classification of tokens.  The scheme for
 * mapping extraction learning to token learning is determined by the
 * Extraction2TaggingReduction.
 *
 * @author William Cohen
 */

public class SequenceAnnotatorLearner implements AnnotatorLearner
{
	private static Logger log = Logger.getLogger(SequenceAnnotatorLearner.class);
	private static final boolean DEBUG = false;

	protected SpanFeatureExtractor fe;
	protected String annotationType = "_prediction";
	protected int historySize = 3;
	protected SequenceDataset seqData = new SequenceDataset();
	protected BatchSequenceClassifierLearner seqLearner;
	protected Extraction2TaggingReduction reduction = new InsideOutsideReduction();

	public SequenceAnnotatorLearner()
	{
		this(new CollinsPerceptronLearner(),new Recommended.TokenFE());
	}
	public SequenceAnnotatorLearner(BatchSequenceClassifierLearner seqLearner,SpanFeatureExtractor fe)
	{
		this(seqLearner,fe,new InsideOutsideReduction());
	}
	public SequenceAnnotatorLearner(
		BatchSequenceClassifierLearner seqLearner,SpanFeatureExtractor fe,Extraction2TaggingReduction reduction)
	{
		this.seqLearner = seqLearner;
		this.reduction = reduction;
		this.fe = fe;
		this.historySize = seqLearner.getHistorySize();
		seqData.setHistorySize(this.historySize);
	}

	public void reset() {
		seqData = new SequenceDataset();
	}

	//
	// getters and setters
	//
	private boolean displayDatasetBeforeLearning=false;

	/** If set, try and pop up an interactive viewer of the sequential dataset before learning. */
	public boolean getDisplayDatasetBeforeLearning() {
		return displayDatasetBeforeLearning; 
	}
	public void setDisplayDatasetBeforeLearning(boolean newDisplayDatasetBeforeLearning) {
		this.displayDatasetBeforeLearning = newDisplayDatasetBeforeLearning;
	}
	public int getHistorySize() { return historySize; }
	public void setHistorySize(int historySize) { this.historySize=historySize; }
	public BatchSequenceClassifierLearner getSequenceClassifierLearner() { return seqLearner; }
	public void setSequenceClassifierLearner(BatchSequenceClassifierLearner learner) { this.seqLearner=learner; }
	public Extraction2TaggingReduction getTaggingReduction() { return reduction; }
	public void setTaggingReduction(Extraction2TaggingReduction reduction) { this.reduction = reduction; }
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
	public Annotator getAnnotator()
	{
		ExampleSchema schema = seqData.getSchema();
		if (schema.getNumberOfClasses()<=1) {
			log.error("In the constructed dataset the number of classes is "+schema.getNumberOfClasses());
			log.error("Hint: this probably means that no spans of the specified type are present in your data");
		}
		seqLearner.setSchema(schema);
		if (displayDatasetBeforeLearning) new ViewerFrame("Sequential Dataset", seqData.toGUI());
		SequenceClassifier seqClassifier = seqLearner.batchTrain(seqData);
		if (DEBUG) log.debug("learned classifier: "+seqClassifier);
		return new SequenceAnnotatorLearner.SequenceAnnotator( seqClassifier, fe, reduction, annotationType );
	}

	/** Get the constructed sequence data.
	 */
	public SequenceDataset getSequenceDataset()
	{
		return seqData;
	}

	/**
	 * A useful subroutine - prepare sequence data the way a SequenceAnnotatorLearner would prepare it
	 * when trained by a TextLabelsAnnotatorTeacher.
	 *
	 */
	static public SequenceDataset prepareSequenceData(
		TextLabels labels,String spanType, String spanProp,
		SpanFeatureExtractor fe,final int historySize,Extraction2TaggingReduction reduction)
	{
		BatchSequenceClassifierLearner dummy1 = new BatchSequenceClassifierLearner() {
				public void setSchema(ExampleSchema schema) {}
				public SequenceClassifier batchTrain(SequenceDataset dataset) {return null;}
				public int getHistorySize() { return historySize; }
			};
		SequenceAnnotatorLearner dummy2 = new SequenceAnnotatorLearner(dummy1,fe,reduction) {
				public Annotator getAnnotator() { return null; }
			};
		new TextLabelsAnnotatorTeacher(labels,spanType,spanProp).train(dummy2);
		return dummy2.getSequenceDataset();
	}

	//
	// learned annotator
	//

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
}
