/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import com.wcohen.ss.api.*;
import com.wcohen.ss.*;
import com.wcohen.ss.lookup.*;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.*;

import java.io.*;

/**
 * @author William Cohen
 * Date: Nov 21, 2003
 */

public class LeaveOneOutDictTransformLearner 
{
	final public static String[] DEFAULT_PATTERN = new String[]{"eq","lc"};

	private String[] featurePattern;
	private boolean buildDictionaryForNegativeClass=false;

	public LeaveOneOutDictTransformLearner()
	{
		this(DEFAULT_PATTERN);
	}

	public LeaveOneOutDictTransformLearner(String[] featurePattern)
	{
		this.featurePattern = featurePattern;
	}

	public void setSchema(ExampleSchema schema)	{;}

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
		for (Example.Looper i=dataset.iterator(); i.hasNext(); ) {
			Example ex = i.nextExample();
			String text = getFeatureValue(ex,featurePattern);
			if (text!=null) {
				//System.out.println("adding text '"+text+"' for dict "+ex.getLabel().bestClassName());
				int y = schema.getClassIndex( ex.getLabel().bestClassName() );
				if (buildDictionaryForNegativeClass || y!=yNeg) {
					softDict[y].put(instanceId(ex),text,ex);
				}
			}
		}
		return new DictionaryTransform(schema,softDict,featurePattern);
	}

	// return a unique code for an example, for leave-one-out matching,
	// and a null code for an instance, so it will match anything
	static private String instanceId(Instance instance)
	{
	    return instance.getSubpopulationId();// +":"+Integer.toString(instance.hashCode());
	    /*		if (instance instanceof Example) {
		    return instance.getSubpopulationId() +":"+Integer.toString(instance.hashCode());
		} else {
			return null;
		}
	    */
	}

	static private String getFeatureValue(Instance instance,String[] featurePattern)
	{
		//System.out.println("looking for "+StringUtil.toString(featurePattern)+" in "+instance);
		for (Feature.Looper i=instance.featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
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

	//
	//
	//

	static public class DictionaryTransform extends AbstractInstanceTransform implements Serializable
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;

		private SoftDictionary[] softDict;
		private String[] featurePattern;
		private ExampleSchema schema;
		private String[] newFeatureNames;
		private double[] newFeatureValues;

		public DictionaryTransform(ExampleSchema schema,SoftDictionary[] softDict,String[] featurePattern)
		{
			this.schema = schema;
			this.softDict = softDict;
			this.featurePattern = featurePattern;
			newFeatureNames = new String[schema.getNumberOfClasses()];
			for (int i=0; i<newFeatureNames.length; i++) {
				newFeatureNames[i] = "distToSome "+schema.getClassName(i);
			}
			newFeatureValues = new double[schema.getNumberOfClasses()];
		}
		public Instance transform(Instance instance)
		{
			//System.out.println("transforming "+instance);
			String text = getFeatureValue(instance,featurePattern);
			if (text==null) return instance;
			else {
				boolean nonZeroFeatureAdded = false;
				for (int i=0; i<schema.getNumberOfClasses(); i++) {
				    // System.out.println("looking up text in dict "+i+ " " + instanceId(instance) + " " + text);
					newFeatureValues[i] = softDict[i].lookupDistance( instanceId(instance), text );
					// softDict returns -Double.MAX_VALUE if nothing matches, not too useful
					if (newFeatureValues[i]<=0) {
						newFeatureValues[i]=0.0;
					} else {
						nonZeroFeatureAdded = true;
						//System.out.println("lookupDistance '"+text+"' for y="+i+" is "+newFeatureValues[i]);
					}
				}
				if (nonZeroFeatureAdded) {
					Instance augmentedInstance = new AugmentedInstance( instance, newFeatureNames, newFeatureValues  );
					//System.out.println("transformed to "+augmentedInstance);
					Feature f = new Feature(newFeatureNames[ schema.getClassIndex(ExampleSchema.POS_CLASS_NAME) ]);
					//System.out.println("weight of "+f+" is "+augmentedInstance.getWeight(f)+" in "+augmentedInstance);
					return augmentedInstance;
				} else {
					return instance;
				}
			}
		}
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
	}
}
