package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.transform.InstanceTransform;
import edu.cmu.minorthird.classify.transform.LeaveOneOutDictTransformLearner;

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
	@Override
	public void setSchema(ExampleSchema schema)
	{
		;
	}

	@Override
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

