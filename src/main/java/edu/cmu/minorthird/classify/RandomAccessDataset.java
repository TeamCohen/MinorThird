/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;



/**
 * A dataset which supports random access to the examples.
 *
 * @author William Cohen
 */

public class RandomAccessDataset extends BasicDataset{
	
	static final long serialVersionUID=20071015;

	public Example getExample(int i){
		return examples.get(i);
	}

}
