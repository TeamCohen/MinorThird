package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.ui.*;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.util.ProgressCounter;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.border.*;
import java.io.Serializable;
import java.util.*;

/**
 * Learn an annotation model using a SegmentDataset dataset and a
 * BatchSequenceClassifierLearner.  
 *
 * @author William Cohen
 */

public class SegmentAnnotatorLearner implements AnnotatorLearner
{
	private static Logger log = Logger.getLogger(SegmentAnnotatorLearner.class);
	private static final boolean DEBUG = log.isDebugEnabled();

	protected String annotationType = "_prediction";
	protected SegmentDataset dataset;
	protected BatchSegmenterLearner learner;
	protected SpanFeatureExtractor fe;
	protected int maxWindowSize;

	public SegmentAnnotatorLearner()
	{
		this(new SegmentCollinsPerceptronLearner(),new Recommended.MultitokenSpanFE());
	}
	public SegmentAnnotatorLearner(BatchSegmenterLearner learner,SpanFeatureExtractor fe)
	{
		this(learner,fe,4);
	}
	public SegmentAnnotatorLearner(BatchSegmenterLearner learner,SpanFeatureExtractor fe,int windowSize)
	{
		this.learner = learner;
		this.fe = fe;
		this.maxWindowSize = windowSize;
		reset();
	}
	public void reset() 
	{
		dataset = new SegmentDataset();
    dataset.setDataCompression(compressDataset);
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

  private boolean compressDataset=false; 

  /** If set, try and compress the data. This leads to longer loading and learning times
   * but less memory usage. */
  public boolean getCompressDataset() { return compressDataset; } 
  public void setCompressDataset(boolean flag) { compressDataset=flag; }

	public int getHistorySize() { return 1; }
	public BatchSegmenterLearner getSemiMarkovLearner() { return learner; }
	public void setSemiMarkovLearner(BatchSegmenterLearner learner) { this.learner=learner; }
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
		// add examples to the SegmentDataset
		Span document = answeredQuery.getDocumentSpan();
		MutableCandidateSegmentGroup g = new MutableCandidateSegmentGroup(maxWindowSize, document.size());
		for (int lo=0; lo<document.size(); lo++) {
			for (int len=1; len<=maxWindowSize; len++) {
				if (len+lo<=document.size()) {
					Span span = document.subSpan(lo,len);
					Instance instance = fe.extractInstance(answeredQuery.getLabels(),span);
					ClassLabel label = new ClassLabel( answeredQuery.getClassName(span) );
					g.setSubsequence(lo,lo+len,instance,label);
				}
			}
		}
		dataset.addCandidateSegmentGroup(g);
	}

	/** Return the learned annotator.
	 */
	public Annotator getAnnotator()
	{
		learner.setSchema(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
		if (displayDatasetBeforeLearning) new ViewerFrame("Sequential Dataset", dataset.toGUI());
		Segmenter segmenter = learner.batchTrain(dataset);
		if (DEBUG) log.debug("learned segmenter: "+segmenter);
		return new SegmentAnnotator(segmenter, fe, maxWindowSize, annotationType);
	}

	//
	// learned annotator
	//

	public static class SegmentAnnotator extends AbstractAnnotator implements Serializable,Visible,ExtractorAnnotator
	{
		private static final long serialVersionUID = 1;
		private Segmenter segmenter;
		private SpanFeatureExtractor fe;
		private String annotationType;
		private int maxWindowSize;

		public SegmentAnnotator(
			Segmenter segmenter,SpanFeatureExtractor fe,int maxWindowSize,String annotationType)
		{
			this.segmenter = segmenter;
			this.fe = fe;
			this.maxWindowSize = maxWindowSize;
			this.annotationType = annotationType;
		}

		public String getSpanType() { return annotationType; }

		protected void doAnnotate(MonotonicTextLabels labels)
		{
			Span.Looper i = labels.getTextBase().documentSpanIterator();
			ProgressCounter pc = new ProgressCounter("tagging with segmenter", "document", i.estimatedSize() );
			while (i.hasNext() ) {
				Span document = i.nextSpan();
				MutableCandidateSegmentGroup g = new MutableCandidateSegmentGroup(maxWindowSize, document.size());
				for (int lo=0; lo<document.size(); lo++) {
					for (int len=1; len<=maxWindowSize; len++) {
						if (len+lo<=document.size()) {
							Span span = document.subSpan(lo,len);
							Instance instance = fe.extractInstance(labels,span);
							g.setSubsequence(lo,lo+len,instance);
						}
					}
				}
				Segmentation segmentation = segmenter.segmentation(g);
				if (DEBUG) log.debug("slidingWindowGroup: "+g);
				if (DEBUG) log.debug("segmentation: "+segmentation);
				for (Iterator j=segmentation.iterator(); j.hasNext(); ) {
					Segmentation.Segment seg = (Segmentation.Segment)j.next();
					String type = segmentation.className(seg);
					if (type!=null) {
						Span span = document.subSpan( seg.lo, seg.hi-seg.lo );
						labels.addToType( span, annotationType );
						if (DEBUG) log.debug("span of type: "+annotationType+": "+span);
					}
				}
				pc.progress();
			}
			pc.finished();
		}
		public String explainAnnotation(TextLabels labels,Span documentSpan)
		{
			return "not implemented";
		}
		public String toString()
		{
			return "[SegmentAnnotator "+annotationType+":\n"+segmenter+"]";
		}

		public Viewer toGUI()
		{
			Viewer v = new ComponentViewer() {
					public JComponent componentFor(Object o) {
						SegmentAnnotator sa = (SegmentAnnotator)o;
						JPanel mainPanel = new JPanel();
						mainPanel.setBorder(new TitledBorder("Segmenter Annotator"));
						Viewer subView = new SmartVanillaViewer(sa.segmenter);
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
