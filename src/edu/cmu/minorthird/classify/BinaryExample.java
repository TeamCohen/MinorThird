package edu.cmu.minorthird.classify;



/** A single example for a learner, labeled with a real number, typically +1 or -1.
 *
 * @author William Cohen
*/

public class BinaryExample extends Example
{
	private double numericLabel = 0;

	public BinaryExample(Instance instance,double numericLabel) 
	{ 
		super(instance, ClassLabel.binaryLabel(numericLabel));
		this.numericLabel=numericLabel; 
	}
	public BinaryExample(Instance instance,ClassLabel label)
	{
		super(instance,label); 
		this.numericLabel = label.numericScore();
	}

	public Example compress()
	{
		if (instance instanceof CompactInstance) return this;
		else return new BinaryExample(new CompactInstance(instance),label);
	}

	public double getNumericLabel() { return numericLabel; }
	public String toString() { return "[binaryExample: "+getNumericLabel()+" "+asInstance().toString()+"]"; }
}
