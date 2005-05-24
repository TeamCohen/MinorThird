/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;



/**
 * A dataset which supports random access to the multiExamples.
 *
 * @author Cameron Williams
 */

public class MultiRandomAccessDataset extends MultiDataset
{
	public MultiRandomAccessDataset()  {super();}

	public MultiExample getMultiExample(int i)
	{
		return (MultiExample)examples.get(i);
	}

	public String toString() 
	{
		return super.toString();
	}
}
