/*
 * Created on Dec 2, 2004
 *
 */
package iitb.Model;

import iitb.CRF.DataSequence;

/**
 * @author sunita
 *
 */
public class ClassPriorFeature extends FeatureTypes {
    private static final long serialVersionUID = 16L;
    transient int thisClassId;
    /**
     * @param fgen
     */
    public ClassPriorFeature(FeatureGenImpl fgen) {
        super(fgen);
    }

    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
     */
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        thisClassId = model.numStates()-1;
        return hasNext();
    }

    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#hasNext()
     */
    public boolean hasNext() {
        return thisClassId >= 0;
    }

    /* (non-Javadoc)
     * @see iitb.Model.FeatureTypes#next(iitb.Model.FeatureImpl)
     */
    public void next(FeatureImpl f) {
        f.yend = thisClassId;
        f.ystart = -1;
        f.val = 1;
        String name="";
        if (featureCollectMode()) {
            name = "Bias_" + thisClassId;
        }
        setFeatureIdentifier(thisClassId,thisClassId,name,f);
        thisClassId--;
    }

}
