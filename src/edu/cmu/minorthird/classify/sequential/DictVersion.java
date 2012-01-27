package edu.cmu.minorthird.classify.sequential;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.wcohen.ss.DistanceLearnerFactory;
import com.wcohen.ss.api.StringDistance;
import com.wcohen.ss.api.StringDistanceLearner;
import com.wcohen.ss.lookup.SoftDictionary;

import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.transform.DictionaryTransform;
import edu.cmu.minorthird.classify.transform.InstanceTransform;
import edu.cmu.minorthird.classify.transform.LeaveOneOutDictTransformLearner;


/**
 * Extend a SegmenterLearner by including a dictionary.
 * 
 * Distance to the closest dictionary entry will be used as an
 * additional feature.
 *
 * @author William Cohen
 */

public class DictVersion implements BatchSegmenterLearner
{
	private String[] featurePattern;
	private BatchSegmenterLearner innerLearner;
  private SoftDictionary softDictionary;
  private StringDistance[] distances;

	public DictVersion(BatchSegmenterLearner innerLearner, File dictFile, String distanceNames) 
    throws IOException,FileNotFoundException
  {
    this(LeaveOneOutDictTransformLearner.DEFAULT_PATTERN, innerLearner, dictFile, distanceNames);
  }

	public DictVersion(String[] featurePattern, BatchSegmenterLearner innerLearner, File dictFile, String distanceNames)
    throws IOException,FileNotFoundException
  {
		this.featurePattern = featurePattern;
		this.innerLearner = innerLearner;
    softDictionary = new SoftDictionary();
    softDictionary.load(dictFile);
    init(distanceNames);
  }

	public DictVersion(
    String[] featurePattern, BatchSegmenterLearner innerLearner, SoftDictionary softDictionary, String distanceNames)
	{
		this.featurePattern = featurePattern;
		this.innerLearner = innerLearner;
    this.softDictionary = softDictionary;
    // set up the array of distances
    init(distanceNames);
  }
  
  private void init(String distanceNames)
  {
		this.distances = DistanceLearnerFactory.buildArray(distanceNames);
    for (int d = 0; d < distances.length; d++) {
      if (distances[d] instanceof StringDistanceLearner) {
        distances[d] = softDictionary.getTeacher().train( (StringDistanceLearner)distances[d] );
      }
    }
	}

	@Override
	public void setSchema(ExampleSchema schema) {;}

	@Override
	public Segmenter batchTrain(SegmentDataset dataset)
	{
    // in this case, we don't need to learn a transform, we can just construct it...
    ExampleSchema schema = dataset.getSchema();
    // the constructor requires one dictionary and one set of distances per class
    SoftDictionary[] dictPerClass = new SoftDictionary[schema.getNumberOfClasses()];
    for (int i=0; i<schema.getNumberOfClasses(); i++) {
      dictPerClass[i] = softDictionary;
    }
    StringDistance[][] distPerClass = new StringDistance[schema.getNumberOfClasses()][distances.length];
    for (int i=0; i<schema.getNumberOfClasses(); i++) {
      for (int j=0; j<distances.length; j++) {
        distPerClass[i][j] = distances[j];
      }
    }
		InstanceTransform transform = new DictionaryTransform(schema,dictPerClass,featurePattern,distPerClass);
		SegmentTransform segmentTransform = new SegmentTransform(transform);

    // now train on the transformed dataset
		SegmentDataset transformedDataset = segmentTransform.transform(dataset);
		//new ViewerFrame("transformedDataset", new SmartVanillaViewer(transformedDataset));
		Segmenter segmenter = innerLearner.batchTrain( transformedDataset );

    // return a transforming version of the learned segmenter
		return new TransformingSegmenter( transform, segmenter ); 
	}

  public static void main(String[] args) 
    throws IOException,FileNotFoundException
  {
    new DictVersion(new SegmentCRFLearner(), new File(args[0]), args[1]);
  }
}

