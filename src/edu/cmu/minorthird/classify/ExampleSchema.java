/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** 
 * Defines legal formats for examples.  Currently this just checks
 * that the class labels are in some legal set.
 *
 * @author William Cohen
*/

public class ExampleSchema implements Serializable
{
	static public final String POS_CLASS_NAME="POS";
	static public final String NEG_CLASS_NAME="NEG";

	/** Schema for binary examples. */
	final static public ExampleSchema BINARY_EXAMPLE_SCHEMA = 
	  new ExampleSchema(new String[]{POS_CLASS_NAME,NEG_CLASS_NAME});

	private String[] validClassNames;
	private Set validClassNameSet;

	/** Create a new scheme with the given list of validClassNames */
	public ExampleSchema(final String[] validClassNames)
	{
		this.validClassNames = validClassNames;
		validClassNameSet = new HashSet();
		for (int i=0; i<validClassNames.length; i++) {
			validClassNameSet.add( validClassNames[i] );
		}
	}

	/** Get an array of all valid class names. */
	public String[] validClassNames() 
	{
		return validClassNames;
	}
	
	/** Return number of valid class names */
	public int getNumberOfClasses()
	{
		return validClassNames.length;
	}

	/** Return i-th valid class name. */
	public String getClassName(int i)
	{
		return validClassNames[i];
	}

	/** Return index of this class name, or -1 if it's not valid. */
	public int getClassIndex(String name)
	{
		for (int i=0; i<validClassNames.length; i++) {
			if (validClassNames[i].equals(name)) return i;
		}
		return -1;
	}

	/** Determine if an example is valid with respect to the schema. */
	public boolean isValid(Example e)
	{
		return isValid(e.getLabel());
	}

	/** Determine if a ClassLabel is valid with respect to the schema. */
	public boolean isValid(ClassLabel label)
	{
		Set classNames = label.possibleLabels();
		for (Iterator i=classNames.iterator(); i.hasNext(); ) {
			if (!validClassNameSet.contains( i.next() )) return false;
		}
		return true;
	}

	public boolean equals(Object o)
	{
		if (!(o instanceof ExampleSchema)) return false;
		ExampleSchema b = (ExampleSchema)o;
		return validClassNameSet.equals(b.validClassNameSet);
	}

	public String toString() { return "[ExampleSchema: "+validClassNameSet+"]"; }
}

