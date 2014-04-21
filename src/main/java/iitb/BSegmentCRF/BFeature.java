/*
 * Created on Apr 2, 2005
 *
 */
package iitb.BSegmentCRF;

import iitb.CRF.Feature;

/**
 * @author sunita
 *
 */
public interface BFeature extends Feature {
    /**
     * @return the start boundary where the feature is valid. 
     * If startOpen() is true then this is valid for all positions
     * to the left of this boundary
     */
    int start();
    /**
     * @return if the start boundary is exact or open
     */
    boolean startOpen();
    /**
     * @return the end boundary where the feature is valid. 
     * If endOpen() is true then this is valie for all positions
     * to the right of this boundary
     */
    int end();
    /**
     * 
     * @return if the end boundary is exact or open
     */
    boolean endOpen();
}
