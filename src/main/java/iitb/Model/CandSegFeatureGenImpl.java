/** CandSegFeatureGenImpl.java
 * Created on May 5, 2005
 *
 * @author Sunita Sarawagi
 * @version 1.3
 */
package iitb.Model;

import iitb.CRF.CandSegDataSequence;
import iitb.CRF.DataSequence;

public class CandSegFeatureGenImpl extends NestedFeatureGenImpl {
    /**
	 * 
	 */
	private static final long serialVersionUID = 948213974126007086L;
	boolean length1SegsForOther=false;
    int otherLabel = -1;
    public CandSegFeatureGenImpl(String modelSpecs, int numLabels,
            boolean addFeatureNow) throws Exception {
        super(modelSpecs, numLabels, addFeatureNow);
    }
    public CandSegFeatureGenImpl(int numLabels,java.util.Properties options, boolean addFeatureNow) throws Exception {
        super(numLabels,options,addFeatureNow);
    }
    public CandSegFeatureGenImpl(String modelSpecs, int numLabels,
            boolean addFeatureNow, int otherLabel) throws Exception {
        super(modelSpecs, numLabels, addFeatureNow);
        this.otherLabel = otherLabel;
        length1SegsForOther = (otherLabel >= 0);
        //length1SegsForOther = false;
    }
    /**
     * @param numLabels
     * @param options
     * @param addFeatureNow
     * @throws Exception
     */
   
    protected boolean retainFeature(DataSequence seq, FeatureImpl f) {
        return ((CandSegDataSequence)seq).holdsInTrainingData(f,cposStart-1,cposEnd);
    }
    public int addTrainRecord(DataSequence data) {
      //  if (!addOnlyTrainFeatures) {
        int numF = 0;
            CandSegDataSequence dataRecord = (CandSegDataSequence)data;
            for (int segEnd = dataRecord.length()-1; segEnd >= 0; segEnd--) {
                for (int nc = dataRecord.numCandSegmentsEndingAt(segEnd)-1; nc >= 0; nc--) {
                    int segStart = dataRecord.candSegmentStart(segEnd,nc);
                    startScanFeaturesAt(dataRecord, segStart-1, segEnd);
                    while (hasNext()) {
                        next();
                        numF++;
                    }		                    
                } 
            }
       /* } else {
            SegmentDataSequence seq = (SegmentDataSequence)data;
            int segEnd;
            for (int l = 0; l < seq.length(); l = segEnd+1) {
                segEnd = seq.getSegmentEnd(l);
                for (startScanFeaturesAt(seq,l-1,segEnd); hasNext(); next());
            }
        } */
            return numF;
    }
    @Override
    protected void advance() {
        super.advance();
        while (hasNext() && length1SegsForOther && (cposEnd>cposStart) && (featureToReturn.y()==otherLabel)) {
            super.advance();
        }
    }
    @Override
    public boolean fixedTransitionFeatures() {
        return !length1SegsForOther && super.fixedTransitionFeatures();
    }
}