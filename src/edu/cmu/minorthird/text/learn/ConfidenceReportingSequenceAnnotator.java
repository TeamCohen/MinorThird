package edu.cmu.minorthird.text.learn;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.sequential.ConfidenceReportingSequenceClassifier;
import edu.cmu.minorthird.classify.sequential.ConfidenceUtils;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;
import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.Details;
import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.learn.experiments.MonotonicSubTextLabels;
import edu.cmu.minorthird.text.learn.experiments.SubTextBase;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Wraps an Annotator learned by SequenceAnnotatorLearner with code to compute
 * confidences for each extracted span.
 * 
 * <p>
 * Sample use, in code: <code><pre>
 * // below, 'ann' might be trained, or pulled off disk - this
 * // is the sort of object that the SequenceAnnotatorLearner instances
 * // (like CRFAnnotatorLearner and VPHMMLearner) produce
 * SequenceAnnotatorLearner.SequenceAnnotator ann = ...;
 * ConfidenceReportingSequenceAnnotator crAnn = new ConfidenceReportingSequenceAnnotator(ann);
 * crAnn.annotate(textLabels);
 * // print the confidence of each extracted span
 * for (Span.Looper i=textLabels.instanceIterator(crAnn.getSpanType()); i.hasNext(); ) {
 *    Span s = i.nextSpan();
 *    Details d = labels.getDetails(s,crAnn.getSpanType());
 *    System.out.println(&quot;confidence=&quot;+d.getConfidence()+&quot; for span &quot;+s);
 * }
 * </pre></code>
 * 
 * <p>
 * Sample use, from command line: <code><pre>
 * # train SequenceAnnotatorLearner.SequenceAnnotator object and save in 'old.ann'
 * % java edu.cmu.minorthird.ui.TrainExtractor -labels sample1.train -saveAs old.ann -spanType trueName 
 * # convert to a ConfidenceReportingSequenceAnnotator 'new.ann'
 * % java edu.cmu.minorthird.text.learn.ConfidenceReportingSequenceAnnotator old.ann new.ann
 * # apply the annotator and view the results - the last column is confidence
 * % java edu.cmu.minorthird.ui.ApplyAnnotator -labels sample1.test -loadFrom new.ann -saveAs new.labels
 * % grep _prediction new.labels
 * addToType testStrings[0] 0 12 _prediction 141.0
 * addToType testStrings[1] 19 15 _prediction 312.0
 * addToType testStrings[2] 8 12 _prediction 188.0
 * addToType testStrings[3] 47 11 _prediction 374.0
 * </pre></code>
 * 
 * Status: EXPERIMENTAL.
 * 
 * @author William Cohen
 */

