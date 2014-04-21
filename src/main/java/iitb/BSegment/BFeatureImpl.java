/** BFeatureImpl.java
 * Created on Apr 12, 2005
 *
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
package iitb.BSegment;

import iitb.BSegmentCRF.BFeature;
import iitb.Model.FeatureImpl;

public class BFeatureImpl extends FeatureImpl implements BFeature {
    /**
	 * 
	 */
	private static final long serialVersionUID = -818475084232481221L;
	int _startB;
    int _endB;
    boolean _startOpen;
    boolean _endOpen;
    /**
     * 
     */
    public BFeatureImpl() {
        super();
    }

    /**
     * @param arg0
     */
    public BFeatureImpl(FeatureImpl arg0) {
        super(arg0);
    }

    /* (non-Javadoc)
     * @see iitb.BSegmentCRF.BFeature#start()
     */
    public int start() {
        return _startB;
    }

    /* (non-Javadoc)
     * @see iitb.BSegmentCRF.BFeature#startOpen()
     */
    public boolean startOpen() {
        return _startOpen;
    }

    /* (non-Javadoc)
     * @see iitb.BSegmentCRF.BFeature#end()
     */
    public int end() {
        return _endB;
    }

    /* (non-Javadoc)
     * @see iitb.BSegmentCRF.BFeature#endOpen()
     */
    public boolean endOpen() {
        return _endOpen;
    }

    public void copy(BFeatureImpl feature) {
        super.copy(feature);
        copyBoundary(feature);
    }
    public String toString() {
        return super.toString() 
        + " S:" + _startB+":"+_startOpen 
        + " E:" + _endB+":"+_endOpen;
    }

    /**
     * @param boundary
     */
    public void copyBoundary(BFeatureImpl feature) {
        // TODO Auto-generated method stub
        _startB = feature._startB;
        _endB = feature._endB;
        _endOpen = feature._endOpen;
        _startOpen = feature._startOpen;
        
    }
}
