/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import java.io.Serializable;

import edu.cmu.minorthird.classify.ExampleSchema;

/** 
 * Defines legal formats for examples with multiple labels.  Currently this just checks
 * that the class labels are in some legal set.
 *
 * @author Cameron Williams
 */

public class MultiExampleSchema implements Serializable{
	
	static final long serialVersionUID=20080130L;

	private ExampleSchema[] schemas;

	private int numDimensions;

	/** Create a new scheme with the given list of validClassNames */
	public MultiExampleSchema(ExampleSchema[] schemas){
		this.schemas=schemas;
		numDimensions=schemas.length;
	}

	public ExampleSchema[] getSchemas(){
		return schemas;
	}

	public int numDimensions(){
		return numDimensions;
	}

	@Override
	public String toString(){
		StringBuilder b=new StringBuilder();
		for(int i=0;i<schemas.length;i++){
			b.append(schemas[i].toString()+"  ");
		}
		b.append("\n");
		return b.toString();
	}

}
