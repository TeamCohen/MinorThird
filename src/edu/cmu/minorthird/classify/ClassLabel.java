/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;
import java.util.Set;

/** 
 * A label which is associated with an instance---either by a classifier,
 * or in training data.
 *
 * ClassLabels should be weighted to that the weight for a label
 * is (approximately) the log-odds having that label.
 *
 * <li>Binary class labels should always be created with the
 * positiveLabel() and negativeLabel() routines.  For training, a
 * +1/-1 labeling of the class can be obtained with the
 * classLabel.numericScore() method---this ignores the underlying
 * score.  For testing, classLabel.isPositive(),
 * classLabel.isNegative(), and classLabel.bestWeight() should be
 * used.  The desired weight is the log odds of the POSITIVE class.
 *
 * </ol>
 *
 * @author William Cohen
*/

public class ClassLabel implements Serializable
{
  static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

	private WeightedSet wset = new WeightedSet();
	private String bestLabel = null;
	private double bestWeight = -Double.MAX_VALUE;

	public ClassLabel() {;}
	public ClassLabel(String label) { this(label,1.0); }
	public ClassLabel(String label,double weight) { add(label,weight); }

	/** Create a positive binary label, with the associated score (in logits). */
	static public ClassLabel positiveLabel(double score)
	{
		ClassLabel result = new ClassLabel(ExampleSchema.POS_CLASS_NAME,score);
		result.add(ExampleSchema.NEG_CLASS_NAME,-score);
		return result;
	}

	/** Create a negative binary label, with the associated score (in logits). */
	static public ClassLabel negativeLabel(double score)
	{
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
	public double numericScore() 
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

	/** Returns the weight of the label. */
	public double getWeight(String label) { return wset.getWeight(label,-Double.MAX_VALUE); }

	/** Returns the set of labels that appear in the ranking. */
	public Set possibleLabels() { return wset.asSet(); }

	/** Is this label correct, relative to another label? */
	public boolean isCorrect(ClassLabel otherLabel) 
	{ 
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

	public String toString() {
		return "[Class: "+bestLabel+" "+bestWeight+"]";
	}
}

