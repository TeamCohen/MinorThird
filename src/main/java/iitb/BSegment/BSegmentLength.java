/*
 * Created on Apr 28, 2005
 *
 */
package iitb.BSegment;

import iitb.CRF.DataSequence;
import iitb.Model.FeatureGenImpl;
import iitb.Model.FeatureTypesSegmentLength;

/**
 * @author sunita
 *
 */
public class BSegmentLength extends FeatureTypesSegmentLength implements BoundaryFeatureFunctions {
    private static final long serialVersionUID = 1L;
    int pos;
    int len;
    boolean directSegmentMode=false;
    /**
     * @param fgen
     */
    public BSegmentLength(FeatureGenImpl fgen, int maxLen) {
        super(fgen,maxLen);
    }

    /* (non-Javadoc)
     * @see iitb.BSegment.BFeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence)
     */
    public boolean startScanFeaturesAt(DataSequence arg, int pos) {
        directSegmentMode=false;
        len = Math.min(maxLen,pos+1);
        super.startScanFeaturesAt(null,pos-len,pos);
        this.pos = pos;
        return hasNext();
    }

    /* (non-Javadoc)
     * @see iitb.BSegment.BFeatureTypes#next(iitb.BSegment.BFeatureImpl)
     */
    BFeatureImpl boundary = new BFeatureImpl();
    public void next(BFeatureImpl f) {
        if (directSegmentMode) {
            super.next(f);
            return;
        }
        if (len==maxLen) {
            boundary._endOpen = true;
        } else {
            boundary._endOpen = false;
        }
        boundary._startOpen = false;
        boundary._startB = pos-len+1;
        boundary._endB = pos;
        super.next(f);
        f.copyBoundary(boundary);
        len--;
        super.startScanFeaturesAt(null,pos-len,pos);
    }

    /* (non-Javadoc)
     * @see iitb.BSegment.BFeatureTypes#maxBoundaryGap()
     */
    public int maxBoundaryGap() {
        return maxLen;
    }

    /* (non-Javadoc)
     * @see iitb.BSegment.BFeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence)
     */
    public boolean startScanFeaturesAt(DataSequence arg) {
        // this should not be called.
        assert(false);
        return false;
    }

    /* (non-Javadoc)
     * @see iitb.BSegment.BFeatureEachPosition.BoundaryType#assignBoundary(iitb.BSegment.BFeatureImpl, int)
     */
/*    public void assignBoundary(BFeatureImpl feature, int pos) {
        feature.copyBoundary(boundary);
    }
    */
    public boolean startScanFeaturesAt(DataSequence arg, int prevPos, int pos) {
        directSegmentMode=true;
        return super.startScanFeaturesAt(arg,prevPos,pos);
    }
}
