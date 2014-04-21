package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.gui.Viewer;

/**
 * An instance that is associated with a ClassLabel. Implements the Instance
 * interface by delegating to a wrapped Instance, so subclasses just need to
 * attach the right label construct.
 * 
 * @author Cameron Williams
 */

public class MultiExample extends Example{

	static final long serialVersionUID=20080125L;

	// overwrites Example.label
	protected MultiClassLabel label;

	public MultiExample(Instance instance,MultiClassLabel label,double weight){
		super(instance,null,weight);
		this.label=label;
	}

	public MultiExample(Instance instance,MultiClassLabel label){
		this(instance,label,1.0);
	}

	/** Returns the first label */
	@Override
	public ClassLabel getLabel(){
		return label.getLabels()[0];
	}
	
	/** get the label associated with the underlying object */
	public MultiClassLabel getMultiLabel(){
		return label;
	}

	/** Returns this MultiExample as separate Example's */
	public Example[] getExamples(){
		ClassLabel[] labels=label.getLabels();
		Example[] examples=new Example[labels.length];
		for(int i=0;i<examples.length;i++){
			examples[i]=new Example(instance,labels[i],weight);
		}
		return examples;
	}

	/** Create a viewer */
	@Override
	public Viewer toGUI(){
		return new GUI.MultiExampleViewer(this);
	}

}
