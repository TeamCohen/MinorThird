package edu.cmu.minorthird.text.learn;

import java.io.Serializable;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.sequential.BatchSegmenterLearner;
import edu.cmu.minorthird.classify.sequential.MutableCandidateSegmentGroup;
import edu.cmu.minorthird.classify.sequential.SegmentCollinsPerceptronLearner;
import edu.cmu.minorthird.classify.sequential.SegmentDataset;
import edu.cmu.minorthird.classify.sequential.Segmentation;
import edu.cmu.minorthird.classify.sequential.Segmenter;
import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.ui.Recommended;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Learn an annotation model using a SegmentDataset dataset and a
 * BatchSequenceClassifierLearner.
 * 
 * @author William Cohen
 */

public class SegmentAnnotatorLearner extends AnnotatorLearner{

	private static Logger log=Logger.getLogger(SegmentAnnotatorLearner.class);

	private static final boolean DEBUG=log.isDebugEnabled();

	protected String annotationType="_prediction";

	protected SegmentDataset dataset;

	protected BatchSegmenterLearner learner;

	protected SpanFeatureExtractor fe;

	protected int maxWindowSize;

	public SegmentAnnotatorLearner(){
		this(new SegmentCollinsPerceptronLearner(),
				new Recommended.MultitokenSpanFE());
	}

	public SegmentAnnotatorLearner(BatchSegmenterLearner learner,
			SpanFeatureExtractor fe){
		this(learner,fe,4);
	}

	public SegmentAnnotatorLearner(BatchSegmenterLearner learner,
			SpanFeatureExtractor fe,int windowSize){
		this.learner=learner;
		this.fe=fe;
		this.maxWindowSize=windowSize;
		reset();
	}

	@Override
	public void reset(){
		dataset=new SegmentDataset();
		dataset.setDataCompression(compressDataset);
	}

	//
	// getters and setters
	//
	private boolean displayDatasetBeforeLearning=false;

	/**
	 * If set, try and pop up an interactive viewer of the sequential dataset
	 * before learning.
	 */
	public boolean getDisplayDatasetBeforeLearning(){
		return displayDatasetBeforeLearning;
	}

	public void setDisplayDatasetBeforeLearning(
			boolean newDisplayDatasetBeforeLearning){
		this.displayDatasetBeforeLearning=newDisplayDatasetBeforeLearning;
	}

	public String getDisplayDatasetBeforeLearningHelp(){
		return "Pop up an interactive viewer of the sequential dataset before learning.";
	}

	private boolean compressDataset=true;

	/**
	 * If set, try and compress the data. This leads to longer loading and
	 * learning times but less memory usage.
	 */
	public boolean getCompressDataset(){
		return compressDataset;
	}

	public void setCompressDataset(boolean flag){
		compressDataset=flag;
	}

	public String getCompressDatasetHelp(){
		return "If set, try and compress the data. This leads to longer loading and <br>learning times but less memory usage.";
	}

	public int getHistorySize(){
		return 1;
	}

	public BatchSegmenterLearner getSemiMarkovLearner(){
		return learner;
	}

	public void setSemiMarkovLearner(BatchSegmenterLearner learner){
		this.learner=learner;
	}

	public String getSemiMarkovLearnerHelp(){
		return "Set the SemiMarkowLearner to be used";
	}

	@Override
	public SpanFeatureExtractor getSpanFeatureExtractor(){
		return fe;
	}

	@Override
	public void setSpanFeatureExtractor(SpanFeatureExtractor fe){
		this.fe=fe;
	}

	/**
	 * Specify the type of annotation produced by this annotator - that is, the
	 * type associated with spans produced by it.
	 */
	@Override
	public void setAnnotationType(String s){
		annotationType=s;
	}

	@Override
	public String getAnnotationType(){
		return annotationType;
	}

	// temporary storage
	private Iterator<Span> documentLooper;

	/** Accept the pool of unlabeled documents. */
	@Override
	public void setDocumentPool(Iterator<Span> documentLooper){
		this.documentLooper=documentLooper;
	}

	/** Ask for labels on every document. */
	@Override
	public boolean hasNextQuery(){
		return documentLooper.hasNext();
	}

