/*
 * Created on Apr 12, 2005
 *
 */
package iitb.BSegment;

import iitb.CRF.DataSequence;
import iitb.Model.FeatureGenImpl;
import iitb.Model.FeatureImpl;
import iitb.Model.FeatureTypes;

/**
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
public abstract class BFeatureTypes extends FeatureTypes implements BoundaryFeatureFunctions {
    /**
	 * 
	 */
	private static final long serialVersionUID = -7882587085868697212L;
	/**
     * @param fgen
     */
    public BFeatureTypes(FeatureGenImpl fgen) {
        super(fgen);
    }
    /**
     * @param single
     */
    public BFeatureTypes(FeatureTypes single) {
        super(single);
    }
    public abstract boolean startScanFeaturesAt(DataSequence arg);
    public abstract void next(BFeatureImpl arg0);
    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
     */
    //public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
    //    return false;
    //}
    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#next(iitb.Model.FeatureImpl)
     */
    public void next(FeatureImpl f) {
        System.err.println("WARNING: Semi-CRF feature not implemented ");
    }
}
