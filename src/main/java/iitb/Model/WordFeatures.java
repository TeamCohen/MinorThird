/** WordFeatures.java
 *
 * @author Sunita Sarawagi
 * @since 1.1
 * @version 1.3
 */ 
package iitb.Model;
import iitb.CRF.DataSequence;

public class WordFeatures extends FeatureTypes {
    /**
	 * 
	 */
	private static final long serialVersionUID = -202366673127245027L;
	protected int stateId;
    int statePos;
    Object token;
    int tokenId;
    protected WordsInTrain dict;
    int _numWordStatePairs;
    public static int RARE_THRESHOLD=0;
    protected int frequency_cutOff;
    boolean assignStateIds=true;
    int numStates;
    public WordFeatures(FeatureGenImpl m, WordsInTrain d) {
        super(m);
        dict = d;
        frequency_cutOff=RARE_THRESHOLD;
        numStates = m.numStates();
    }
    public WordFeatures(FeatureGenImpl m, WordsInTrain d, int freqCuttOff) {
        this(m,d);
        if (freqCuttOff >= 0) frequency_cutOff=freqCuttOff;
    }
    public WordFeatures(FeatureGenImpl m, WordsInTrain d, int freqCuttOff, boolean assignStateIds) {
        this(m,d);
        if (freqCuttOff >= 0) frequency_cutOff=freqCuttOff;
        this.assignStateIds=assignStateIds;
        if (assignStateIds==false)
            numStates=1;
    }
    private void nextStateId() {       
        stateId = dict.nextStateWithWord(token, stateId);
        statePos++;
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        stateId = -1;
        if (dict.count(data.x(pos)) > frequency_cutOff) {
            tokenId = dict.getIndex(data.x(pos));
            token = data.x(pos);
            if (assignStateIds) {
                statePos = -1;
                nextStateId();
            } else {
                stateId=0;
            }
            return true;
        } 
        return false;
    }
    public boolean hasNext() {
        return (stateId != -1);
    }
    public void next(FeatureImpl f) {
        if (assignStateIds) {
            if (featureCollectMode())
                setFeatureIdentifier(tokenId*numStates+stateId,stateId,name()+dict.getKey(token),f);
            else
                setFeatureIdentifier(tokenId*numStates+stateId,stateId,null,f); 
            f.yend = stateId;
            nextStateId();
        } else {
            f.yend = 0;
            if (featureCollectMode())
                f.strId.name = name()+dict.getKey(token);
            f.strId.id = tokenId;
            stateId=-1;
        }
        f.ystart = -1;
        f.val = 1;
    }
    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#maxFeatureId()
     */
    public int maxFeatureId() {
        return dict.dictionaryLength()*numStates;
    }
    public String name() {
        return "W_";
    }
};


