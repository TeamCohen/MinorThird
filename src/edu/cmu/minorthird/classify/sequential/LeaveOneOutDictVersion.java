package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.transform.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.border.*;
import java.io.*;

/**
 *
 * @author William Cohen
 */

public class LeaveOneOutDictVersion implements BatchSegmenterLearner
{
	private String[] featurePattern;
	private BatchSegmenterLearner innerLearner;
    private String distanceNames;

	public LeaveOneOutDictVersion(String[] featurePattern, BatchSegmenterLearner innerLearner, String distanceNames)
	{
		this.featurePattern = featurePattern;
		this.innerLearner = innerLearner;
		this.distanceNames = distanceNames;
	}

	public LeaveOneOutDictVersion(BatchSegmenterLearner innerLearner, String distanceNames)
	{
	    this(LeaveOneOutDictTransformLearner.DEFAULT_PATTERN,innerLearner,distanceNames);
	}

	public LeaveOneOutDictVersion(BatchSegmenterLearner innerLearner) {
		this(LeaveOneOutDictTransformLearner.DEFAULT_PATTERN,innerLearner,"Jaccard");
	}
	public void setSchema(ExampleSchema schema)
	{
		;
	}

	public Segmenter batchTrain(SegmentDataset dataset)
	{
		LeaveOneOutDictTransformLearner transformLearner = new LeaveOneOutDictTransformLearner(featurePattern, distanceNames);
		InstanceTransform transform = transformLearner.batchTrain(dataset);
		SegmentTransform segmentTransform = new SegmentTransform(transform);
		SegmentDataset transformedDataset = segmentTransform.transform(dataset);
		//new ViewerFrame("transformedDataset", new SmartVanillaViewer(transformedDataset));
		Segmenter segmenter = innerLearner.batchTrain( transformedDataset );
		return new TransformingSegmenter( transform, segmenter ); 
	}
}

