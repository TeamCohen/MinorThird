package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.*;
import java.util.*;
import java.io.*;
import org.apache.log4j.*;

/**
 * A budgeted version of binary MIRA.
 *
 * @author William Cohen
 */


public class BBMira extends OnlineBinaryClassifierLearner
{
	private static Logger log = Logger.getLogger(BBMira.class);

	private LinkedList cache;
	private double minimalMargin = 0.001;
	private boolean useBudget=true;

	public BBMira(boolean useBudget,double minimalMargin ) { 
		this.useBudget = useBudget;
		this.minimalMargin = minimalMargin; 
		reset(); 
	}
	public BBMira() { this(true,0.001); }

	public void reset() { cache = new LinkedList(); }

	public void addExample(Example example)
	{
		double y = example.getLabel().numericScore();
		Instance x = example.asInstance();
		double s = cacheScore(cache,x);
		if (y*s <= minimalMargin) {
			double tau = truncateG( - y*s / kernel(x,x) );
			cache.add( new WeightedExample( example,  tau ) );
			//log.info("into cache, tau="+tau+" :"+x);
			if (useBudget) distillCache();
		}
	}

	public Classifier getClassifier() { return new KernelClassifier(cache); }

	// inner product of x1 and x2
	private static double kernel(Instance x1,Instance x2)
	{
		double result = 0;
		for (Feature.Looper i = x1.featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			result += x1.getWeight(f) * x2.getWeight(f);
		}
    // correction of +1 is for the hyperplaneBias feature, which is
    // always invisibly present
		return result + 1.0; 
	}

	// Crammer & Singer's G function
	static private double truncateG(double z)
	{
		if (z<0) return 0.0;
		else if (z>1) return 1.0;
		else return z;
	}

	//
	// the cache
	//
	private static class WeightedExample {
		public Example example;
		public double alpha;
		public WeightedExample(Example example,double alpha) { this.example=example; this.alpha=alpha; }
		public String toString()	{	return "[WX: "+example+" alpha="+alpha+"]"; }
	}

	private void distillCache()
	{
		boolean somethingRemoved=true;
		while (somethingRemoved) {
			somethingRemoved = false;
			for (ListIterator i=cache.listIterator(); i.hasNext(); ) {
				WeightedExample wx = (WeightedExample)i.next();
				double y = wx.example.getLabel().numericScore();
				Instance x = wx.example.asInstance();
				double currentPrediction = cacheScore(cache,x);
				double wxContribution = kernel(x,x) * wx.alpha * y;
				if ((currentPrediction - wxContribution)*y >= minimalMargin) {
					i.remove();
					somethingRemoved = true;
					log.info("reduced cache to "+cache.size()+" entries");
				}
			}
		}
	}

	static private double cacheScore(List cache,Instance x)
	{
		double result = 0;
		for (Iterator i=cache.iterator(); i.hasNext(); ) {
			WeightedExample wx = (WeightedExample)i.next();
			double y = wx.example.getLabel().numericScore();
			double delta = kernel(x, wx.example.asInstance())* wx.alpha * y;
			log.debug("score += "+delta+" from "+wx);
			result += delta;
		}
		// this +1 correction is because W can't be initialized to
		// zero...
		return result + 1.0;
	}

	public static class KernelClassifier extends BinaryClassifier implements Serializable
	{
		private List cache;
		public KernelClassifier(List cache) { this.cache=cache; }
		public double score(Instance instance) { return cacheScore(cache,instance); }
		public String explain(Instance instance) { return "not implemented"; }
	}
}
