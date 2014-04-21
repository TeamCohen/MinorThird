/*
 * Created on Nov 10, 2008
 * @author sunita
 */
package iitb.Model;

import iitb.CRF.DataSequence;
import iitb.CRF.SegmentDataSequence;

/**
 * 
 * @author Sunita Sarawagi
 * @since 1.3
 * @version 1.3
 */
public class FeatureTypesEachEdge extends EdgeFeatures {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6906050783826384279L;
	FeatureTypes single;
    FeatureImpl featureImpl,edgeFeature;
    DataSequence data;
    int pos, prevPos;
    public FeatureTypesEachEdge(FeatureGenImpl fgen, FeatureTypes edgeLevelFeature) {
        super(fgen);
        this.single = edgeLevelFeature;
        featureImpl = new FeatureImpl();
        edgeFeature = new FeatureImpl();
        //thisTypeId = single.thisTypeId;
    }
    boolean advance() {
        if (!super.hasNext() && single.hasNext()){ 
            single.next(featureImpl);
            super.startScanFeaturesAt(data, prevPos, pos);
        } 
        return super.hasNext();
    }
    @Override
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        single.startScanFeaturesAt(data, prevPos, pos);
        if (single.hasNext()) {
            single.next(featureImpl);
            super.startScanFeaturesAt(data, prevPos, pos);
        }
        this.data=data;
        this.pos = pos;
        this.prevPos=prevPos;
        return advance();
    }
    @Override
    public void next(iitb.Model.FeatureImpl f) {
        super.next(edgeFeature);
        f.copy(featureImpl);
        f.yend = edgeFeature.yend;
        f.ystart = edgeFeature.ystart;
        assert(f.val > 0);
        Object name = f.strId.name;
        if (featureCollectMode()) {
            name = name+"_"+edgeFeature.strId.name;
        }
        assert(edgeNum <= model.numEdges());
        setFeatureIdentifier(f.strId.id * model.numEdges()+edgeNum-1,
                f.yend, name, f);
        advance();
    }
    
    @Override
    public boolean requiresTraining() {
        return single.requiresTraining();
    }
    @Override
    public void train(DataSequence data, int pos) {
        single.train(data, pos);
    }
    @Override
    public void train(SegmentDataSequence sequence, int segStart, int segEnd) {
        single.train(sequence, segStart, segEnd);
    }
    @Override
    public boolean fixedTransitionFeatures() {
        return false;//single.fixedTransitionFeatures();
    }
    @Override
    public boolean needsCaching() {
        return single.needsCaching();
    }
    @Override
    public boolean hasNext() {
        return (prevPos >= 0) && (super.hasNext() || single.hasNext());
    }
}