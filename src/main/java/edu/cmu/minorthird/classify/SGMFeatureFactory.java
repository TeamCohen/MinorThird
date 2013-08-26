/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

/**
 * For Stacked Graphical Learning. Creates Features, and maintains a mapping
 * between Features and numeric ids. Also ensures that only a single feature
 * instance exists with a particular name. Add the ExampleID when creating new
 * SGMExample.
 * 
 */

public class SGMFeatureFactory extends FeatureFactory{

	static final long serialVersionUID=20080128L;

	/**
	 * Return a version of the example in which all features have been translated
	 * to canonical versions from the feature factory.
	 */
	
	public SGMExample compressSGM(SGMExample example){
		Instance compactInstance=compress(example.asInstance());
		return new SGMExample(compactInstance,example.getLabel(),example.getExampleID(),example.getWeight());
	}

}
