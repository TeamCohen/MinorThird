/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;



/**
 * A dataset which supports random access to the examples.
 *
 * @author William Cohen
 */

public class RandomAccessDataset extends BasicDataset
{
	public RandomAccessDataset()  {super();}

	public Example getExample(int i)
	{
		return (Example)examples.get(i);
	}

	public String toString() 
	{
		return super.toString();
	}
}
