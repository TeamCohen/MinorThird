package edu.cmu.minorthird.classify.sequential;

import java.util.Iterator;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;


/**
 * Sequential learner based on the CRF algorithm.  Source for the iitb.CRF
 * package available from http://crf.sourceforge.net.
 * This class implements the semi-markov version of CRF
 *
 * @author Sunita Sarawagi
 */

public class SegmentCRFLearner extends CRFLearner implements BatchSegmenterLearner,SequenceConstants,Segmenter
{
	static final long serialVersionUID=20080207L;
	
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
    @Override
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
    @Override
		public int y(int i) {
	    return labels[i];
    }
    @Override
		public Object x(int i) {
	    return null;
    }
    @Override
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
    @Override
		public int getSegmentEnd(int segmentStart) {
	    return segLengths[segmentStart]+segmentStart-1;
    }
    @Override
		public void setSegment(int segmentStart, int segmentEnd, int y){
	    for (int pos = segmentStart; pos <= segmentEnd; pos++) {
        labels[pos] = y;
        segLengths[pos] = -1;
	    }
	    segLengths[segmentStart] = segmentEnd-segmentStart+1;
    }
  };

  class CRFSegmentDataIter implements iitb.CRF.DataIter {
    Iterator<CandidateSegmentGroup> iter;
    SegmentDataset dataset;
    SegmentDataSequence segData;
    CRFSegmentDataIter(SegmentDataset ds) {
	    dataset = ds;
	    segData = new SegmentDataSequence();
    }
    @Override
		public void startScan() {
	    iter =dataset.candidateSegmentGroupIterator(); 
    }
    @Override
		public boolean hasNext() {
	    return iter.hasNext();
    }
    @Override
		public iitb.CRF.DataSequence next() {
	    segData.init(iter.next());
	    return segData;
    }
  };

  class  NestedMTFeatureTypes extends MTFeatureTypes {
  	static final long serialVersionUID=20080207L;
    NestedMTFeatureTypes(iitb.Model.NestedFeatureGenImpl gen) {
	    super(gen);
    }
    @Override
		public  boolean startScanFeaturesAt(iitb.CRF.DataSequence data, int prevPos, int pos) {
	    SegmentDataSequence segData = (SegmentDataSequence)data;
	    example = segData.segs.getSubsequenceInstance(prevPos+1,pos+1);
	    featureLooper = example.featureIterator();
	    return startScan();
    }
  };

  public class SemiMTFeatureGenImpl extends iitb.Model.NestedFeatureGenImpl {
  	static final long serialVersionUID=20080207L;
    public SemiMTFeatureGenImpl(int numLabels, String[] labelNames, java.util.Properties options) throws Exception {
	    super(numLabels,options,false);
	    Feature features[] = new Feature[labelNames.length];
	    for (int i = 0; i < labelNames.length; i++)
        features[i] = new Feature(new String[]{ HISTORY_FEATURE, "1", labelNames[i]});
	    addFeature(new iitb.Model.EdgeFeatures(this, features));
	    addFeature(new iitb.Model.StartFeatures(this, new Feature(new String[]{ HISTORY_FEATURE, "1", NULL_CLASS_NAME})));
	    //addFeature(new iitb.Model.EndFeatures(model, new Feature("E")));
	    addFeature(new NestedMTFeatureTypes(this));
    }
  };
  iitb.CRF.NestedCRF nestedCrfModel;
  iitb.CRF.DataIter allocModel(SegmentDataset dataset) throws Exception {
    maxMemory = dataset.getMaxWindowSize();
    options.setProperty("MaxMemory",""+maxMemory);
    negativeClass = schema.getClassIndex(ExampleSchema.NEG_CLASS_NAME);
    featureGen = new SemiMTFeatureGenImpl(schema.getNumberOfClasses(),schema.validClassNames(),options);
    nestedCrfModel = new iitb.CRF.NestedCRF(featureGen.numStates(),featureGen,options);
    crfModel = nestedCrfModel;
    return new CRFSegmentDataIter(dataset);
  }
  @Override
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
  @Override
	public Segmentation segmentation(CandidateSegmentGroup g) {
    SegmentDataSequence seq = new SegmentDataSequence(g);
    nestedCrfModel.apply(seq);
    //	featureGen.mapStatesToLabels(seq);
    return seq.getSegments();
  }
  /** Return some string that 'explains' the classification */
  @Override
	public String explain(CandidateSegmentGroup g) {
    return "not supported";
  }

};
