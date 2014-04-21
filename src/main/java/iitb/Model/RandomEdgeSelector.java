/*
 * Created on Nov 9, 2008
 * @author sunita
 * 
 * Feature that can be used to decide if an edge should be added between two words.
 */
package iitb.Model;

import java.util.Random;

import iitb.CRF.DataSequence;
import iitb.CRF.KeyedDataSequence;

public class RandomEdgeSelector extends EdgeSelector {
    Random rand;
    public RandomEdgeSelector(FeatureGenImpl fgen, int width, String patternFile, int histSize) {
        super(fgen,width,patternFile,histSize,0);
        windowSize=width;
        this.histSize = histSize;
        assert(histSize >= 1);
        rand = new Random();
    }
    public RandomEdgeSelector(FeatureGenImpl fgen,String patternFile) {
        this(fgen,0,patternFile,1);
    }
    public RandomEdgeSelector(FeatureGenImpl fgen,String patternFile, int histSize) {
        this(fgen,0,patternFile, histSize);
    }
    public RandomEdgeSelector(FeatureGenImpl fgen) {
        this(fgen,0,null,1);
    }
    @Override
    public boolean hasNext() {
        return (index < patternOccurence.length) && super.hasNext();
    }

    @Override
    public void next(FeatureImpl f) {
        f.val = 1;
        f.strId.id =  index*histSize+(currentHistSize-1);
        f.id = f.strId.id;
        f.ystart = -1;
        if(featureCollectMode()){
            f.strId.name = featureName(f.id);
        }
        advance();
    }
    @Override
    protected boolean advance() { 
        index += rand.nextInt(2);
        return hasNext();
    }

    @Override
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        currentHistSize = pos-prevPos;
        assert(currentHistSize >=1);
        assert(currentHistSize <= histSize);
        segLen = Math.min(pos+windowSize,data.length()-1)-Math.max(pos-windowSize-histSize,0)+1;
        index = 0;
        rand.setSeed(((KeyedDataSequence)data).getKey()*data.length()+pos);
        return advance();
    }
    @Override
    public int labelIndependentId(FeatureImpl f) {
        return f.id;
    }

    @Override
    public int maxFeatureId() {
        return patternString.length*histSize;
    }

    @Override
    public String name() {
        return "RandomEdgeSel";
    }
    
    public String featureName(int index) {
        return name()+"_"+patternString[index/histSize][0]+((histSize > 1)?("_H"+histSize):"");
    }
}