	/** Return the next unlabeled document. */
	@Override
	public Span nextQuery(){
		return documentLooper.next();
	}

	/** Accept the answer to the last query. */
	@Override
	public void setAnswer(AnnotationExample answeredQuery){
		// add examples to the SegmentDataset
		Span document=answeredQuery.getDocumentSpan();
		MutableCandidateSegmentGroup g=
				new MutableCandidateSegmentGroup(maxWindowSize,document.size());
		for(int lo=0;lo<document.size();lo++){
			for(int len=1;len<=maxWindowSize;len++){
				if(len+lo<=document.size()){
					Span span=document.subSpan(lo,len);
					Instance instance=fe.extractInstance(answeredQuery.getLabels(),span);
					ClassLabel label=new ClassLabel(answeredQuery.getClassName(span));
					g.setSubsequence(lo,lo+len,instance,label);
				}
			}
		}
		dataset.addCandidateSegmentGroup(g);
	}

	/**
	 * Return the learned annotator.
	 */
	@Override
	public Annotator getAnnotator(){
		learner.setSchema(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
		if(displayDatasetBeforeLearning)
			new ViewerFrame("Sequential Dataset",dataset.toGUI());
		Segmenter segmenter=learner.batchTrain(dataset);
		if(DEBUG)
			log.debug("learned segmenter: "+segmenter);
		return new SegmentAnnotator(segmenter,fe,maxWindowSize,annotationType);
	}

	//
	// learned annotator
	//

	public static class SegmentAnnotator extends AbstractAnnotator implements
			Serializable,Visible,ExtractorAnnotator{

		private static final long serialVersionUID=1;

		private Segmenter segmenter;

		private SpanFeatureExtractor fe;

		private String annotationType;

		private int maxWindowSize;

		public SegmentAnnotator(Segmenter segmenter,SpanFeatureExtractor fe,
				int maxWindowSize,String annotationType){
			this.segmenter=segmenter;
			this.fe=fe;
			this.maxWindowSize=maxWindowSize;
			this.annotationType=annotationType;
		}

		@Override
		public String getSpanType(){
			return annotationType;
		}

		@Override
		protected void doAnnotate(MonotonicTextLabels labels){
			Iterator<Span> i=labels.getTextBase().documentSpanIterator();
			ProgressCounter pc=
					new ProgressCounter("tagging with segmenter","document");
			while(i.hasNext()){
				Span document=i.next();
				MutableCandidateSegmentGroup g=
						new MutableCandidateSegmentGroup(maxWindowSize,document.size());
				for(int lo=0;lo<document.size();lo++){
					for(int len=1;len<=maxWindowSize;len++){
						if(len+lo<=document.size()){
							Span span=document.subSpan(lo,len);
							Instance instance=fe.extractInstance(labels,span);
							g.setSubsequence(lo,lo+len,instance);
						}
					}
				}
				Segmentation segmentation=segmenter.segmentation(g);
				if(DEBUG)
					log.debug("slidingWindowGroup: "+g);
				if(DEBUG)
					log.debug("segmentation: "+segmentation);
				for(Iterator<Segmentation.Segment> j=segmentation.iterator();j.hasNext();){
					Segmentation.Segment seg=j.next();
					String type=segmentation.className(seg);
					if(type!=null){
						Span span=document.subSpan(seg.lo,seg.hi-seg.lo);
						labels.addToType(span,annotationType);
						if(DEBUG)
							log.debug("span of type: "+annotationType+": "+span);
					}
				}
				pc.progress();
			}
			pc.finished();
		}

		@Override
		public String explainAnnotation(TextLabels labels,Span documentSpan){
			return "not implemented";
		}

		@Override
		public String toString(){
			return "[SegmentAnnotator "+annotationType+":\n"+segmenter+"]";
		}

		@Override
		public Viewer toGUI(){
			Viewer v=new ComponentViewer(){
				static final long serialVersionUID=20080306L;
				@Override
				public JComponent componentFor(Object o){
					SegmentAnnotator sa=(SegmentAnnotator)o;
					JPanel mainPanel=new JPanel();
					mainPanel.setBorder(new TitledBorder("Segmenter Annotator"));
					Viewer subView=new SmartVanillaViewer(sa.segmenter);
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
