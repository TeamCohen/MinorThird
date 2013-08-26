/*
 * Created on Apr 12, 2005
 *
 */
package iitb.BSegment;

import iitb.CRF.DataSequence;
import iitb.Model.FeatureImpl;
import iitb.Model.FeatureTypes;

import java.io.Serializable;

/**
 * @author sunita
 * @since 1.2
 * @version 1.3
 */
public class BFeatureEachPosition extends BFeatureTypes {
    /**
	 * 
	 */
	private static final long serialVersionUID = -5443203053730870354L;
	int pos;
    FeatureTypes single;
    transient DataSequence data;
    static class TypePos implements BoundaryFeatureFunctions, Serializable {
        FeatureTypes ftype;
        int pos;
        private static final long serialVersionUID = 1L;
        TypePos(FeatureTypes ftype) {
            this.ftype = ftype;
        }
        public void next(BFeatureImpl feature) {
            ftype.next(feature);
            assignBoundary(feature,pos);
        }
        public void assignBoundary(BFeatureImpl feature, int pos) {
          feature._startOpen = true;
          feature._endOpen = true;
          feature._endB = feature._startB = pos;
      }
      public int maxBoundaryGap() {return 1;}
    /* (non-Javadoc)
     * @see iitb.BSegment.BoundaryFeatureFunctions#startScanFeaturesAt(iitb.CRF.DataSequence, int)
     */
    public boolean startScanFeaturesAt(DataSequence data, int pos) {
        this.pos=pos;
        return ftype.startScanFeaturesAt(data,pos);
    }
    };
    public static class TypePosEndOpen extends TypePos {
        /**
		 * 
		 */
		private static final long serialVersionUID = 7566849608695659821L;

		/**
         * @param ftype
         */
        public TypePosEndOpen(FeatureTypes ftype) {
            super(ftype);
        }

        public void assignBoundary(BFeatureImpl feature, int pos) {
            super.assignBoundary(feature,pos);
            feature._startOpen = false;
            feature._endOpen = true;
        }
    };
    public static class TypePosStartOpen extends TypePos {
        /**
		 * 
		 */
		private static final long serialVersionUID = -3328637175240373276L;

		/**
         * @param ftype
         */
        public TypePosStartOpen(FeatureTypes ftype) {
            super(ftype);
        }

        public void assignBoundary(BFeatureImpl feature, int pos) {
            super.assignBoundary(feature,pos);
            feature._startOpen = true;
            feature._endOpen = false;
        }
    };
    BoundaryFeatureFunctions type;
    /**
     * 
     */
    public BFeatureEachPosition(FeatureTypes single, BoundaryFeatureFunctions type) {
        super(single);
        this.single = single;
        this.type = type;
    }
    public BFeatureEachPosition(FeatureTypes single) {
        this(single,(BoundaryFeatureFunctions)single);
    }
    public BFeatureEachPosition(TypePos type) {
        this(type.ftype,type);
    }
    /* (non-Javadoc)
     * @see iitb.BSegmentCRF.BFeatureGenerator#startScanFeaturesAt(iitb.BSegmentCRF.BDataSequence)
     */
    public boolean startScanFeaturesAt(DataSequence data) {
        pos = data.length()-1;
        this.data = data;
        type.startScanFeaturesAt(data,pos);
        return advance();
    }
    /**
     * 
     */
    protected boolean advance() {
        while (!single.hasNext()) {
            pos--;
            if (pos < 0)
                return false;
            type.startScanFeaturesAt(data,pos);
        }
        return true;
    }
    /* (non-Javadoc)
     * @see iitb.BSegmentCRF.BFeatureGenerator#hasNext()
     */
    public boolean hasNext() {
        return single.hasNext();
    }
    public void next(BFeatureImpl feature) {
        //single.next(feature);
        //type.assignBoundary(feature,pos);
        type.next(feature);
        advance();
    }
    public int maxBoundaryGap() {return type.maxBoundaryGap();}
    /* (non-Javadoc)
     * @see iitb.BSegment.BoundaryFeatureFunctions#assignBoundary(iitb.BSegment.BFeatureImpl, int)
     */
    public void assignBoundary(BFeatureImpl feature, int pos) {
        // TODO Auto-generated method stub
        
    }
    public boolean fixedTransitionFeatures() {
        return single.fixedTransitionFeatures();
    }
    public void next(FeatureImpl feature) {
        single.next(feature);
    }
    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
     */
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
       return single.startScanFeaturesAt(data,prevPos,pos);
    }
    public boolean requiresTraining() {
        return single.requiresTraining();
    }
    public void train(DataSequence data, int pos) {
        single.train(data, pos);
    }
    public boolean needsCaching() {
        return single.needsCaching();
    }
}
