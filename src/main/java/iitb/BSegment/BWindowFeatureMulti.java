/*
 * Created on Apr 13, 2005
 *
 */
package iitb.BSegment;

import iitb.CRF.DataSequence;
import iitb.Model.FeatureTypes;
import iitb.Model.WindowFeatures;

/**
 * @author sunita
 *
 * This assumes that the single features is to be fired independently for each position of the window.
 */
public class BWindowFeatureMulti extends WindowFeatures implements BoundaryFeatureFunctions {
    int maxGap = 0;
    boolean directSegmentMode=false;
    BFeatureImpl fImpl = new BFeatureImpl();
    BFeatureImpl temp = new BFeatureImpl();
    /**
     * @param arg0
     * @param arg1
     */
    public BWindowFeatureMulti(Window[] windows, FeatureTypes single) {
        super(windows,single);
        for (int i = windows.length-1; i >= 0; i--) {
            maxGap = Math.max(maxGap,windows[i].start - windows[i].end+1);
        }
    }
    private static final long serialVersionUID = 1L;
    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int)
     * all windows that overlap with this position
     * 
     */
    private void assignBoundaryInt(BFeatureImpl feature, int pos) {
        Window w = windows[currentWindow];
        if (w.startRelativeToLeft && w.endRelativeToLeft) {
            // TODO -- handle this case. for each value between w.start..w.end generate a different feature
            assert (pos-w.start == pos-w.end);
            feature._startB = pos - w.start;
            feature._endB = feature._startB+w.minLength-1;
            feature._startOpen = false;
            feature._endOpen = (w.maxLength == Integer.MAX_VALUE);
        } else if (w.startRelativeToLeft && !w.endRelativeToLeft) {
            feature._startB = pos - w.start;
            feature._endB = pos - w.end;
            feature._startOpen = (w.maxLength == Integer.MAX_VALUE);
            feature._endOpen = (w.maxLength == Integer.MAX_VALUE);
        } else if (!w.startRelativeToLeft && !w.endRelativeToLeft) {
            assert (pos-w.start == pos-w.end);
            feature._endB = pos - w.end;
            feature._startB = feature._endB-w.minLength+1;
            feature._startOpen = (w.maxLength == Integer.MAX_VALUE);
            feature._endOpen = false;
        } else assert(false); // invalid combination
    }
    /*
    public void assignBoundary(BFeatureImpl feature, int pos) {
        feature.copyBoundary(boundary);
        assert((boundary._startB >= 0) && (boundary._endB < dataSeq.length()));
    }
    */
    protected boolean advance() {
    	currentWindow--;
    	while ((currentWindow >= 0) || single.hasNext()) {
    		if ((currentWindow < 0) && single.hasNext()) {
    			currentWindow = windows.length-1;
    			single.next(fImpl);
    		}
    		for (; currentWindow >= 0;currentWindow--) {
    			int leftB = pos-windows[currentWindow].start;
    			int rightB = pos-windows[currentWindow].end;
    			// TODO --- this may not be right with start and end features with 
    			// a window of length > 1.
    			if (((leftB >= 0) && leftB < dataSeq.length())
    					&& ((rightB >= 0) && rightB < dataSeq.length())
    				) {
    			    assignBoundaryInt(fImpl,pos);
    				if ((fImpl._startB >= 0) && (fImpl._endB < dataSeq.length())) {
    				        return true;
    				}
    			}
    		}
    	}
    	return false;
    }
    public boolean startScanFeaturesAt(DataSequence arg, int pos) {
        directSegmentMode=false;
        this.pos = pos;
        dataSeq = arg;
        single.startScanFeaturesAt(arg,pos);
        currentWindow = 0;
        return advance();
    }
    public boolean hasNext() {return directSegmentMode?super.hasNext():(currentWindow >= 0);}
    BFeatureImpl boundary = new BFeatureImpl();
   
    public void next(BFeatureImpl f) {
        if (directSegmentMode) {
            super.next(f);
            return;
        }
        f.copy(fImpl);
//        assignBoundaryInt(f,pos);
        String name = "";
        if (featureCollectMode()) {
	        name += f.strId.name + ".W." + windows[currentWindow];
	    }
	    setFeatureIdentifier(f.strId.id*windows.length+currentWindow, f.strId.stateId, name, f);
	    
	    assert(!windows[currentWindow].toString().equals("unique") || (!f._startOpen && !f._endOpen && (f._startB == f._endB)));
	    assert(!windows[currentWindow].toString().equals("end") || (f._startOpen && !f._endOpen && (f._startB + 1 == f._endB)));
       // next((FeatureImpl)f);
//	    f.copyBoundary(boundary);
//	    assert((boundary._startB >= 0) && (boundary._endB < dataSeq.length()));
	    advance();
	}
    public int maxBoundaryGap() {return maxGap;}
    public boolean startScanFeaturesAt(DataSequence arg, int prevPos, int pos) {
        directSegmentMode=true;
        return super.startScanFeaturesAt(arg,prevPos,pos);
    }
}
