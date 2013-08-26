/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Instance;

/**
 * @author William Cohen
 * Date: Nov 21, 2003
 */

public interface InstanceTransform {

    /** Create a transformed copy of the instance. */
    public Instance transform(Instance instance);

    /** Create a transformed copy of the example. */
    public Example transform(Example example);

    /** Create a transformed copy of a dataset. */
    public Dataset transform(Dataset dataset);
}
