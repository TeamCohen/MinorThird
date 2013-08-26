/*
 * Created on May 4, 2005
 *
 */
package iitb.BSegment;

import iitb.Model.*;

/**
 * @author sunita
 *
 */
public class BFeatureTypesEachLabel extends FeatureTypesEachLabel implements BoundaryFeatureFunctions {
    BoundaryFeatureFunctions bsingle;
    BFeatureImpl bfeatureImpl = new BFeatureImpl();
    /**
     * @param single
     */
    public BFeatureTypesEachLabel(FeatureGenImpl fgen, FeatureTypes single) {
        super(fgen,single);
        bsingle = (BoundaryFeatureFunctions)single;
    }
    private static final long serialVersionUID = 1L;

    public int maxBoundaryGap() {
        return bsingle.maxBoundaryGap();
    }
    protected void nextFeature() {
	    bsingle.next(bfeatureImpl);
	}
    /* (non-Javadoc)
     * @see iitb.BSegment.BoundaryFeatureFunctions#assignBoundary(iitb.BSegment.BFeatureImpl, int)
     */
   /* public void assignBoundary(BFeatureImpl feature, int pos) {
        bsingle.assignBoundary(feature,pos);
    }*/

    /* (non-Javadoc)
     * @see iitb.BSegment.BoundaryFeatureFunctions#next(iitb.BSegment.BFeatureImpl)
     */
    public void next(BFeatureImpl f) {
    	    f.copy(bfeatureImpl);
    	    super.nextCopyDone(f);
    }
    public void next(FeatureImpl f) {
	    f.copy(bfeatureImpl);
	    super.nextCopyDone(f);
    }
}
