/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.knn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.MathUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.io.*;

/** A k-nearest neighbor classifier. This is based on the
 * distance-weighted cosine classifiers introduced by Yang, eg in "An
 * Evaluation of Statistical Approaches to Text Categorization",
 * Information Retrieval, 1(1/2), pp 69--90, 1999.
 *
 * @author William Cohen
 */

class KnnClassifier implements Classifier,Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

	private static Logger log = Logger.getLogger(KnnClassifier.class);
	private static final boolean DEBUG = log.isDebugEnabled();

	private DatasetIndex index;
	private ExampleSchema schema;
	private int k;
	

	public KnnClassifier(DatasetIndex index,ExampleSchema schema,int k)
	{
		this.index = index;
		this.schema = schema;
		this.k = k; 
		if (DEBUG) log.debug("knn classifier for index:\n"+index);
	}

	public ClassLabel classification(Instance instance)
	{
		if (DEBUG) log.debug("classifying: "+instance);
		// compute distance to neighbors
		TreeSet set = new TreeSet();
		for (Example.Looper i=index.getNeighbors(instance); i.hasNext(); ) {
			Example e = i.nextExample();
			double sim = computeSimilarity(instance,e);
			set.add( new Neighbor(e,sim) );
		}
		// compute weighted sim of distances
		double tot = 0.0;
		HashMap classCounts = new HashMap();
		int num=0;
		for (Iterator j=set.iterator(); num++<k && j.hasNext(); ) {
			Neighbor n = (Neighbor) j.next();
			String s = n.e.getLabel().bestClassName();
			double w = n.e.getWeight() * n.sim;
			Double d = (Double) classCounts.get( s );
			if (d==null) classCounts.put(s, (d = new Double(0)) );
			classCounts.put( s, new Double(d.doubleValue() + w ) );
			tot += w;
			if (DEBUG) 
				log.debug("neighbor: "+n.e+" distance: "+n.sim+" weight: "+w+" count["+s+"]: "+classCounts.get(s) );
		}

		// create a new classlabel with log odds
		ClassLabel result = new ClassLabel();
		for (Iterator i = classCounts.keySet().iterator(); i.hasNext(); ) {
			String s = (String) i.next();
			double d = ((Double)classCounts.get( s )).doubleValue();
			result.add(s, Math.log( d/tot + 0.001 ) - Math.log( (tot-d)/tot + 0.001 ) ); 
		}
		return result;
	}

	private static class Neighbor implements Comparable
	{
		Example e;
		double sim; 
		public Neighbor(Example e,double sim)
		{
			this.e = e;
			this.sim = sim;
		}
		public int compareTo(Object o)
		{
			return MathUtil.sign( ((Neighbor)o).sim - sim);
		}
	}

	public String explain(Instance instance) { return "not implemented";}

	// cosine distance
	private double computeSimilarity(Instance a,Instance b)
	{
		double aNorm=0, dotProd=0; 
		for (Feature.Looper i=a.featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			double aw = a.getWeight(f);
			double bw = b.getWeight(f);
			aNorm += aw*aw;
			dotProd += aw*bw;
		}
		double bNorm=0; 
		for (Feature.Looper i=b.featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			double bw = b.getWeight(f);
			bNorm += bw*bw;
		}
		return dotProd / (Math.sqrt(aNorm)*Math.sqrt(bNorm));
	}
}
