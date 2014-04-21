/*
 * Created on Apr 25, 2005
 *
 */
package iitb.BSegment;

import iitb.CRF.CandSegDataSequence;
import iitb.CRF.DataSequence;
import iitb.CRF.SegmentDataSequence;
import iitb.Model.FeatureImpl;
import iitb.Model.FeatureTypes;

/**
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
public class BSegmentFeatureType extends BFeatureTypes {
   
    /**
	 * 
	 */
	private static final long serialVersionUID = 3343700357935882340L;
	FeatureTypes single;
    transient CandSegDataSequence candSegs;
    int maxMemory;
    int segEnd;
    int numSegs;
    int segStart;
    int currentSeg;
    int dataLen;
    boolean _hasNext;
    /**
     * @param single
     */
    public BSegmentFeatureType(FeatureTypes single, int maxMemory) {
        super(single);
        this.single = single;
        this.maxMemory = maxMemory;
    }
    
    boolean advance() {
        while (true) {
            if ((currentSeg >= 0) && single.hasNext())
                return true;
            currentSeg++;
            if (currentSeg < numSegs) {
                int segStart = candSegs.candSegmentStart(segEnd,currentSeg);
             //   if (segEnd-segStart+1 <= maxMemory+1)
                    single.startScanFeaturesAt(candSegs,segStart-1,segEnd);
            } else {
                segEnd++;
                if (segEnd < dataLen) {
                    currentSeg = -1;
                    numSegs =  candSegs.numCandSegmentsEndingAt(segEnd);
                } else {
                    return false;
                }
            }
        }	
    }
    /* (non-Javadoc)
     * @see iitb.BSegment.BFeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence)
     */
    public boolean startScanFeaturesAt(DataSequence data) {
        candSegs = (CandSegDataSequence)data;
        currentSeg = -1;
        numSegs = 0;
        segEnd = -1;
        dataLen = data.length();
        _hasNext = advance();
        return _hasNext;
    }

    /* (non-Javadoc)
     * @see iitb.BSegment.BFeatureTypes#next(iitb.BSegment.BFeatureImpl)
     */
    public void next(BFeatureImpl f) {
        single.next(f);
        f._startB = candSegs.candSegmentStart(segEnd,currentSeg);
        f._endB = segEnd;
        f._endOpen = false;
        f._startOpen = false; 
        if (f._endB - f._startB+1 >= maxMemory) {
            f._endB = f._startB+maxMemory-1;
            f._endOpen = true;
        } 
        _hasNext = advance();
        assert((f._startB <= f._endB) && (f._endB < dataLen)); 
    }

    /* (non-Javadoc)
     * @see iitb.BSegment.BFeatureTypes#maxBoundaryGap()
     */
    public int maxBoundaryGap() {
        return maxMemory;
    }

    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#hasNext()
     */
    public boolean hasNext() {
        return _hasNext;
    }
    public void assignBoundary(BFeatureImpl feature, int pos) {
        ;
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        _hasNext = single.startScanFeaturesAt(data,prevPos,pos);
        return hasNext();
    }
    public void next(FeatureImpl f) {
        single.next(f);
        _hasNext = single.hasNext();
    }
    public boolean needsCaching() {
        return single.needsCaching();
    }
    @Override
    public boolean requiresTraining() {
        return single.requiresTraining();
    }

    @Override
    public void train(DataSequence data, int pos) {
        single.train(data, pos);
    }

    @Override
    public void train(SegmentDataSequence sequence, int segStart, int segEnd) {
        single.train(sequence, segStart, segEnd);
    }

    @Override
    public void trainingDone() {
        single.trainingDone();
    } 
}