/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.ExampleSchema;

/** 
 * A label which is associated with an instance---either by a classifier,
 * or in training data.
 *
 *<p>
 * MultiClassLabels should be weighted to that the weight for a class name
 * is (approximately) the log-odds having that class name, ie if
 * the probability of class "POS" is p, the getWeight("POS") should
 * return Math.log( p/(1-p) ).
 *
 * The POS and NEG class labels (as defined in
 * ExampleSchema.POS_CLASS_NAME and ExampleSchema.NEG_CLASS_NAME) are
 * special. Binary class labels should be created with the
 * positiveLabel(posWeight) and negativeLabel(negWeight) routines, or
 * else the binaryLabel routine. The numericLabel() returns +1 or -1
 * for binary classLabels.  The posWeight() method returns the score
 * of the positive class.

 * The classLabel.numericLabel() method ignores the underlying score.
 * For testing binary examples, classLabel.isPositive(),
 * classLabel.isNegative(), and classLabel.bestWeight() should be
 * used.
 *
 * @author Cameron Williams
 */

public class MultiClassLabel implements Serializable{

	static final long serialVersionUID=20080130L;

	private ClassLabel[] labels;

	private int dimensions;

	public MultiClassLabel(){
		;
	}

	public MultiClassLabel(ClassLabel[] labels){
		this.labels=labels;
		this.dimensions=labels.length;
	}

	public ClassLabel[] getLabels(){
		return labels;
	}

	/** Return the number of dimensions in the multiLabel */
	public int numDimensions(){
		return dimensions;
	}

	/** See if this is one of the distinguished binary labels. */
	public boolean[] isBinary(){
		boolean[] binary=new boolean[dimensions];
		for(int i=0;i<dimensions;i++){
			binary[i]=ExampleSchema.BINARY_EXAMPLE_SCHEMA.isValid(labels[i]);
		}
		return binary;
	}

	/** See if this is the distinguished positive label. */
	public boolean[] isPositive(){
		boolean[] positive=new boolean[dimensions];
		for(int i=0;i<dimensions;i++){
			positive[i]=
					ExampleSchema.POS_CLASS_NAME.equals(labels[i].bestClassName());
		}
		return positive;
	}

	/** See if this is the distinguished negative label. */
	public boolean[] isNegative(){
		boolean[] negative=new boolean[dimensions];
		for(int i=0;i<dimensions;i++){
			negative[i]=
					ExampleSchema.NEG_CLASS_NAME.equals(labels[i].bestClassName());
		}
		return negative;
	}

	/** Return a numeric score of +1, or -1 for a binary example */
	public double[] numericLabel(){
		double[] numLabel=new double[dimensions];
		for(int i=0;i<dimensions;i++){
			numLabel[i]=labels[i].numericLabel();
		}
		return numLabel;
	}

	/** Returns the highest-ranking label. */
	public String[] bestClassName(){
		String[] bestName=new String[dimensions];
		for(int i=0;i<dimensions;i++){
			bestName[i]=labels[i].bestClassName();
		}
		return bestName;
	}

	/** Returns the weight of the highest-ranking label. */
	public double[] bestWeight(){
		double[] bestWeight=new double[dimensions];
		for(int i=0;i<dimensions;i++){
			bestWeight[i]=labels[i].bestWeight();
		}
		return bestWeight;
	}

	/** Returns the weight of the positive class name */
	public double[] posWeight(){
		double[] posWeight=new double[dimensions];
		for(int i=0;i<dimensions;i++){
			posWeight[i]=labels[i].getWeight(ExampleSchema.POS_CLASS_NAME);
		}
		return posWeight;
	}

	/** Returns the probability of the positive class name */
	public double[] posProbability(){
		double[] posProb=new double[dimensions];
		for(int i=0;i<dimensions;i++){
			posProb[i]=labels[i].getProbability(ExampleSchema.POS_CLASS_NAME);
		}
		return posProb;
	}

	/** Returns the weight of the label. */
	public double[] getWeight(String[] label){
		double[] weight=new double[dimensions];
		for(int i=0;i<dimensions;i++){
			weight[i]=labels[i].getWeight(label[i]);
		}
		return weight;
	}

	/** Returns the probability of a label. */
	public double[] getProbability(String[] label){
		double[] odds=new double[dimensions];
		for(int i=0;i<dimensions;i++){
			double expOdds=Math.exp(labels[i].getWeight(label[i]));
			odds[i]=expOdds/(1.0+expOdds);
		}
		return odds;
	}

	/** Returns the set of labels that appear in the ranking. */
	public List<Set<String>> possibleLabels(){
		List<Set<String>> sets=new ArrayList<Set<String>>(dimensions);
		for(int i=0;i<dimensions;i++){
			sets.add(labels[i].possibleLabels());
		}
		return sets;
	}

	/** Is this label correct, relative to another label? */
	public boolean[] isMultiCorrect(MultiClassLabel otherLabel){
		if(otherLabel==null)
			throw new IllegalArgumentException("null otherLabel?");
		if(bestClassName()==null)
			throw new IllegalArgumentException("null bestClassName?");
		if(dimensions!=otherLabel.numDimensions())
			throw new IllegalArgumentException("Number of Dimensions do not match");
		boolean[] correct=new boolean[dimensions];
		for(int i=0;i<dimensions;i++){
			correct[i]=
					this.labels[i].bestClassName().equals(
							otherLabel.labels[i].bestClassName());
		}
		return correct;
	}

	/** Is this label correct, relative to another label? */
	public boolean isCorrect(MultiClassLabel otherLabel){
		if(otherLabel==null)
			throw new IllegalArgumentException("null otherLabel?");
		if(bestClassName()==null)
			throw new IllegalArgumentException("null bestClassName?");
		if(dimensions!=otherLabel.numDimensions())
			throw new IllegalArgumentException("Number of Dimensions do not match");
		boolean correct=true;
		for(int i=0;i<dimensions;i++){
			correct=
					correct&&
							this.labels[i].bestClassName().equals(
									otherLabel.labels[i].bestClassName());
		}
		return correct;
	}

	@Override
	public String toString(){
		String labelString="";
		for(int i=0;i<dimensions;i++){
			labelString=labelString+labels[i].toString();
		}
		return labelString;
	}

	public String toDetails(){
		String details="";
		for(int i=0;i<dimensions;i++){
			details=details+labels[i].toDetails();
		}
		return details;
	}
}
