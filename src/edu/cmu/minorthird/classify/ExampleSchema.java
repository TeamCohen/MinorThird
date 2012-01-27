/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/** 
 * Defines legal formats for examples.  Currently this just checks
 * that the class labels are in some legal set.
 *
 * @author William Cohen
 */

public class ExampleSchema implements Serializable{

	static final long serialVersionUID=20071015;

	public static final String POS_CLASS_NAME="POS";
	public static final String NEG_CLASS_NAME="NEG";

	/** Schema for binary examples. */
	public final static ExampleSchema BINARY_EXAMPLE_SCHEMA=new ExampleSchema(new String[]{POS_CLASS_NAME,NEG_CLASS_NAME});

	private String[] validClassNames;
	private Set<String> validClassNameSet;

	/** Create a new scheme with the given list of validClassNames */
	public ExampleSchema(final String[] validClassNames){
		this.validClassNames=validClassNames;
		validClassNameSet=new HashSet<String>();
		for(int i=0;i<validClassNames.length;i++){
			validClassNameSet.add(validClassNames[i]);
		}
	}

	/** Added extend method to extend the schema with new class label value */
	public void extend(String newClassName){
		String newValidClassNames[]=new String[validClassNames.length+1];
		for(int i=0;i<validClassNames.length;i++){
			newValidClassNames[i]=validClassNames[i];
		}
		newValidClassNames[validClassNames.length]=newClassName;
		validClassNames=newValidClassNames;
		validClassNameSet.add(newClassName);
	}

	/** Get an array of all valid class names. */
	public String[] validClassNames(){
		return validClassNames;
	}

	/** Return number of valid class names */
	public int getNumberOfClasses(){
		return validClassNames.length;
	}

	/** Return i-th valid class name. */
	public String getClassName(int i){
		return validClassNames[i];
	}

	/** Return index of this class name, or -1 if it's not valid. */
	public int getClassIndex(String name){
		for(int i=0;i<validClassNames.length;i++){
			if(validClassNames[i].equals(name)){
				return i;
			}
		}
		return -1;
	}

	/** Determine if a ClassLabel is valid with respect to the schema. */
	public boolean isValid(ClassLabel label){
		for(String l:label.possibleLabels()){
			if(!validClassNameSet.contains(l)){
				return false;
			}
		}
		return true;
	}

	/** Determine if an example is valid with respect to the schema. */
	public boolean isValid(Example e){
		return isValid(e.getLabel());
	}

	@Override
	public boolean equals(Object o){
		if(o instanceof ExampleSchema){
			ExampleSchema b=(ExampleSchema)o;
			return validClassNameSet.equals(b.validClassNameSet);
		}
		else{
			return false;
		}
	}

	@Override
	public String toString(){
		return "[ExampleSchema: "+validClassNameSet+"]";
	}

}

