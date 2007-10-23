package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.util.AbstractLooper;
import edu.cmu.minorthird.util.gui.Visible;

import java.util.Collection;
import java.util.Iterator;
import java.io.*;

/** An instance designed for a relational dataset.  Extends from
 * Example.
 * Modified by Zhenzhen Kou to include an ExmapleID
 * @author Zhenzhen Kou
 */

public class SGMExample extends Example implements Instance,Visible,Serializable
{

	static final long serialVersionUID=20071015;

	protected String ExampleID;

	public SGMExample(Instance instance,ClassLabel label , String ID) 
	{
		super(instance,label,1.0);
		this.ExampleID=ID;
	}
	public SGMExample(Instance instance,ClassLabel label, String ID,double weight) 
	{
		super(instance,label,weight);
		this.ExampleID=ID;
	}

	/** Get the ExampleID */
	public String getExampleID() { return ExampleID; }

	/** Has the ExampleID or not */
	public boolean hasID( String ID) { 
		if( ExampleID.equals(ID) )
			return true;
		else
			return false;
	}

	public String toString() { return "[ ID: "+getExampleID()+" example: "+getLabel()+" "+asInstance().toString()+"]"; }

	static public class SGMLooper extends AbstractLooper {
		public SGMLooper(Iterator i) { super(i); }
		public SGMLooper(Collection c) { super(c); }
		public SGMExample nextExample() { return (SGMExample)next(); }
	}


}
