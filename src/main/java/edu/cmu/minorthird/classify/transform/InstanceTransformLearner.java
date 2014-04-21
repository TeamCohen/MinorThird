/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.ExampleSchema;

/**
 * @author William Cohen
 * Date: Nov 21, 2003
 */

public interface InstanceTransformLearner {

    /** Accept an ExampleSchema - constraints on what the
      * Examples will be. */
    public void setSchema(ExampleSchema schema);

    /** Examine data, build an instance transformer */
    public InstanceTransform batchTrain(Dataset dataset);

}
