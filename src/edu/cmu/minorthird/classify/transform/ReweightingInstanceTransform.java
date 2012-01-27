package edu.cmu.minorthird.classify.transform;

import java.util.Iterator;

import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;

/**
 * @author William Cohen
 */

// warning: this code is untested!!!

public class ReweightingInstanceTransform extends AbstractInstanceTransform
{
	private FeatureReweighter reweighter;

	public ReweightingInstanceTransform(FeatureReweighter reweighter)
	{
		this.reweighter = reweighter;
	}

	@Override
	public Instance transform(Instance instance)
	{
		MutableInstance result = new MutableInstance(instance.getSource(), instance.getSubpopulationId());
		for (Iterator<Feature> i=instance.featureIterator(); i.hasNext(); ) {
			Feature f = i.next();
			result.addNumeric( f, reweighter.reweight(f,instance) ); 
		}
		return result;
	}

	@Override
	public String toString() { return "[ReweightInstTranform "+reweighter+"]"; }
}
