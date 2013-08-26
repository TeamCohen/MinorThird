package iitb.Model;
import iitb.CRF.DataSequence;
import iitb.CRF.FeatureGeneratorNested;
import iitb.CRF.SegmentDataSequence;

/**
 * 
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
public class NestedFeatureGenImpl extends FeatureGenImpl implements FeatureGeneratorNested {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6957722060399675011L;
	/* protected boolean holdsInData(DataSequence seq, FeatureImpl f) {
     return (cposEnd == ((SegmentDataSequence)seq).getSegmentEnd(cposStart)) 
     && ((cposStart == 0) || (cposStart-1 == ((SegmentDataSequence)seq).getSegmentEnd(cposStart-1)))
     &&    super.holdsInData(seq, f);
     }
     */
    int maxMem[];
    int maxMemOverall=1;
    public int addTrainRecord(DataSequence data) {
        int numF = 0;
        if (addOnlyTrainFeatures) {
            SegmentDataSequence seq = (SegmentDataSequence)data;
            int segEnd;
            for (int l = 0; l < seq.length(); l = segEnd+1) {
                segEnd = seq.getSegmentEnd(l);
                for (startScanFeaturesAt(seq,l-1,segEnd); hasNext(); next(),numF++);
            }
        } else {
            for (int l = 0; l < data.length(); l++) {
                for (int m = 1; (m <= maxMemOverall) && (l-m >= -1); m++) {
                    for (startScanFeaturesAt(data,l-m,l); hasNext(); ) {
                        next(); numF++;
                    }
                }
            }
        }
        return numF;
    }
    /**
     * @param featureType
     * @param seq
     */
    protected void trainFeatureType(FeatureTypes featureType, DataSequence data) {
        SegmentDataSequence seq = (SegmentDataSequence)data;
        int segEnd;
        for (int l = 0; l < seq.length(); l = segEnd+1) {
            segEnd = seq.getSegmentEnd(l);
            featureType.train(seq,l,segEnd);
        }
    }
    public NestedFeatureGenImpl(int numLabels,java.util.Properties options, boolean addFeatureNow) throws Exception {
        super("naive",numLabels,false);
        if (options.getProperty("MaxMemory") != null) {
            maxMemOverall = Integer.parseInt(options.getProperty("MaxMemory"));
        } 
        if (addFeatureNow) {
            addFeature(new EdgeFeatures(this));
            addFeature(new StartFeatures(this));
            addFeature(new EndFeatures(this));
            dict = new WordsInTrain();
            addFeature(new FeatureTypesMulti(new UnknownFeature(this,dict)));
            addFeature(new FeatureTypesMulti(new WordFeatures(this, dict)));
            addFeature(new FeatureTypesEachLabel(this, new FeatureTypesSegmentLength(this)));
            WindowFeatures.Window windows[] = new WindowFeatures.Window[] {
                    new WindowFeatures.Window(0,true,0,true,"start"), 
                    new WindowFeatures.Window(0,false,0,false,"end"),
                    new WindowFeatures.Window(1,true,-1,false,"continue"),
                    new WindowFeatures.Window(-1,true,-1,true,"left-1"),
                    new WindowFeatures.Window(1,false,1,false,"right+1"),
            };
            /*		addFeature(new FeatureTypesEachLabel(model, 
             new WindowFeatures(windows, new FeatureTypesConcat(model,
             new ConcatRegexFeatures(model,0,0), maxMemOverall))));		
             */		addFeature(new FeatureTypesEachLabel(this, 
                     new WindowFeatures(windows, new FeatureTypesMulti(
                             new ConcatRegexFeatures(this,0,0)))));
             
        }
    }
    public NestedFeatureGenImpl(int numLabels,java.util.Properties options) throws Exception {
        this(numLabels,options,true);
    }
    /**
     * @param modelSpecs
     * @param numLabels
     * @param addFeatureNow
     */
    public NestedFeatureGenImpl(String modelSpecs, int numLabels, boolean addFeatureNow) throws Exception {
        super(modelSpecs,numLabels,addFeatureNow);
    }

    public void startScanFeaturesAt(DataSequence data, int pos) {
        startScanFeaturesAt(data,pos-1,pos);
    }
    public int maxMemory() {
        return maxMemOverall;
    }
    public void setMaxMemory(int i) {
        maxMemOverall = i;
    }
    
    // we assume each label is associated with a maximum length for which it
    // is willing to output a grouped probability.  That is, different y-s
    // have different value of maxMem.
    public void startScanFeaturesAt(DataSequence d, int prevPos, int pos) {
        data = d;
        cposEnd = pos;
        cposStart = prevPos+1;
        for (int i = 0; i < features.size(); i++) {
            getFeature(i).startScanFeaturesAt(data,prevPos,cposEnd);
        }
        currentFeatureType = null;
        featureIter = features.iterator();
        advance();
        // if no word features activated, do not send the edge and
        // start/end features.
        /*	if ((currentFeatureType != getFeature(0)) && (cpos-prevPos > 1)) {
         featureToReturn.id = -1;
         }
         */
        
    }
    // TODO do not send any features where the maxMem property of the y
    // is violated.
};
