/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.algorithms.linear.*;



/**
 * Abstract class which implements the 'getBinaryClassifier' method of BinaryClassifierLearner's.
 *
 * @author William Cohen
 */

public abstract class OnlineBinaryClassifierLearner extends OnlineClassifierLearner implements BinaryClassifierLearner
{    
  public Hyperplane c = null;

	final public void setSchema(ExampleSchema schema)
	{
		if (!ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema)) {
			throw new IllegalStateException("can only learn binary example data");
		}
	}

	final public BinaryClassifier getBinaryClassifier()
	{
		return (BinaryClassifier)getClassifier();
	}

  public void reset() 
  {
    c = null;
  }

  public void addClassifier(Hyperplane classifier) {
    if(this.c == null) 
	    this.c = new Hyperplane();
    this.c.increment(classifier);
  }

}
