/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;

/**
 * @author William Cohen
 * Date: Nov 21, 2003
 */

abstract public class AbstractInstanceTransform implements InstanceTransform
{
	final public Example transform(Example example) 
	{ 
		return new Example( transform(example.asInstance()), example.getLabel() );
  }


	final public Dataset transform(Dataset dataset)
	{ 
		Dataset transformed = new BasicDataset();
		for (Example.Looper i = dataset.iterator(); i.hasNext(); ) {
			transformed.add( transform(i.nextExample()) );
		}
		return transformed;
	}


	abstract public Instance transform(Instance instance);
}
