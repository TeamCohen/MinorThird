package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;

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

	public Instance transform(Instance instance)
	{
		MutableInstance result = new MutableInstance(instance.getSource(), instance.getSubpopulationId());
		for (Feature.Looper i=instance.featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			result.addNumeric( f, reweighter.reweight(f,instance) ); 
		}
		return result;
	}

	public String toString() { return "[ReweightInstTranform "+reweighter+"]"; }
}
