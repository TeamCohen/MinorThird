package edu.cmu.minorthird.classify;

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
 * @author William Cohen
*/

public class Example implements Instance,Visible,Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

	protected Instance instance;
	protected ClassLabel label = null;

	public Example(Instance instance,ClassLabel label) 
	{
		this.instance = instance;
		this.label = label;
	}

	/** compress the example, if possible */
	public Example compress()
	{
		if (instance instanceof CompactInstance) return this;
		else return new Example(new CompactInstance(instance),label);
	}

	/** get the label associated with the underlying object */
	public ClassLabel getLabel() { return label; }

	/** Get the underlying object */
	final public Object getSource() { return instance.getSource(); }

	/** Get the weight assigned to a feature in the instance.
	 */
	final public double getWeight(Feature f) { return instance.getWeight(f); }

	/** Return an iterator over all binary features  */
	final public Feature.Looper binaryFeatureIterator() { return instance.binaryFeatureIterator(); }

	/** Return an iterator over all numeric features */
	final public Feature.Looper numericFeatureIterator() { return instance.numericFeatureIterator(); }

	/** Return an iterator over all numeric features */
	final public Feature.Looper featureIterator() { return instance.featureIterator(); }

	/** Get the weight of this instance.  Not final, since we might want
	 * to re-weight examples locally in a dataset. */
	public double getWeight() { return instance.getWeight(); }

	/** Return the subpopulation Id of the instance. **/
	final public String getSubpopulationId() { return instance.getSubpopulationId(); }

	/** Return an unlabeled version of the example (an Instance) */
	final public Instance asInstance() { return instance; }

	public String toString() { return "[example: "+getLabel()+" "+asInstance().toString()+"]"; }

	/** Create a viewer */
	public Viewer toGUI() { return new GUI.ExampleViewer(this);	}

	static public class Looper extends AbstractLooper {
		public Looper(Iterator i) { super(i); }
		public Looper(Collection c) { super(c); }
		public Example nextExample() { return (Example)next(); }
	}
}
