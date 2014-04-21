/*
 * Created on Mar 21, 2005
 *
 */
package iitb.Model;

import iitb.CRF.DataSequence;

/**
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
public class FeatureTypesPosition extends FeatureTypes {
    /**
	 * 
	 */
	private static final long serialVersionUID = -9069993803665666842L;
	static byte maxParams=3;
    FeatureTypes ftype;
    byte currentParamId; // send three terms per feature: constant, linear, square.
    int segStart;
    int segEnd;
    int currPos;
    FeatureImpl savedFeature = new FeatureImpl();
    int dataLen;
    transient DataSequence dataSeq;
    public FeatureTypesPosition(FeatureGenImpl fgen, FeatureTypes ftype) {
        super(fgen);
        this.ftype = ftype;
    }
    void advance() {
        currentParamId++;
        if (currentParamId<maxParams)
            return;
        while (true) {
            if ((currPos >= segStart) && ftype.hasNext()) {
                currentParamId=0;
                return;
            }
            currPos++;
            if (currPos > segEnd)
                return;
            ftype.startScanFeaturesAt(dataSeq,currPos-1,currPos);
        }
    }
    public  boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        segStart = prevPos+1;
        segEnd = pos;
        currPos = segStart-1;
        currentParamId = maxParams;
        dataSeq = data;
        dataLen = data.length();
        advance();
        return ftype.hasNext();
    }
    public boolean hasNext() {
        return (currentParamId < maxParams) || ((currPos <= segEnd) && ftype.hasNext());
    }
    public void next(FeatureImpl f) {
        if (currentParamId==0) {
            ftype.next(f);
            savedFeature.copy(f);
        } else {
            f.copy(savedFeature);
            switch (currentParamId) {
            case 1:
                f.val = currPos-segStart+1; // (float)(currPos-segStart+1)/(segEnd-segStart+1); //dataLen;
                break;
            case 3:
                f.val = segEnd-currPos+1;
                break;
            default:
                f.val *= f.val; 
            }
        } 
        String name="";
        if (featureCollectMode()) {
            name = "POS^" + currentParamId +  f.strId.name;
        }
        setFeatureIdentifier(maxParams*f.strId.id+currentParamId,f.strId.stateId, name, f);
        advance();
    }
    public boolean requiresTraining() {
		return ftype.requiresTraining();
	}
	public void train(DataSequence data, int pos) {
		ftype.train(data, pos);
	}
	public int labelIndependentId(FeatureImpl f) {
		return ftype.labelIndependentId(f);
	}
	public int maxFeatureId() {
		return ftype.maxFeatureId()*maxParams;
	}
}
