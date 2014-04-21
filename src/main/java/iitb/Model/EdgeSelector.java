/**
 * Created on Nov 9, 2008
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 * 
 * Feature that can be used to decide if an edge should be added between two words.
 */
package iitb.Model;

import iitb.CRF.DataSequence;

public class EdgeSelector extends RegexCountFeatures {
    /**
	 * 
	 */
	private static final long serialVersionUID = 3669485110606844964L;
	int windowSize=0;
    int segLen;
    int histSize;
    int currentHistSize;
    int minHist;
    public EdgeSelector(FeatureGenImpl fgen, int width, String patternFile, int histSize, int minHist) {
        super(fgen,2*width+2,patternFile);
        windowSize=width;
        this.histSize = histSize;
        assert(histSize >= 1);
    }
    public EdgeSelector(FeatureGenImpl fgen,String patternFile) {
        this(fgen,0,patternFile,1,0);
    }
    public EdgeSelector(FeatureGenImpl fgen,String patternFile, int histSize) {
        this(fgen,0,patternFile, histSize,0);
    }
    public EdgeSelector(FeatureGenImpl fgen) {
        this(fgen,0,null,1,0);
    }
    @Override
    public boolean hasNext() {
        return (segLen > 1) && super.hasNext();
    }

    @Override
    public void next(FeatureImpl f) {
        f.val = (float)patternOccurence[index]/segLen;
        assert(f.val>0);
        f.strId.id =  index*histSize+(currentHistSize-1);
        f.id = f.strId.id;
        f.ystart = -1;
        if(featureCollectMode()){
            f.strId.name = featureName(f.id);
            //System.out.println((String)f.strId.name +" " +index + " " + f.strId.id);
        }
        advance();
    }

    @Override
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        currentHistSize = pos-prevPos;
        assert(currentHistSize >=1);
        assert(currentHistSize <= histSize);
        segLen = Math.min(pos+windowSize,data.length()-1)-Math.max(pos-windowSize-histSize,0)+1;
        return super.startScanFeaturesAt(data, Math.max(pos-windowSize-histSize,0)-1, Math.min(pos+windowSize,data.length()-1));
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
        return "EdgeSel";
    }
    
    public String featureName(int index) {
        return name()+"_"+patternString[index/histSize][0]+((histSize > 1)?("_H"+(index % histSize)):"");
    }
	public int historySize() {
		return histSize;
	}
}