public class ConfidenceReportingSequenceAnnotator extends AbstractAnnotator
		implements ExtractorAnnotator,Serializable,Visible{

	static final long serialVersionUID=20080306L;
	
	private static Logger log=
			Logger.getLogger(ConfidenceReportingSequenceAnnotator.class);

	private static final boolean DEBUG=false;

	private SequenceAnnotatorLearner.SequenceAnnotator sequenceAnnotator;

	public ConfidenceReportingSequenceAnnotator(
			SequenceAnnotatorLearner.SequenceAnnotator sequenceAnnotator){
		this.sequenceAnnotator=sequenceAnnotator;
	}

	@Override
	public String getSpanType(){
		return sequenceAnnotator.getSpanType();
	}

	@Override
	public void doAnnotate(MonotonicTextLabels labels){
		Iterator<Span> i=labels.getTextBase().documentSpanIterator();
		ProgressCounter pc=
				new ProgressCounter("tagging with classifier","document");
		while(i.hasNext()){

			Span docSpan=i.next();
			String docId=docSpan.getDocumentId();

			log.info("extracting from doc '"+docId+"'");

			// create a TextLabels holding just this document which
			// contains all the annotations of labels, but make sure
			// that any annotations made on the textLabels will NOT
			// propogate back to the original textLabels
			Iterator<Span> justThisDoc=Collections.singleton(docSpan).iterator();
			MonotonicSubTextLabels tmpLabels=null;
			try{
				SubTextBase tmpBase=new SubTextBase(labels.getTextBase(),justThisDoc);
				tmpLabels=
						new MonotonicSubTextLabels(tmpBase,new NestedTextLabels(labels));
			}catch(SubTextBase.UnknownDocumentException ex){
				throw new IllegalStateException("error: "+ex);
			}

			// build a sequence of instances for this document and classify it
			Instance[] sequence=new Instance[docSpan.size()];
			for(int j=0;j<docSpan.size();j++){
				Span tokenSpan=docSpan.subSpan(j,1);
				sequence[j]=
						sequenceAnnotator.getSpanFeatureExtractor().extractInstance(labels,
								tokenSpan);
			}
			ClassLabel[] classLabels=
					sequenceAnnotator.getSequenceClassifier().classification(sequence);

			// now, look at each extracted span in tmpLabels, compute its confidence,
			// and pass that along to the 'label' structure
			sequenceAnnotator.doAnnotate(tmpLabels);
			// new ViewerFrame(docId,new SmartVanillaViewer(tmpLabels));
			for(Iterator<Span> k=
					tmpLabels.instanceIterator(sequenceAnnotator.getSpanType());k
					.hasNext();){
				Span extractedSpan=k.next();
				double confidence=
						computeConfidence(sequenceAnnotator.getSequenceClassifier(),
								sequence,extractedSpan,classLabels);
				if(DEBUG)
					log.info("confidence: "+confidence+" for "+extractedSpan);
				labels.addToType(extractedSpan,sequenceAnnotator.getSpanType(),
						new Details(confidence,ConfidenceReportingSequenceAnnotator.class));
			}
			pc.progress();
		}
		pc.finished();
	}

	private double computeConfidence(SequenceClassifier seqClassifier,
			Instance[] sequence,Span extractedSpan,ClassLabel[] predictedLabels){
		int startIndex=extractedSpan.documentSpanStartIndex();
		int endIndex=startIndex+extractedSpan.size();
		if(seqClassifier instanceof ConfidenceReportingSequenceClassifier){
			ConfidenceReportingSequenceClassifier crSeqClassifier=
					(ConfidenceReportingSequenceClassifier)seqClassifier;
			// report confidence if the positions startIndex...endIndex-1 are
			// constrained to be 'NEG'
			ClassLabel[] alternateLabels=new ClassLabel[predictedLabels.length];
			for(int i=startIndex;i<endIndex;i++){
				alternateLabels[i]=ClassLabel.negativeLabel(-1.0);
			}
			return crSeqClassifier.confidence(sequence,predictedLabels,
					alternateLabels,startIndex,endIndex);
		}else{
			return ConfidenceUtils.sumPredictedWeights(predictedLabels,startIndex,
					endIndex);
		}
	}

	@Override
	public String explainAnnotation(TextLabels labels,Span documentSpan){
		return sequenceAnnotator.explainAnnotation(labels,documentSpan);
	}

	@Override
	public Viewer toGUI(){
		return new SmartVanillaViewer(sequenceAnnotator);
	}

	/**
	 * Convert an appropriately trained ExtractorAnnotator to a
	 * ConfidenceReportingSequenceAnnotator. This only works for an
	 * ExtractorAnnotator that was trained with a SequenceAnnotatorLearner.
	 */

	static public void main(String[] args){
		if(args.length==3&&"-test".equals(args[0])){
			// undocumented usage mode: -test previouslySavedAnnotatorFile labelsKey
			File loadFile=new File(args[1]);
			MonotonicTextLabels labels=
					new NestedTextLabels(FancyLoader.loadTextLabels(args[2]));
			SequenceAnnotatorLearner.SequenceAnnotator ann=null;
			try{
				ann=
						(SequenceAnnotatorLearner.SequenceAnnotator)IOUtil
								.loadSerialized(loadFile);
			}catch(IOException ex){
				throw new IllegalArgumentException("can't load annotator from "+
						loadFile+": "+ex);
			}
			ConfidenceReportingSequenceAnnotator crAnn=
					new ConfidenceReportingSequenceAnnotator(ann);
			crAnn.annotate(labels);
			// print the confidence of each extracted span
			for(Iterator<Span> i=labels.instanceIterator(crAnn.getSpanType());i
					.hasNext();){
				Span s=i.next();
				Details d=labels.getDetails(s,crAnn.getSpanType());
				System.out.println("confidence="+d.getConfidence()+" for span "+s);
			}
			return;
		}else if(args.length!=2){
			throw new IllegalArgumentException(
					"usage: previouslySavedAnnotatorFile newAnnotatorFile");
		}
		File loadFile=new File(args[0]);
		File saveFile=new File(args[1]);
		Annotator ann=null;
		try{
			ann=(Annotator)IOUtil.loadSerialized(loadFile);
		}catch(IOException ex){
			throw new IllegalArgumentException("can't load annotator from "+loadFile+
					": "+ex);
		}
		if(!(ann instanceof SequenceAnnotatorLearner.SequenceAnnotator)){
			throw new IllegalArgumentException(loadFile+
					" does not contain an annotator learned with a SequenceAnnotatorLearner");
		}
		SequenceAnnotatorLearner.SequenceAnnotator seqAnn=
				(SequenceAnnotatorLearner.SequenceAnnotator)ann;
		ConfidenceReportingSequenceAnnotator newAnnotator=
				new ConfidenceReportingSequenceAnnotator(seqAnn);
		try{
			IOUtil.saveSerialized(newAnnotator,saveFile);
		}catch(IOException ex){
			throw new IllegalArgumentException("can't save new annotator in "+
					saveFile+": "+ex);
		}
	}
}
