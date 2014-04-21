/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.knn;

import edu.cmu.minorthird.classify.*;

import java.io.*;

/**
 * Learn an Knn Classifier.
 * 
 * @author William Cohen
 */

public class KnnLearner extends OnlineClassifierLearner implements Serializable{

	static final long serialVersionUID=20080128L;

	private DatasetIndex index;

	private ExampleSchema schema;

	private int k=5;

	public KnnLearner(){
		this(5);
	}

	public KnnLearner(int k){
		this.k=k;
		reset();
	}

	@Override
	public ClassifierLearner copy(){
		KnnLearner knn=null;
		try{
			knn=(KnnLearner)this.clone();
			knn.index=new DatasetIndex();
		}catch(Exception e){
			e.printStackTrace();
		}
		return knn;
	}

	public int getK(){
		return k;
	}

	public void setK(int k){
		this.k=k;
	}

	@Override
	public void reset(){
		index=new DatasetIndex();
	}

	@Override
	public void addExample(Example e){
		index.addExample(e);
	}

	@Override
	public Classifier getClassifier(){
		return new KnnClassifier(index,schema,k);
	}

	@Override
	public void setSchema(ExampleSchema schema){
		this.schema=schema;
	}

	@Override
	public ExampleSchema getSchema(){
		return schema;
	}

	@Override
	public String toString(){
		return "[KnnLearner k:"+k+"]";
	}
}
