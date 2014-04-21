/**
 * This is the feature that is fired for each position in the given window.
 * The window.minLength specify the minimum length of the segment for which
 * the feature at a particular position should be considered. The window.maxLength
 * similarly specifies the maximum segment length for which the feature
 * is to be fired.
 */

package iitb.BSegment;
import iitb.CRF.DataSequence;
import iitb.Model.FeatureTypes;
import iitb.Model.WindowFeatures;

import java.io.Serializable;

public class BWindowFeatureMultiNew extends WindowFeatures implements BoundaryFeatureFunctions {
	/**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;
    int currPos;
	int currBdry;
	int currWin;

	boolean DEBUG = false;

	transient DataSequence dataSeq;
	int dataLen;
	boolean allFeaturesFlag;
	boolean featureValid = false;
	boolean firstCall = true;

	int maxGap = 1;
	int minWinLength = 0;
    boolean directSegmentMode = false;


	/*
	 * The multifeature is fired for x(i) at various positions. 
	 * - posBdry stores the valid starting and ending position for a sequence.
	 * - featureBdry stores the valid starting and ending f._startB and f._endB values in a sequence.
	 * - fboundary stores the boundaries of the feature.
	 * - cfRange stores the indexes of the starting(or ending) of the segments for which the feature fired
	 *   at currPos will hold true.
	 */
	BFeatureImpl bfeatureImpl = new BFeatureImpl();
	Boundary posBdry = new Boundary();
	Boundary featureBdry = new Boundary();
	BFeatureImpl fboundary = new BFeatureImpl();
	Boundary cfRange = new Boundary();

	/*
	 * WIN_LEFT : Window defined over left end
	 * WIN_RIGHT : Window defined over right end
	 * WIN_MIDDLE : Window defined over both ends
	 */
	final static int WIN_LEFT	= 0;
	final static int WIN_RIGHT	= 1;
	final static int WIN_MIDDLE = 2;

