/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.knn;

import edu.cmu.minorthird.classify.*;

/**
 * Learn an Knn Classifier.
 *
 * @author William Cohen
 */

public class KnnLearner extends OnlineClassifierLearner
{
	private DatasetIndex index;
	private ExampleSchema schema;
	private int k = 5;

	public KnnLearner()	{	this(5); }

	public KnnLearner(int k) { this.k = k; reset(); }

	public int getK() { return k; }
	public void setK(int k) { this.k=k; }

	public void reset() { index = new DatasetIndex(); }

	public void addExample(Example e) { index.addExample( e ); }

	public Classifier getClassifier() {	return new KnnClassifier(index, schema, k);	}

	public void setSchema(ExampleSchema schema) { this.schema = schema; }

	public String toString() { return "[KnnLearner k:"+k+"]"; }
}

