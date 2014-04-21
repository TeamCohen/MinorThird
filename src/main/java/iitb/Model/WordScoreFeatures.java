package iitb.Model;
import iitb.CRF.DataSequence;

/**
 * These return one feature per state.  The value of the feature is the
 * fraction of training instances passing through this state that contain
 * the word
 *
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */ 
public class WordScoreFeatures extends FeatureTypes {
    /**
	 * 
	 */
	private static final long serialVersionUID = 5855042861074710317L;
	int stateId;
    int wordPos;
    int wordCnt;
    int scoreType;
    int numScoreType=2;
    WordsInTrain dict;
    public WordScoreFeatures(FeatureGenImpl m, WordsInTrain d) {
        super(m);
        dict = d;
    }
    private void nextStateId() {
        stateId = dict.nextStateWithWord(wordPos, stateId);
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        stateId = -1;
        scoreType=0;
        wordCnt = dict.count(data.x(pos));
        if (wordCnt > WordFeatures.RARE_THRESHOLD) {
            Object token = (data.x(pos));
            wordPos = dict.getIndex(token);
            stateId = -1;
            nextStateId();
            return true;
        } 
        return false;
    }
    public boolean hasNext() {
        return (stateId < model.numStates()) && (stateId >= 0);
    }
    public void next(FeatureImpl f) {
        switch (scoreType) {
        case 0:
            //pr(stateId|w)
             
            //if (wordCnt > 1) {
                f.val = (float) Math.log(1 + (double)dict.count(wordPos,stateId)/(double)wordCnt);
                //f.val = (float) (dict.count(wordPos,stateId)/((double)wordCnt));
               break;
            //} else {
              // scoreType++;
            //}
        case 1:
            //Pr(stateId,smoothed|w)
            f.val = (float) ((1+(double)dict.count(wordPos,stateId))/((double)(wordCnt+model.numStates()) ));
            break;
        case 2:
            //Pr(w|statedId)
            f.val = (float)Math.log(1+((double)dict.count(wordPos,stateId))/dict.count(stateId));
            break;
        default:
            f.val = (float) (1 + Math.log(1+Math.log(dict.count(wordPos,stateId))));
            break;
        //f.val = (float) (1 + Math.log(1+Math.log(dict.count(wordPos,stateId))));
        }
        
        if (featureCollectMode())
            setFeatureIdentifier(stateId*numScoreType+scoreType,stateId,"WS_"+scoreType+"_"+stateId,f);
        else
            setFeatureIdentifier(stateId*numScoreType+scoreType,stateId,null,f);
        f.yend = stateId;
        f.ystart = -1;
        
        //for (int s = 0; s < model.numStates(); s = dict.nextStateWithWord(wordPos, s)) {}
        // System.out.println(f.toString());
        if (scoreType<numScoreType-1) {
            scoreType++;
        } else {
            scoreType=0;
            nextStateId();
        }
    }
};