	private class Boundary implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public int start;
		public int end;
		public Boundary() {	start = end = 0;}
		public String toString() {	return "Start : "+ start + " : End : "  + end; }
	};


	public BWindowFeatureMultiNew(Window twindows[], FeatureTypes tsingle) {
        super(twindows,tsingle);
		if(checkValidity(windows) == false) {
			System.exit(0);
		}
	}
    
	public boolean startScanFeaturesAt(DataSequence tdataSeq, int tprevPos, int tpos) {
		directSegmentMode = true;
        return super.startScanFeaturesAt(tdataSeq, tprevPos, tpos);
    }
	
	public boolean startScanFeaturesAt(DataSequence tdataSeq, int tpos) {
		directSegmentMode = false;
		dataSeq = tdataSeq;
		dataLen = dataSeq.length();
		currPos = tpos;
		
		if(checkWinLength(windows[currWin]) == false) {
			allFeaturesFlag = true;
			return false; 
		}
		
		allFeaturesFlag = false;
		init();
		
		if(currPos < posBdry.start || currPos > posBdry.end || single.startScanFeaturesAt(dataSeq, tpos) == false) {
			allFeaturesFlag = true;
			return false;
		}

		return advance();
	}

	
	private void init() {
		currWin = 0;

		Window twindow = windows[currWin]; 
		initBoundary(twindow);
		initFeatureOpenFlag(twindow, fboundary);
		

		featureValid = false;
		firstCall = true;
	}

	/*
	 * This method sets the range of the segments' boundaries that will have the feature fired at tpos
	 */
	private void initCurrFeaturePosBdry(Window twindow, int tpos) {
		switch(getWindowType(twindow)) {
			case WIN_LEFT:
			case WIN_RIGHT:
				cfRange.start = Math.max(featureBdry.start, tpos - twindow.end);
				cfRange.end = Math.min(featureBdry.end, tpos - twindow.start);
				maxGap = twindow.minLength;
				break;
			case WIN_MIDDLE:
				if(twindow.start >= 0 && twindow.end <= 0) {
					maxGap = twindow.start - twindow.end + 1;
					cfRange.start = tpos - twindow.start;
					cfRange.end = tpos - twindow.start;
				} else if(twindow.start > twindow.end) {
					maxGap = twindow.start - twindow.end + 1;	
					cfRange.start = tpos - twindow.start;
					cfRange.end = tpos - twindow.start;
					//TODO - To be checked if maxGap > 
				} else if(twindow.start < twindow.end) {
					maxGap = twindow.minLength;
					cfRange.start = Math.max(featureBdry.start, tpos - twindow.end - ((maxGap>1)?(maxGap - 1):0));
					cfRange.end = Math.min(featureBdry.end, tpos - twindow.start - ((maxGap>1)?(maxGap - 2):0));
				}
				break;
		}

		currBdry = cfRange.start - 1;
	}

	private void initFeatureOpenFlag(Window twindow, BFeatureImpl tfeatureImpl) {
		switch(getWindowType(twindow)) {
			case WIN_LEFT:
				tfeatureImpl._startOpen = false;
				tfeatureImpl._endOpen = (twindow.maxLength == Integer.MAX_VALUE);
				break;
			case WIN_RIGHT:
				tfeatureImpl._startOpen = (twindow.maxLength == Integer.MAX_VALUE);
				tfeatureImpl._endOpen = false;
				break;
			case WIN_MIDDLE:
				tfeatureImpl._startOpen = twindow.maxLength == Integer.MAX_VALUE;
				tfeatureImpl._endOpen = twindow.maxLength == Integer.MAX_VALUE;
				break;
			default:
				assert(false);
		}
	}

	private void initBoundary(Window twindow) {
		switch(getWindowType(twindow)) {
			case WIN_LEFT:
				posBdry.start = Math.max(0, twindow.start);
				posBdry.end = Math.min(dataLen - 1, dataLen - 1 - (twindow.minLength - 1) + twindow.end); 
				featureBdry.start = Math.max(0, -1 * twindow.end);
				featureBdry.end = Math.min(dataLen - 1 - twindow.minLength + 1, dataLen - 1 - twindow.start);
				minWinLength = getLeftWinMinLen(twindow);
				break;
			case WIN_RIGHT:
				posBdry.start = Math.max(0, twindow.minLength - 1 + twindow.start);
				posBdry.end = Math.min(dataLen - 1, dataLen - 1 + twindow.end);
				featureBdry.start = Math.max(twindow.minLength - 1, -1 * twindow.end);
				featureBdry.end = Math.min(dataLen - 1, dataLen - 1 - twindow.start);
				minWinLength = getRightWinMinLen(twindow);
				break;
			case WIN_MIDDLE:
				posBdry.start = Math.max(0, twindow.start);
				posBdry.end = Math.min(dataLen - 1, dataLen - 1 + twindow.end);
				featureBdry.start = Math.max(0, -1 * twindow.start);
				featureBdry.end = Math.min(dataLen - 1, dataLen - 1 - twindow.start);
				minWinLength = getMidWinMinLen(twindow);
				break;
		}
		
		if(posBdry.start > posBdry.end || featureBdry.start > featureBdry.end) {
			if(DEBUG) System.out.println("TODO : check the initBoundary method : " + posBdry + " : " + featureBdry);
		}

	}

	private boolean nextFeatureNew() {
		Window twindow = windows[currWin];
		currBdry++;
		if(currBdry <= cfRange.end) {
			switch(getWindowType(twindow)) {
				case WIN_LEFT:
					fboundary._startB = currBdry;
					fboundary._endB = fboundary._startB + minWinLength - 1;
					break;
				case WIN_RIGHT:
					fboundary._endB = currBdry;
					fboundary._startB = fboundary._endB - minWinLength + 1;
					break;
				case WIN_MIDDLE:
					fboundary._startB = currBdry;
					fboundary._endB = 	fboundary._startB + maxGap - 1;
					break;
			}
			return true;
		} else {
			featureValid = false;
			return false;
		}
	}

	private boolean advance() {
		if(featureValid && nextFeatureNew()) {
			return true;
		} else {
			if(single.hasNext()) {
				single.next(bfeatureImpl);
				initCurrFeaturePosBdry(windows[currWin], currPos);
				featureValid = true;
				return advance();
			} else {
				allFeaturesFlag = true;
				return false;
			}
		}
	}

    public boolean hasNext() {return directSegmentMode?super.hasNext():allFeaturesFlag==false;}

	public void next(BFeatureImpl f) {
        if (directSegmentMode) {
            super.next(f);
            return;
        }

		assignBoundary(bfeatureImpl, currPos);
		f.copy(bfeatureImpl);
		advance();
	}

	public int maxBoundaryGap() {
		return maxGap;
	}

	public void assignBoundary(BFeatureImpl tfeatureImpl, int tpos) {
		tfeatureImpl.copyBoundary(fboundary);
	}

	/*
	 * Methods for checking the validity of the parameters.
	 */
	private boolean checkValidity(Window twindowArr[]) {
		if(twindowArr.length > 1) {
			System.out.println("BWindowFeatureMulti : Only a single window is currently supported.");
			return false;
		}
		for(int i=0; i<twindowArr.length; i++) {
			Window twindow = twindowArr[i];
			switch(getWindowType(twindow)) {
				case WIN_LEFT:
				case WIN_RIGHT:
					if(twindow.maxLength < Integer.MAX_VALUE) {
						System.out.println("BWindowFeatureMulti : The maxLength of windows defined for either end can not be less than Integer.MAX_VALUE");
						return false;
					}
					break;
				case WIN_MIDDLE:
					if(twindow.start < 0 && twindow.end > 0) {
						System.out.println("BWindowFeatureMulti : The given window is not supported.");
						return false;
					}
					break;
			}
		}
		return true;	
	}

	private boolean checkWinLength(Window twindow) {
		switch(getWindowType(twindow)) {
			case WIN_LEFT:
				return getMinLenSeqLeftWin(twindow) <= dataLen && getLeftWinMinLen(twindow) <= twindow.maxLength;
			case WIN_RIGHT:
				return getMinLenSeqRightWin(twindow) <= dataLen && getRightWinMinLen(twindow) <= twindow.maxLength;
			case WIN_MIDDLE:
				return getMinLenSeqMidWin(twindow) <= dataLen && getMidWinMinLen(twindow) <= twindow.maxLength;
			default:
				assert(false);
				return false;
		}
	}
	
	/*
	 * A window should have atleast some minimum length for it to have* valid featrues.
	 */
	private int getMidWinMinLen(Window twindow) {
		return Math.max(twindow.minLength, twindow.start - twindow.end + 1);
	}

	private int getLeftWinMinLen(Window twindow) {
		return Math.max(twindow.minLength, twindow.start + 1);
	}

	private int getRightWinMinLen(Window twindow) {
		return Math.max(twindow.minLength, -1*twindow.end  + 1);
	}

	/*
	 * This returns the minimum length of a sequence in which the given window will be valid for some segment
	 */
	private int getMinLenSeqLeftWin(Window twindow) {
		int tminLen = getLeftWinMinLen(twindow);
		return Math.max(tminLen, tminLen - twindow.end);
	}

	private int getMinLenSeqRightWin(Window twindow) {
		int tminLen = getRightWinMinLen(twindow);
		return Math.max(tminLen, tminLen + twindow.start);
	}
	
	private int getMinLenSeqMidWin(Window twindow) {
		return Math.max(getMidWinMinLen(twindow), Math.max(twindow.start, -1*twindow.end)+1);
	}
	
	/*
	 * Get the type of the window
	 */
	int getWindowType(Window twindow) {
		if(twindow.startRelativeToLeft && twindow.endRelativeToLeft) {
			return WIN_LEFT;
		} else if(!twindow.startRelativeToLeft && !twindow.endRelativeToLeft) {
			return WIN_RIGHT;
		} else if(twindow.startRelativeToLeft && !twindow.endRelativeToLeft) {
			return WIN_MIDDLE;
		} else {
			assert(false);
			return -1;
		}
	}

	int winSize(Window twindow) {	
		return twindow.end - twindow.start + 1;
	}

	public boolean requiresTraining() {
		return super.requiresTraining();
	}

	public void train(DataSequence tdataSeq, int tpos) {
		super.train(tdataSeq, tpos);
	}
};
