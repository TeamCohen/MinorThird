package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.experiments.*;
import edu.cmu.minorthird.ui.*;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.util.*;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * Wraps an Annotator learned by SequenceAnnotatorLearner with code to
 * compute confidences for each extracted span. 
 *
 * Status: EXPERIMENTAL.  
 *
 * @author William Cohen
 */

public class ConfidenceReportingSequenceAnnotator extends AbstractAnnotator implements ExtractorAnnotator,Serializable,Visible
{
    private static Logger log = Logger.getLogger(ConfidenceReportingSequenceAnnotator.class);

    private SequenceAnnotatorLearner.SequenceAnnotator sequenceAnnotator;

    public ConfidenceReportingSequenceAnnotator(SequenceAnnotatorLearner.SequenceAnnotator sequenceAnnotator)
    {
        this.sequenceAnnotator = sequenceAnnotator;
    }

    public String getSpanType()
    {
        return sequenceAnnotator.getSpanType();
    }

    public void doAnnotate(MonotonicTextLabels labels)
    {
        Span.Looper i = labels.getTextBase().documentSpanIterator();
        ProgressCounter pc = new ProgressCounter("tagging with classifier", "document", i.estimatedSize() );
        while (i.hasNext() ) {

            Span docSpan = i.nextSpan();
            String docId = docSpan.getDocumentId();

            log.info("extracting from doc '"+docId+"'");

            // create a TextLabels holding just this document which
            // contains all the annotations of labels, but make sure
            // that any annotations made on the textLabels will NOT
            // propogate back to the original textLabels
            Span.Looper justThisDoc = new BasicSpanLooper(Collections.singleton(docSpan));
            MonotonicSubTextLabels tmpLabels = null;
            try {
                SubTextBase tmpBase = new SubTextBase( labels.getTextBase(), justThisDoc );
                tmpLabels = new MonotonicSubTextLabels(tmpBase,new NestedTextLabels(labels));
            } catch (SubTextBase.UnknownDocumentException ex) {
                throw new IllegalStateException("error: "+ex);
            }

            // build a sequence of instances for this document and classify it
            Instance[] sequence = new Instance[docSpan.size()];
            for (int j=0; j<docSpan.size(); j++) {
                Span tokenSpan = docSpan.subSpan(j,1);
                sequence[j] = sequenceAnnotator.getSpanFeatureExtractor().extractInstance(labels,tokenSpan);
            }
            ClassLabel[] classLabels = sequenceAnnotator.getSequenceClassifier().classification( sequence );

            // now, look at each extracted span in tmpLabels, compute its confidence,
            // and pass that along to the 'label' structure
            sequenceAnnotator.doAnnotate( tmpLabels );
            //new ViewerFrame(docId,new SmartVanillaViewer(tmpLabels));
            for (Span.Looper k=tmpLabels.instanceIterator(sequenceAnnotator.getSpanType()); k.hasNext(); ) {
                Span extractedSpan = k.nextSpan();
                double confidence = computeConfidence( extractedSpan, classLabels );
                log.info("confidence: "+confidence+" for "+extractedSpan);
                labels.addToType(extractedSpan, sequenceAnnotator.getSpanType(), new Details(confidence,ConfidenceReportingSequenceAnnotator.class));
            }
            pc.progress();
        }
        pc.finished();
    }
    
    private double computeConfidence( Span extractedSpan, ClassLabel[] classLabels)
    {
        int startIndex = extractedSpan.documentSpanStartIndex();
        int endIndex = startIndex+extractedSpan.size();
        return ConfidenceUtils.sumPredictedWeights(classLabels,startIndex,endIndex);
    }

    public String explainAnnotation(TextLabels labels,Span documentSpan)
    {
        return sequenceAnnotator.explainAnnotation(labels,documentSpan);
    }

    public Viewer toGUI()
    {
        return new SmartVanillaViewer(sequenceAnnotator);
    }


    /**
     * Convert an appropriately trained ExtractorAnnotator to a
     * ConfidenceReportingSequenceAnnotator.  This only works for an
     * ExtractorAnnotator that was trained with a
     * SequenceAnnotatorLearner.
     */
    
    static public void main(String[] args) 
    {
        if (args.length!=2) {
            throw new IllegalArgumentException("usage: previouslySavedAnnotatorFile newAnnotatorFile");
        } 
        File loadFile = new File(args[0]);
        File saveFile = new File(args[1]);
        Annotator ann = null;
	try {
	    ann = (Annotator)IOUtil.loadSerialized(loadFile);
	} catch (IOException ex) {
	    throw new IllegalArgumentException("can't load annotator from "+loadFile+": "+ex);
	}
        if (!(ann instanceof SequenceAnnotatorLearner.SequenceAnnotator)) {
            throw new IllegalArgumentException(loadFile+" does not contain an annotator learned with a SequenceAnnotatorLearner");
        }
        SequenceAnnotatorLearner.SequenceAnnotator seqAnn = (SequenceAnnotatorLearner.SequenceAnnotator)ann;
        ConfidenceReportingSequenceAnnotator newAnnotator = new ConfidenceReportingSequenceAnnotator(seqAnn);
        try {
            IOUtil.saveSerialized( newAnnotator, saveFile );
        } catch (IOException ex) {
	    throw new IllegalArgumentException("can't save new annotator in "+saveFile+": "+ex);            
        }
    }
}
