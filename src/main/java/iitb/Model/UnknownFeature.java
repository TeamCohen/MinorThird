/** UnknownFeature.java
 * 
 * @author Sunita Sarawagi
 * @version 1.3
 */
package iitb.Model;
import iitb.CRF.DataSequence;

/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class UnknownFeature extends FeatureTypes {
    private static final long serialVersionUID = 6122L;
    int stateId;
    WordsInTrain dict;
    int numStates;
    public UnknownFeature(FeatureGenImpl m, WordsInTrain d) {
        this(m,d,true);
    }
    public UnknownFeature(FeatureGenImpl m, WordsInTrain d,boolean assignStateIds) {
        super(m);
        dict = d;
        if (assignStateIds)
            numStates = m.numStates();
        else
            numStates=1;
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        if (dict.count(data.x(pos)) > WordFeatures.RARE_THRESHOLD+1) {
            stateId = numStates;
            return false;
        } else {
            stateId = 0;
            return true;
        }
    }
    public boolean hasNext() {
        return (stateId < numStates);
    }
    public void next(FeatureImpl f) {
        setFeatureIdentifier(stateId,stateId,"U",f);
        f.yend = stateId;
        f.ystart = -1;
        f.val = 1;
        stateId++;
    }
    public int maxFeatureId() {
        return numStates;
    }
};


