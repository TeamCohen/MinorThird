/*
 * Created on Dec 6, 2004
 *
 */
package iitb.Model;

import iitb.CRF.DataSequence;
import iitb.CRF.SegmentDataSequence;
import iitb.Utils.*;
/**
 * @author Administrator
 *
 */
public class FeatureTypesConcat extends FeatureTypes {
	FeatureTypes single;
	int numBits = 0;
	FeatureImpl feature = new FeatureImpl();
	private static final long serialVersionUID = 612L;
	private int maxConcatLength;
	
	/**
	 * @param m
	 */
	public FeatureTypesConcat(FeatureGenImpl fgen, FeatureTypes single, int maxMemory) {
		super(fgen);
		this.single = single;
        maxConcatLength = maxMemory;
        thisTypeId = single.thisTypeId;
        numBits = 0;
		
	}

    void setNumBits() {
        if (numBits > 0) return;
        int maxId = single.maxFeatureId()+1; // for the feature not firing.
        numBits = Utils.log2Ceil(maxId);
        if (maxConcatLength*numBits > Integer.SIZE) {
            System.out.println("Cannot handle larger than " + Integer.SIZE/numBits 
                    + " long segments. Resetting to this value");
            maxConcatLength = Integer.SIZE/numBits;
        }
    }
	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
	 */
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		int bitMap = 0;
		String name = "C";
		feature.strId.id=0;
        setNumBits();
		if (pos-prevPos > maxConcatLength)
		    return false;
		for (int i = 0; (i < pos-prevPos) && (i < maxConcatLength); i++) {
			if (single.startScanFeaturesAt(data,pos-i-1,pos-i) && single.hasNext()) {
				single.next(feature);
				// this could be wrong since label information is not present in single.
				//int thisId = single.offsetLabelIndependentId(feature);
                
                // 02 May '08: Replaced above with this which is correct but will not work for label tied base features..
                int thisId=feature.strId.id+1; // to distinguish from the feature not firing..
				bitMap = bitMap | (thisId << i*numBits);
				if (featureCollectMode()) {
					name = feature.strId.name + "_" + name;
					if (thisId > (1 << numBits)) {
						System.out.println("Error in max-feature-id value " + feature);
					}
					if (single.hasNext()) {
					    //						System.out.println("FeatureTypesConcat: Taking only the first feature: others to be ignored");
					}
				}
			}
		}
		//setFeatureIdentifier(bitMap,feature.strId.stateId,name,feature);
        feature.strId.id=bitMap;
        feature.strId.name=name;
		return (bitMap != 0);
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#hasNext()
	 */
	public boolean hasNext() {
		return feature.strId.id > 0;
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#next(iitb.Model.FeatureImpl)
	 */
	public void next(FeatureImpl f) {
		f.copy(feature);
		feature.strId.id = -1;
	}

	public boolean requiresTraining() {
		return single.requiresTraining();
	}
	public void train(DataSequence data, int pos) {
		single.train(data, pos);
	}
    public void train(SegmentDataSequence data, int prevPos, int pos) {
        single.train(data, prevPos, pos);
    }
    @Override
    public void trainingDone() {
        single.trainingDone();
    }
}
