/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** 
 * Defines legal formats for examples with multiple labels.  Currently this just checks
 * that the class labels are in some legal set.
 *
 * @author Cameron Williams
*/

public class MultiExampleSchema implements Serializable
{
    private ExampleSchema[] schemas;
    private int numDimensions;

	/** Create a new scheme with the given list of validClassNames */
	public MultiExampleSchema(ExampleSchema[] schemas)
	{
	    this.schemas = schemas;
	    numDimensions = schemas.length;
	}

    public ExampleSchema[] getSchemas() {
	return schemas;
    }

    public int numDimensions() {
	return numDimensions;
    }
    public String toString() {
	String s = new String("");;
	for (int i=0; i<schemas.length; i++) {
	    s = s + schemas[i].toString() + "  ";
	}
	s = s + '\n';
	return s;
    }
    
}

