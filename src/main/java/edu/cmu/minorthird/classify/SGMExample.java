package edu.cmu.minorthird.classify;

import java.io.Serializable;

import edu.cmu.minorthird.util.gui.Visible;

/**
 * An instance designed for a relational dataset. Extends from Example. Modified
 * by Zhenzhen Kou to include an ExmapleID
 * 
 * @author Zhenzhen Kou
 */

public class SGMExample extends Example implements Instance,Visible,Serializable{

	static final long serialVersionUID=20071015;

	protected String exampleId;

	public SGMExample(Instance instance,ClassLabel label,String exampleId,double weight){
		super(instance,label,weight);
		this.exampleId=exampleId;
	}
	
	public SGMExample(Instance instance,ClassLabel label,String exampleId){
		this(instance,label,exampleId,1.0);
	}

	/** Get the ExampleID */
	public String getExampleID(){
		return exampleId;
	}

	/** Has the ExampleID or not */
	public boolean hasID(String id){
		return exampleId.equals(id);
	}

	@Override
	public String toString(){
		return "[ ID: "+getExampleID()+" example: "+getLabel()+" "+asInstance().toString()+"]";
	}

}
