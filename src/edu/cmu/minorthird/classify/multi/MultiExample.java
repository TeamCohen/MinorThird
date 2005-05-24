package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.AbstractLooper;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

import java.util.Collection;
import java.util.Iterator;
import java.io.*;

/** An instance that is associated with a ClassLabel.  Implements the
 * Instance interface by delegating to a wrapped Instance, so
 * subclasses just need to attach the right label construct.
 *
 * @author Cameron Williams
 */

public class MultiExample implements Instance,Visible,Serializable
{
    static private final long serialVersionUID = 1;
    private final int CURRENT_VERSION_NUMBER = 1;

    protected Instance instance;
    protected MultiClassLabel label;
    protected double weight;

    public MultiExample(Instance instance,MultiClassLabel label) 
    {
	this(instance,label,1.0);
    }
    public MultiExample(Instance instance,MultiClassLabel label,double weight) 
    {
	this.instance = instance;
	this.label = label;
	this.weight = weight;
    }
    
    /** Returns the first label */
    public ClassLabel getLabel(){ 
	ClassLabel[] labels = label.getLabels();
	return labels[0];
    }

    public Example[] getExamples() {
	ClassLabel[] labels = label.getLabels();
	Example[] examples = new Example[labels.length];
	for(int i=0; i<examples.length; i++) {
	    examples[i] = new Example(instance, labels[i], weight);
	}
	return examples;
    }
    
    /** Return the subpopulation Id of the instance. **/
    final public String getSubpopulationId() { return instance.getSubpopulationId(); }

    /** Get the underlying object */
    final public Object getSource() { return instance.getSource(); }

    /** get the label associated with the underlying object */
    public MultiClassLabel getMultiLabel() { return label; }

    /** Return an iterator over all numeric features */
    final public Feature.Looper featureIterator() { return instance.featureIterator(); }

    /** Return an iterator over all binary features  */
    final public Feature.Looper binaryFeatureIterator() { return instance.binaryFeatureIterator(); }

    /** Return an iterator over all numeric features */
    final public Feature.Looper numericFeatureIterator() { return instance.numericFeatureIterator(); }

    /** Get the weight assigned to a feature in the instance.
     */
    final public double getWeight(Feature f) { return instance.getWeight(f); }

     /** Get the weight of this example. */
    final public double getWeight() { return weight; }

    /** Change the weight of this example. */
    final public void setWeight(double newWeight) { this.weight=newWeight; }

    /** Return an unlabeled version of the example (an Instance) */
    final public Instance asInstance() { return instance; }

    /** Create a viewer */
    public Viewer toGUI() { return new GUI.MultiExampleViewer(this);	}

    static public class Looper extends AbstractLooper {
	public Looper(Iterator i) { super(i); }
	public Looper(Collection c) { super(c); }
	public MultiExample nextMultiExample() { return (MultiExample)next(); }
    }
}
