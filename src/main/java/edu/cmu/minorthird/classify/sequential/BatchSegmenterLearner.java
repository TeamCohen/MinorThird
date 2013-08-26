package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;

/**
 *
 * @author William Cohen
 */

public interface BatchSegmenterLearner
{
	public Segmenter batchTrain(SegmentDataset dataset);
	public void setSchema(ExampleSchema schema);
}

