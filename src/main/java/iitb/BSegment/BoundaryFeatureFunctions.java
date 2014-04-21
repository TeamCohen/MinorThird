/*
 * Created on May 4, 2005
 *
 */
package iitb.BSegment;

import iitb.CRF.DataSequence;

/**
 * @author sunita
 *
 */
public interface BoundaryFeatureFunctions {
   // public void assignBoundary(BFeatureImpl feature, int pos);
    public int maxBoundaryGap();
    public void next(BFeatureImpl feature);
    public boolean startScanFeaturesAt(DataSequence data, int pos);
}
