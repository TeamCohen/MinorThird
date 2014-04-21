/*
 * Created on Dec 6, 2004
 *
 */
package iitb.Model;

import java.io.Serializable;

import iitb.CRF.DataSequence;
import iitb.CRF.SegmentDataSequence;

/**
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 *
 * Define features for windows of ranges.
 */

public class WindowFeatures extends FeatureTypes  {
    private static final long serialVersionUID = 6123;
    protected FeatureTypes single;
	protected int currentWindow;
	int prevPos;
	protected int pos;
	protected transient DataSequence dataSeq;
	
	public static class Window implements Serializable {
	    /**
		 * 
		 */
		private static final long serialVersionUID = -3237732969557838010L;
		public int start;
	    public boolean startRelativeToLeft;
	    public int end;
	    public boolean endRelativeToLeft;
	    String winName=null;
	    public int maxLength=Integer.MAX_VALUE;
	    public int minLength=1;
	       public Window(int start, boolean startRelativeToLeft, int end,
                boolean endRelativeToLeft) {
			this(start,startRelativeToLeft,end,endRelativeToLeft,null);
	       	String startB = startRelativeToLeft?"L":"R";
	       	String endB = endRelativeToLeft?"L":"R";
	       	winName = startB + start + endB + end;
	    }	
        public Window(int start, boolean startRelativeToLeft, int end,
                boolean endRelativeToLeft, String winName) {
            this.start = start;
            this.startRelativeToLeft = startRelativeToLeft;
            this.end = end;
            this.endRelativeToLeft = endRelativeToLeft;
            this.winName = winName;
        }
        public Window(int start, boolean startRelativeToLeft, int end,
                boolean endRelativeToLeft, String winName, int minWinLength, int maxWinLength) {
            this(start,startRelativeToLeft,end,endRelativeToLeft,winName);
            this.maxLength = maxWinLength;
            this.minLength = minWinLength;
        }
        
        int leftBoundary(int segStart, int segEnd) {
            if (startRelativeToLeft)
                return boundary(segStart,start);
             return boundary(segEnd,start);
        }

        int rightBoundary(int segStart, int segEnd) {
            if (endRelativeToLeft)
                return boundary(segStart,end);
             return boundary(segEnd,end);
        }

        /**
         * 
         * @param boundary
         * @param offset
         * @return
         */
        private int boundary(int boundary, int offset) {
            return boundary+offset;
        }
        public String toString() {
        	return winName;
        }
	}
	protected Window windows[];
    private int dataLen;
	/**
	 * 
	 */
	public WindowFeatures(Window windows[], FeatureTypes single) {
		super(single);
		this.single = single;
		this.windows = windows;
	}

	protected boolean advance(boolean firstCall) {
	    while (firstCall || !single.hasNext()) {
	        currentWindow--;
	        if (currentWindow < 0)
	            return false;
	        if ((windows[currentWindow].maxLength < pos-prevPos) || (windows[currentWindow].minLength > pos-prevPos))
	            continue;
	        int rightB = windows[currentWindow].rightBoundary(prevPos+1,pos);
	        int leftB = windows[currentWindow].leftBoundary(prevPos+1,pos);
	 
	        if ((leftB < dataLen) && (rightB >= 0) && (leftB <= rightB)) {
	            single.startScanFeaturesAt(dataSeq,Math.max(leftB,0)-1, Math.min(rightB,dataLen-1));
	            firstCall = false;
	        }
	    }
	    return true;
	}
	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
	 */
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	    currentWindow = windows.length;
	    dataSeq = data;
	    dataLen = dataSeq.length();
	    this.prevPos = prevPos;
	    this.pos = pos;
		return advance(true);
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#hasNext()
	 */
	public boolean hasNext() {
		return single.hasNext() && (currentWindow >= 0);
	}

	/* (non-Javadoc)
	 * @see iitb.Model.FeatureTypes#next(iitb.Model.FeatureImpl)
	 */
	public void next(FeatureImpl f) {
	    single.next(f);
	    String name = "";
	    if (featureCollectMode()) {
	        name += f.strId.name + ".W." + windows[currentWindow];
	    }
	    setFeatureIdentifier(f.strId.id*windows.length+currentWindow, f.strId.stateId, name, f);
	    advance(false);
	}

	public boolean requiresTraining() {
		return single.requiresTraining();
	}
	public void train(DataSequence data, int pos) {
		single.train(data, pos);
	}
     public void train(SegmentDataSequence sequence, int segStart, int segEnd) {
            single.train(sequence, segStart, segEnd);
     }
    
	public boolean needsCaching() {
        return single.needsCaching();
	}
}
