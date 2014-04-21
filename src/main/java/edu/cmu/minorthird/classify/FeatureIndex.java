package edu.cmu.minorthird.classify;

import java.util.Iterator;

/**
 * @author Edoardo Airoldi
 * Date: Nov 24, 2003
 */

public interface FeatureIndex {

    public void addExample(Example e);

    public Iterator<Feature> featureIterator();

    /** Number of examples containing the current feature. */
    public int size(Feature f);

    /** Get i-th example contained in feature f */
    public Example getExample(Feature f,int i);

    /** Get counts of feature f in i-th example containing feature f */
    public double getCounts(Feature f,int i);

    /** Get all examples containing a feature. */
    public Iterator<Example> getNeighbors(Instance instance);

}
