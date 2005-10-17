/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.multi.*;

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
    
    final public MultiExample transform(MultiExample example) 
	{ 
		return new MultiExample( transform(example.asInstance()), example.getMultiLabel() );
	}


	final public Dataset transform(Dataset dataset)
	{ 
		Dataset transformed = new BasicDataset();
		for (Example.Looper i = dataset.iterator(); i.hasNext(); ) {
			transformed.add( transform(i.nextExample()) );
		}
		return transformed;
	}

    final public MultiDataset transform(MultiDataset dataset)
	{ 
		MultiDataset transformed = new MultiDataset();
		for (MultiExample.Looper i = dataset.multiIterator(); i.hasNext(); ) {
			transformed.addMulti( transform(i.nextMultiExample()) );
		}
		return transformed;
	}


	abstract public Instance transform(Instance instance);
}
