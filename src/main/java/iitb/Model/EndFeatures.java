package iitb.Model;
import iitb.CRF.*;

/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class EndFeatures  extends FeatureTypes {
    int stateId;
    int endStateNum;
    Object fname;
    public EndFeatures(FeatureGenImpl m) {
	super(m);
	fname="End.";
    }
    public EndFeatures(FeatureGenImpl m, Object fname) {
	super(m);
	this.fname = fname;
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
	if (pos < data.length()-1) {
	    stateId = -1;
	    return false;
	} else {
	    endStateNum = 0;
	    stateId = model.endState(endStateNum);	
	    return true;
	}
    }
    public boolean hasNext() {
	return (stateId >= 0);
    }
    public void next(FeatureImpl f) {
	setFeatureIdentifier(stateId,stateId,fname,f);
	f.yend = stateId;
	f.ystart = -1;
	f.val = 1;
	endStateNum++;
	stateId = model.endState(endStateNum);	
    }
};
