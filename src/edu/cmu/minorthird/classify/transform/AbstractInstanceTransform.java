/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Instance;

/**
 * @author William Cohen
 * Date: Nov 21, 2003
 */

abstract public class AbstractInstanceTransform {

    final public Example transform(Example example) { return null; }

    final public Dataset transform(Dataset dataset) { return null; }

    // I like including this..
    abstract public Instance transform(Instance instance);

}
