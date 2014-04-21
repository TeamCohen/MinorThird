/*
 * Created on May 4, 2008
 * @author sunita
 */
package iitb.Model;

import iitb.CRF.DataSequence;

/**
 * 
 * @author Sunita Sarawagi
 * @since 1.3
 * @version 1.3
 */
public class TokenShapeFeatures extends FeatureTypes {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7648807821467697829L;
	char pattern[][] = {
            {'a','z','x'},
            {'A','Z','X'},
            {'0','9','d'},
            {'.','.','.'},
            {',',',',','},
            {'-','-','-'},
            {'\'','\'','\''},
            {'"','"','"'},
            {'o','o','o'}
            };
    String allowedPatterns[]={
            "X", "XX", "XXX", "XXX+"
            ,"x","xx","xxx","xxx+"
            ,"Xx", "Xxx", "Xxx+"
            ,".", ",", "-"
            ,"X."
            ,"1", "11", "111", "111+"
            ,"1.1"
            ,"1,1+", "11,+", "1.1+", "11.+","11-+","11#"
            ,"'s", "\"", "'"
            };
    int idToFeatureIdMap[];
    int numPos=3;
    String word=null;
    
    public TokenShapeFeatures(FeatureGenImpl fgen) {
        super(fgen);
        int maxPatternId = (int) Math.pow(pattern.length+1,numPos)*2;
        idToFeatureIdMap = new int[maxPatternId];
        for (int i = 0; i < allowedPatterns.length; i++) {
            int id = getPatternId(allowedPatterns[i]);
            idToFeatureIdMap[id] = i+1;
        }
    }
    private int getPatternId(String word) {
        int len = word.length();
        int id=0;
//        if (featureCollectMode() && (f != null)) f.strId.name=name();
        for (int i = 0; i < numPos; i++) {
            int matchPos=(i < len)?findMatchPosition(word.charAt(i)):pattern.length;
            id = matchPos + id*(pattern.length+1);
  /*          if (featureCollectMode() && (f != null)) {
                if (matchPos < pattern.length) {
                    f.strId.name = (String)f.strId.name + pattern[matchPos][2];
                }
            }
    */    }
        // indicator for whether the word is > numPos
        id *= 2;
        if (len > numPos) {
            id += 1;
      //      if (featureCollectMode() && (f != null)) 
        //        f.strId.name = f.strId.name+"+";
        }
        return id;
    }
    @Override
    public boolean hasNext() {
        return (word != null);
    }
    @Override
    public void next(FeatureImpl f) {
        f.strId.id = idToFeatureIdMap[getPatternId(word)];
        if (featureCollectMode()) {
            if (f.strId.id > 0)
                f.strId.name = allowedPatterns[f.strId.id-1];
            else
                f.strId.name = "Other";
            
           // System.out.println(word + " "+f.strId.name);
        }
        f.val=1;
        word=null;
    }
    private int findMatchPosition(char c) {
        for (int i = 0; i < pattern.length-1; i++) {
            if ((pattern[i][0] <= c) && (pattern[i][1] >= c))
                return i;
        }
        return pattern.length-1;
    }

    @Override
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        assert(pos-prevPos==1);
        word = data.x(pos).toString();
        return hasNext();
    }

    @Override
    public int maxFeatureId() {
        return allowedPatterns.length+1;
    }
    @Override
    public String name() {
        return "Shape_";
    }
}
