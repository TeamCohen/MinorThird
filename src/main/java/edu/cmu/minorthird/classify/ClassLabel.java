/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;
import java.util.Set;

/** 
 * A label which is associated with an instance---either by a classifier,
 * or in training data.
 *
 *<p>
 * ClassLabels should be weighted to that the weight for a class name
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
 * @author William Cohen
 */

public class ClassLabel implements Serializable{

	static private final long serialVersionUID = 1;

	private WeightedSet<String> wset=new WeightedSet<String>();
	private String bestLabel=null;
	private double bestWeight=Double.NEGATIVE_INFINITY;

	public ClassLabel(String label,double weight){
		add(label,weight);
	}

	public ClassLabel(String label){
		this(label,1.0);
	}

	public ClassLabel(){}

	/** Create a positive binary label, with the associated score (in logits). */
	static public ClassLabel multiPosLabel(String label, double score)
	{
		ClassLabel result = new ClassLabel(label,score);
		//String negLabel = "NOT" + label;
		return result;
	}

	/** Create a positive binary label, with the associated score (in logits). */
	static public ClassLabel multiNegLabel(String label, double score)
	{
		ClassLabel result = new ClassLabel(label,score);
		return result;
	}

	/** Create a binary label, either positive or negative, as appropriate, with the associated score (in logits). */
	static public ClassLabel multiLabel(String name, double score)
	{
		return (score>=0?multiPosLabel(name, score):multiNegLabel(name, score));
	}

	/** Create a positive binary label, with the associated score (in logits). */
	static public ClassLabel positiveLabel(double score)
	{
		if (score<0) throw new IllegalArgumentException("positiveLabel should have positive score");
		ClassLabel result = new ClassLabel(ExampleSchema.POS_CLASS_NAME,score);
		result.add(ExampleSchema.NEG_CLASS_NAME,-score);
		return result;
	}

	/** Create a negative binary label, with the associated score (in logits). */
	static public ClassLabel negativeLabel(double score)
	{
		if (score>0) throw new IllegalArgumentException("negativeLabel should have negative score");
		ClassLabel result = new ClassLabel(ExampleSchema.POS_CLASS_NAME,score);
		result.add(ExampleSchema.NEG_CLASS_NAME,-score);
		return result;
	}
	/** Create a binary label, either positive or negative, as appropriate, with the associated score (in logits). */
	static public ClassLabel binaryLabel(double score)
	{
		return (score>=0?positiveLabel(score):negativeLabel(score));
	}

	/** See if this is one of the distinguished binary labels. */
	public boolean isBinary() { return ExampleSchema.BINARY_EXAMPLE_SCHEMA.isValid(this); }

	/** See if this is the distinguished positive label. */
	public boolean isPositive() { return ExampleSchema.POS_CLASS_NAME.equals(this.bestLabel); }

	/** See if this is the distinguished negative label. */
	public boolean isNegative() { return ExampleSchema.NEG_CLASS_NAME.equals(this.bestLabel); }

	/** Return a numeric score of +1, or -1 for a binary example */
	public double numericLabel() 
	{
		if (isPositive()) return +1;
		else if (isNegative()) return -1;
		else throw new IllegalArgumentException("not binary label");
	}

	/** Returns the highest-ranking label. */
	public String bestClassName() { return bestLabel; }

	/** Returns the weight of the highest-ranking label. */
	public double bestWeight() { return bestWeight; }

	/** Returns the weight of the positive class name */
	public double posWeight() { return getWeight(ExampleSchema.POS_CLASS_NAME);	}

	/** Returns the probability of the positive class name */
	public double posProbability() { return getProbability(ExampleSchema.POS_CLASS_NAME); }

	/** Returns the weight of the label. */
	public double getWeight(String label) { return wset.getWeight(label,-Double.MAX_VALUE); }

	/** Returns the probability of a label. */
	public double getProbability(String label) 
	{ 
		// same as MathUtil.logistic, I think
		double expOdds = Math.exp( getWeight(label) );
		return expOdds/(1.0 + expOdds);
	}

	/** Returns the set of labels that appear in the ranking. */
	public Set<String> possibleLabels() { return wset.asSet(); }

	/** Is this label correct, relative to another label? */
	public boolean isCorrect(ClassLabel otherLabel) 
	{ 
		if (otherLabel==null) throw new IllegalArgumentException("null otherLabel?");
		if (bestClassName()==null) throw new IllegalArgumentException("null bestClassName?");
		return this.bestClassName().equals(otherLabel.bestClassName());
	}

	/** Is this label correct, relative to a numeric label? */
	public boolean isCorrect(double otherLabel) 
	{ 
		if (isBinary()) return (isPositive() && otherLabel>=0);
		else throw new IllegalArgumentException("not a binary label");
	}

	/** Add a label with the given weight to this ClassLabel. */
	public void add(String label, double weight) {
		if (weight>bestWeight) {
			bestWeight = weight;
			bestLabel = label;
		}
		wset.add( label, weight );
	}

	@Override
	public String toString() 
	{
		return "[Class: "+bestLabel+" "+bestWeight+"]";
	}

	public String toDetails()
	{
		return "[ClassLabel: "+wset+"]"; 
	}
}

