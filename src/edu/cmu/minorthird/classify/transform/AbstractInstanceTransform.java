/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import java.util.Iterator;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.multi.MultiDataset;
import edu.cmu.minorthird.classify.multi.MultiExample;

/**
 * @author William Cohen
 * Date: Nov 21, 2003
 */

abstract public class AbstractInstanceTransform implements InstanceTransform
{
	@Override
	final public Example transform(Example example) 
	{ 
		return new Example( transform(example.asInstance()), example.getLabel() );
	}
    
    final public MultiExample transform(MultiExample example) 
	{ 
		return new MultiExample( transform(example.asInstance()), example.getMultiLabel() );
	}


	@Override
	final public Dataset transform(Dataset dataset)
	{ 
		Dataset transformed = new BasicDataset();
		for (Iterator<Example> i = dataset.iterator(); i.hasNext(); ) {
			transformed.add( transform(i.next()) );
		}
		return transformed;
	}

    final public MultiDataset transform(MultiDataset dataset)
	{ 
		MultiDataset transformed = new MultiDataset();
		for (Iterator<MultiExample> i = dataset.multiIterator(); i.hasNext(); ) {
			transformed.addMulti( transform(i.next()) );
		}
		return transformed;
	}


	@Override
	abstract public Instance transform(Instance instance);
}
