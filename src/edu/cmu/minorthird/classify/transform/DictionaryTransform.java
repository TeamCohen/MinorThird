/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import java.io.Serializable;
import java.util.Iterator;

import com.wcohen.ss.BasicStringWrapper;
import com.wcohen.ss.api.StringDistance;
import com.wcohen.ss.api.StringWrapper;
import com.wcohen.ss.lookup.SoftDictionary;

import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;

public class DictionaryTransform extends AbstractInstanceTransform implements Serializable{
	
  static private final long serialVersionUID = 1;

  private SoftDictionary[] softDict;
  private String[] featurePattern;
  private ExampleSchema schema;
  private String[] newFeatureNames;
  private double[] newFeatureValues;
	    
  private StringDistance distances[][];
  int numDistances;
	    
  public DictionaryTransform(ExampleSchema schema,SoftDictionary[] softDict,String[] featurePattern, StringDistance dists[][])
  {
    this.schema = schema;
    this.softDict = softDict;
    this.featurePattern = featurePattern;
    distances = dists;
    numDistances = distances[0].length;
    newFeatureNames = new String[schema.getNumberOfClasses()*numDistances];
    newFeatureValues = new double[newFeatureNames.length];
    int r = 0;
    for (int i=0; i< schema.getNumberOfClasses(); i++) {
      for (int d = 0; d < distances[i].length; d++) {
        newFeatureNames[r++] = distances[i][d].toString()+"_"+schema.getClassName(i);
      }
    }
  }
  @Override
	public Instance transform(Instance instance)
  {
    for (int i = 0; i < newFeatureValues.length; newFeatureValues[i++] = 0);
    //System.out.println("transforming "+instance);
    String text = getFeatureValue(instance,featurePattern);
    if (text==null) return instance;
    else {
      boolean nonZeroFeatureAdded = false;
      StringWrapper spanString = new BasicStringWrapper(text);
      for (int i=0; i<schema.getNumberOfClasses(); i++) {
        //System.out.println("looking up text in dict "+i);

        Object closestMatch = softDict[i].lookup(instance.getSubpopulationId(), spanString );			
        if (closestMatch != null) {
          // create various types of similarity measures.
          for (int d = 0; d < distances[i].length; d++) {
            double score  = distances[i][d].score(spanString, (StringWrapper)closestMatch);
            if (score >= 0) {
              nonZeroFeatureAdded = true;
              newFeatureValues[i*numDistances+d] = score;
            } 
          }
        }
      }
      if (nonZeroFeatureAdded) {
        Instance augmentedInstance = new AugmentedInstance( instance, newFeatureNames, newFeatureValues  );
        //System.out.println("transformed to "+augmentedInstance);
        // Feature f = new Feature(newFeatureNames[ schema.getClassIndex(ExampleSchema.POS_CLASS_NAME) ]);
        //System.out.println("weight of "+f+" is "+augmentedInstance.getWeight(f)+" in "+augmentedInstance);
        return augmentedInstance;
      } else {
        return instance;
      }
    }
  }
  @Override
	public String toString() 
  { 
    StringBuffer buf = new StringBuffer("[DictionaryTransform: dictSize");
    for (int i=0; i<schema.getNumberOfClasses(); i++) {
      buf.append(" "+schema.getClassName(i)+"=");
      buf.append(Integer.toString(softDict[i].size()));
    }
    buf.append("]");
    return buf.toString();
  }

  /** 
   * If feature pattern is of the form x.y.z and there is a feature of
   * the form x.y.z.a, return the string "a".
   */ 
	static public String getFeatureValue(Instance instance,String[] featurePattern)
	{
		//System.out.println("looking for "+StringUtil.toString(featurePattern)+" in "+instance);
		for (Iterator<Feature> i=instance.featureIterator(); i.hasNext(); ) {
			Feature f = i.next();
			String[] name = f.getName();
			if (matches(name,featurePattern)) return name[name.length-1];
		}
		return null;
	}

	static private boolean matches(String[] name,String[] featurePattern)
	{
		if (name.length-1 != featurePattern.length) return false;
		for (int i=0; i<featurePattern.length; i++) {
			if (!featurePattern[i].equals(name[i])) return false;
		}
		return true;
	}
  
}
