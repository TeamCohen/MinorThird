package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;

import java.io.Serializable;
import java.util.Iterator;
import java.util.*;
import java.awt.*;


/**
 * Sequential learner based on the CRF algorithm.  Source for the iitb.CRF
 * package available from http://crf.sourceforge.net.
 * This class implements the semi-markov version of CRF
 *
 * @author Sunita Sarawagi
 */

public class SegmentCRFLearner extends CRFLearner implements BatchSegmenterLearner,SequenceConstants,Segmenter
{
  static int negativeClass = 0;
  int maxMemory;

  public SegmentCRFLearner() {
    this("");
  }

  public SegmentCRFLearner(String args) {
    super(args);
  }
    
  class SegmentDataSequence implements iitb.CRF.SegmentDataSequence {
    CandidateSegmentGroup segs;
    int labels[]=null;
    int segLengths[];
    SegmentDataSequence(CandidateSegmentGroup tokens) {
	    segs = tokens;
	    alloc();
    }
    SegmentDataSequence() {}
    void alloc() {
	    if ((labels == null) || (length() > labels.length)) {
        labels = new int[length()];
        segLengths = new int[length()];
	    }
    }
    public int length() {return segs.getSequenceLength();}
    void init(CandidateSegmentGroup tokens) {
	    segs = tokens;
	    alloc();

	    int pos, len;
	    for (pos=0; pos<length(); pos++) {
        labels[pos] = negativeClass;
        segLengths[pos] = 1;
        for (len=1;  len<=tokens.getMaxWindowSize(); len++) {
          Instance inst = tokens.getSubsequenceInstance(pos,pos+len);
          ClassLabel label = tokens.getSubsequenceLabel(pos,pos+len);
          if (inst!=null && !label.isNegative()) {
            for (int k = pos; k < pos+len; segLengths[k] = -1, labels[k++] = schema.getClassIndex(label.bestClassName()));
            segLengths[pos] = len;
            pos += (len-1);
            break;
          }
        }
	    }
    }
    public int y(int i) {
	    return labels[i];
    }
    public Object x(int i) {
	    return null;
    }
    public void set_y(int i, int label) {
	    labels[i] = label;
    }
    Segmentation getSegments() {	
	    Segmentation segs = new Segmentation(schema); 
	    for (int i = 0; i < length(); i+= segLengths[i]) {
        segs.add(new Segmentation.Segment(i,i+segLengths[i],labels[i]));
	    }
	    return segs;
    }
    public int getSegmentEnd(int segmentStart) {
	    return segLengths[segmentStart]+segmentStart-1;
    }
    public void setSegment(int segmentStart, int segmentEnd, int y){
	    for (int pos = segmentStart; pos <= segmentEnd; pos++) {
        labels[pos] = y;
        segLengths[pos] = -1;
	    }
	    segLengths[segmentStart] = segmentEnd-segmentStart+1;
    }
  };

  class CRFSegmentDataIter implements iitb.CRF.DataIter {
    SegmentDataset.Looper iter;
    SegmentDataset dataset;
    SegmentDataSequence segData;
    CRFSegmentDataIter(SegmentDataset ds) {
	    dataset = ds;
	    segData = new SegmentDataSequence();
    }
    public void startScan() {
	    iter =dataset.candidateSegmentGroupIterator(); 
    }
    public boolean hasNext() {
	    return iter.hasNext();
    }
    public iitb.CRF.DataSequence next() {
	    segData.init(iter.nextCandidateSegmentGroup());
	    return segData;
    }
  };

  class  NestedMTFeatureTypes extends MTFeatureTypes {
    NestedMTFeatureTypes(iitb.Model.Model m) {
	    super(m);
    }
    public  boolean startScanFeaturesAt(iitb.CRF.DataSequence data, int prevPos, int pos) {
	    SegmentDataSequence segData = (SegmentDataSequence)data;
	    example = segData.segs.getSubsequenceInstance(prevPos+1,pos+1);
	    featureLooper = example.featureIterator();
	    return startScan();
    }
  };

  public class SemiMTFeatureGenImpl extends iitb.Model.NestedFeatureGenImpl {
    public SemiMTFeatureGenImpl(int numLabels, String[] labelNames, java.util.Properties options) throws Exception {
	    super(numLabels,options,false);
	    Feature features[] = new Feature[labelNames.length];
	    for (int i = 0; i < labelNames.length; i++)
        features[i] = new Feature(new String[]{ HISTORY_FEATURE, "1", labelNames[i]});
	    addFeature(new iitb.Model.EdgeFeatures(model, features));
	    addFeature(new iitb.Model.StartFeatures(model, new Feature(new String[]{ HISTORY_FEATURE, "1", NULL_CLASS_NAME})));
	    //addFeature(new iitb.Model.EndFeatures(model, new Feature("E")));
	    addFeature(new NestedMTFeatureTypes(model));
    }
  };
  iitb.CRF.NestedCRF nestedCrfModel;
  iitb.CRF.DataIter allocModel(SegmentDataset dataset) throws Exception {
    maxMemory = dataset.getMaxWindowSize();
    options.setProperty("MaxMemory",""+maxMemory);
    negativeClass = schema.getClassIndex(ExampleSchema.NEG_CLASS_NAME);
    featureGen = new SemiMTFeatureGenImpl(schema.getNumberOfClasses(),schema.validClassNames(),options);
    nestedCrfModel = new iitb.CRF.NestedCRF(featureGen.numStates(),(iitb.CRF.FeatureGeneratorNested)featureGen,options);
    crfModel = nestedCrfModel;
    return new CRFSegmentDataIter(dataset);
  }
  public Segmenter batchTrain(SegmentDataset dataset) {
    try {
	    schema = dataset.getSchema();
	    doTrain(allocModel(dataset));
	    return this;
    } catch (Exception e) {
	    e.printStackTrace();
	    throw new IllegalStateException("error in CRF: "+e);
    }
  }
  /** Return a predicted type for each element of the sequence. */
  public Segmentation segmentation(CandidateSegmentGroup g) {
    SegmentDataSequence seq = new SegmentDataSequence(g);
    nestedCrfModel.apply(seq);
    //	featureGen.mapStatesToLabels(seq);
    return seq.getSegments();
  }
  /** Return some string that 'explains' the classification */
  public String explain(CandidateSegmentGroup g) {
    return "not supported";
  }

};
