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
import edu.cmu.minorthird.classify.sequential.BatchSequenceClassifierLearner;
import edu.cmu.minorthird.classify.sequential.CollinsPerceptronLearner;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Learn an annotation model using a sequence dataset and a
 * BatchSequenceClassifierLearner. This class reduces extraction learning to
 * sequential classification of tokens. The scheme for mapping extraction
 * learning to token learning is determined by the Extraction2TaggingReduction.
 * 
 * @author William Cohen
 */

public class SequenceAnnotatorLearner extends AbstractBatchAnnotatorLearner{

	private static Logger log=Logger.getLogger(SequenceAnnotatorLearner.class);

	private static final boolean DEBUG=false;

	protected BatchSequenceClassifierLearner seqLearner;

	public SequenceAnnotatorLearner(){
		super();
		seqLearner=new CollinsPerceptronLearner();
	}

	public SequenceAnnotatorLearner(BatchSequenceClassifierLearner seqLearner,
			SpanFeatureExtractor fe){
		super(fe,new InsideOutsideReduction());
		this.seqLearner=seqLearner;
	}

	public SequenceAnnotatorLearner(BatchSequenceClassifierLearner seqLearner,
			SpanFeatureExtractor fe,Extraction2TaggingReduction reduction){
		super(fe,reduction);
		this.seqLearner=seqLearner;
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

	public BatchSequenceClassifierLearner getSequenceClassifierLearner(){
		return seqLearner;
	}

	public void setSequenceClassifierLearner(
			BatchSequenceClassifierLearner learner){
		this.seqLearner=learner;
	}

	// Help Buttons
	public String getDisplayDatasetBeforeLearningHelp(){
		return "Pop up an interactive viewer of the sequential dataset before learning.";
	}

	public String getSequenceClassifierLearnerHelp(){
		return "The classifierLearner used to classify each token using the <br>predictions of previous tokens as features";
	}

	/**
	 * Return the learned annotator.
	 */
	@Override
	public Annotator getAnnotator(){
		ExampleSchema schema=seqData.getSchema();
		if(schema.getNumberOfClasses()<=1){
			log.error("In the constructed dataset the number of classes is "+
					schema.getNumberOfClasses());
			log
					.error("Hint: this probably means that no spans of the specified type are present in your data");
		}
		seqLearner.setSchema(schema);
		if(displayDatasetBeforeLearning)
			new ViewerFrame("Sequential Dataset",seqData.toGUI());
		SequenceClassifier seqClassifier=seqLearner.batchTrain(seqData);
		if(DEBUG)
			log.debug("learned classifier: "+seqClassifier);
		return new SequenceAnnotatorLearner.SequenceAnnotator(seqClassifier,fe,
				reduction,annotationType);
	}

	/**
	 * A useful subroutine - prepare sequence data the way a
	 * SequenceAnnotatorLearner would prepare it when trained by a
	 * TextLabelsAnnotatorTeacher.
	 * 
	 */
	static public SequenceDataset prepareSequenceData(TextLabels labels,
			String spanType,String spanProp,SpanFeatureExtractor fe,
			final int historySize,Extraction2TaggingReduction reduction){
		BatchSequenceClassifierLearner dummy1=new BatchSequenceClassifierLearner(){

			@Override
			public void setSchema(ExampleSchema schema){
			}

			@Override
			public SequenceClassifier batchTrain(SequenceDataset dataset){
				return null;
			}

			@Override
			public int getHistorySize(){
				return historySize;
			}
		};
		SequenceAnnotatorLearner dummy2=
				new SequenceAnnotatorLearner(dummy1,fe,reduction){

					@Override
					public Annotator getAnnotator(){
						return null;
					}
				};
		new TextLabelsAnnotatorTeacher(labels,spanType,spanProp).train(dummy2);
		return dummy2.getSequenceDataset();
	}

	//
	// learned annotator
	//

	public static class SequenceAnnotator extends AbstractAnnotator implements
			Serializable,Visible,ExtractorAnnotator{

		private static final long serialVersionUID=2;

		private SequenceClassifier seqClassifier;

		private SpanFeatureExtractor fe;

		private Extraction2TaggingReduction reduction;

		private String annotationType;

		public SequenceAnnotator(SequenceClassifier seqClassifier,
				SpanFeatureExtractor fe,String annotationType){
			this(seqClassifier,fe,new InsideOutsideReduction(),annotationType);
		}

		public SequenceAnnotator(SequenceClassifier seqClassifier,
				SpanFeatureExtractor fe,Extraction2TaggingReduction reduction,
				String annotationType){
			this.seqClassifier=seqClassifier;
			this.fe=fe;
			this.reduction=reduction;
			this.annotationType=annotationType;
		}

		@Override
		public String getSpanType(){
			return annotationType;
		}

		public SpanFeatureExtractor getSpanFeatureExtractor(){
			return fe;
		}

		public Extraction2TaggingReduction getReduction(){
			return reduction;
		}

		public SequenceClassifier getSequenceClassifier(){
			return seqClassifier;
		}

		@Override
		protected void doAnnotate(MonotonicTextLabels labels){
			Iterator<Span> i=labels.getTextBase().documentSpanIterator();
			ProgressCounter pc=
					new ProgressCounter("tagging with classifier","document");
			while(i.hasNext()){
				Span s=i.next();
				Instance[] sequence=new Instance[s.size()];
				for(int j=0;j<s.size();j++){
					Span tokenSpan=s.subSpan(j,1);
					sequence[j]=fe.extractInstance(labels,tokenSpan);
				}
				ClassLabel[] classLabels=seqClassifier.classification(sequence);
				for(int j=0;j<classLabels.length;j++){
					labels.setProperty(s.getToken(j),reduction.getTokenProp(),
							classLabels[j].bestClassName());
				}
				pc.progress();
			}
			pc.finished();
			reduction.extractFromTags(annotationType,labels);
		}

		@Override
		public String explainAnnotation(TextLabels labels,Span documentSpan){
			return "not implemented";
		}

		@Override
		public String toString(){
			return "[SequenceAnnotator "+annotationType+":\n"+seqClassifier+"]";
		}

		@Override
		public Viewer toGUI(){
			Viewer v=new ComponentViewer(){
				static final long serialVersionUID=20080306L;
				@Override
				public JComponent componentFor(Object o){
					SequenceAnnotator sa=(SequenceAnnotator)o;
					JPanel mainPanel=new JPanel();
					mainPanel.setBorder(new TitledBorder("Sequence Annotator"));
					Viewer subView=new SmartVanillaViewer(sa.seqClassifier);
					subView.setSuperView(this);
					mainPanel.add(subView);
					return new JScrollPane(mainPanel);
				}
			};
			v.setContent(this);
			return v;
		}
	}

	static public void main(String[] args){
		try{
			SequenceAnnotator a=
					(SequenceAnnotator)edu.cmu.minorthird.util.IOUtil
							.loadSerialized(new java.io.File(args[0]));
			a.annotationType=args[1];
			edu.cmu.minorthird.util.IOUtil
					.saveSerialized(a,new java.io.File(args[2]));
		}catch(Exception ex){
			ex.printStackTrace();
			System.out.println("usage: inputFile new-annotation-type outputfile");
		}
	}
}
