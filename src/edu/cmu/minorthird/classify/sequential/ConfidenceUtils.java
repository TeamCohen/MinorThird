package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;

/**
 * A hack for computing approximate confidences over a sequence.
 *
 * @author William Cohen
 */

public class ConfidenceUtils
{
    static public double sumPredictedWeights(ClassLabel[] predictedClasses,int lo,int hi)
    {
        double conf = 0;
        for (int i=lo; i<hi; i++) {
            conf += predictedClasses[i].bestWeight();
        }
        return conf;
    }
}

