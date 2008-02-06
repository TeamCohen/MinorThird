/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import java.util.Iterator;

import com.wcohen.ss.DistanceLearnerFactory;
import com.wcohen.ss.api.StringDistance;
import com.wcohen.ss.api.StringDistanceLearner;
import com.wcohen.ss.lookup.SoftDictionary;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;

/**
 * Construct a transformation of a dataset that includes "leave one out dictionary".
 *
 * <p> The value of some feature for of all training examples will be stored in a soft
 * dictionary, and distance to the closest dictionary entry will be
 * used as an additional feature.  For more information, see Sarawagi
 * and Cohen, "Semi-Markov Conditional Random Fields for Information
 * Extraction", 2004.
 *
 * @author William Cohen
 */

public class LeaveOneOutDictTransformLearner 
{
	final public static String[] DEFAULT_PATTERN = new String[]{"eq","lc"};

	private String[] featurePattern;
	private boolean buildDictionaryForNegativeClass=false;
  private StringDistance distances[][];
  String distanceNames;

  public LeaveOneOutDictTransformLearner() {
    this("SoftTFIDF");
  }
	public LeaveOneOutDictTransformLearner(String distanceNames)
	{
		this(DEFAULT_PATTERN,distanceNames);
	}

  public LeaveOneOutDictTransformLearner(String[] featurePattern) {
    this(featurePattern,"SoftTFIDF");
	}
	public LeaveOneOutDictTransformLearner(String[] featurePattern, String distanceNames) {
		this.featurePattern = featurePattern;
		this.distanceNames = distanceNames;
	}

	public void setSchema(ExampleSchema schema)	{;}


  public void trainDistances(ExampleSchema schema, SoftDictionary[] softDict) {
    distances = new StringDistance[schema.getNumberOfClasses()][0];
    for (int i=0; i<schema.getNumberOfClasses(); i++) {
	    distances[i] = DistanceLearnerFactory.buildArray(distanceNames);
    }
    for (int i=0; i<schema.getNumberOfClasses(); i++) {
	    for (int d = 0; d < distances[i].length; d++) {
        if (distances[i][d] instanceof StringDistanceLearner) {
          distances[i][d] = softDict[i].getTeacher().train( (StringDistanceLearner)distances[i][d] );
        }
	    }
    }
  }
    

	/** Examine data, build an instance transformer */
	public InstanceTransform batchTrain(Dataset dataset)
	{
		// build a soft dictionary for each example
		ExampleSchema schema = dataset.getSchema();
		int yNeg = schema.getClassIndex( ExampleSchema.NEG_CLASS_NAME );
		SoftDictionary[] softDict = new SoftDictionary[schema.getNumberOfClasses()];
		for (int i=0; i<schema.getNumberOfClasses(); i++) {
			softDict[i] = new SoftDictionary();
		}
		for (Iterator<Example> i=dataset.iterator(); i.hasNext(); ) {
			Example ex = i.next();
			String text = DictionaryTransform.getFeatureValue(ex,featurePattern);
			if (text!=null) {
				//System.out.println("adding text '"+text+"' for dict "+ex.getLabel().bestClassName());
				int y = schema.getClassIndex( ex.getLabel().bestClassName() );
				if (buildDictionaryForNegativeClass || y!=yNeg) {
					softDict[y].put(ex.getSubpopulationId(),text,ex);
				}
			}
		}
		trainDistances(schema,softDict);
		return new DictionaryTransform(schema,softDict,featurePattern,distances);
	}

}
