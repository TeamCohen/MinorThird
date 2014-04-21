package iitb.CRF;

import java.io.Serializable;

/**
 * The basic interface to be implemented by the user of this package
 * for providing features of an individual data sequence. 
 * 
 * @author Sunita Sarawagi
 * */


public interface FeatureGenerator extends Serializable {
    /** The number of features has to be correctly set before train is called. */
    int numFeatures();
    void startScanFeaturesAt(DataSequence data, int pos);
    boolean hasNext();
    Feature next();
    public String featureName(int featureIndex);
};
