/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.knn;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.DatasetIndex;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.MathUtil;

/** A k-nearest neighbor classifier. This is based on the
 * distance-weighted cosine classifiers introduced by Yang, eg in "An
 * Evaluation of Statistical Approaches to Text Categorization",
 * Information Retrieval, 1(1/2), pp 69--90, 1999.
 *
 * @author William Cohen
 */

class KnnClassifier implements Classifier,Serializable{

	static private final long serialVersionUID=1;

	private static Logger log=Logger.getLogger(KnnClassifier.class);

	private static final boolean DEBUG=log.isDebugEnabled();

	private DatasetIndex index;

	private ExampleSchema schema;

	private int k;

	public KnnClassifier(DatasetIndex index,ExampleSchema schema,int k){
		this.index=index;
		this.schema=schema;
		this.k=k;
		if(DEBUG)
			log.debug("knn classifier for index:\n"+index);
	}

	@Override
	public ClassLabel classification(Instance instance){
		if(DEBUG)
			log.debug("classifying: "+instance);
		// compute distance to neighbors
		Set<Neighbor> set=new TreeSet<Neighbor>();
		for(Iterator<Example> i=index.getNeighbors(instance);i.hasNext();){
			Example e=i.next();
			double sim=computeSimilarity(instance,e);
			set.add(new Neighbor(e,sim));
		}
		// compute weighted sim of distances
		double tot=0.0;
		Map<String,Double> classCounts=new HashMap<String,Double>();
		int num=0;
		for(Iterator<Neighbor> j=set.iterator();num++<k&&j.hasNext();){
			Neighbor n=j.next();
			String s=n.e.getLabel().bestClassName();
			double w=n.e.getWeight()*n.sim;
			Double d=classCounts.get(s);
			if(d==null)
				classCounts.put(s,(d=new Double(0)));
			classCounts.put(s,new Double(d.doubleValue()+w));
			tot+=w;
			if(DEBUG){
				log.debug("neighbor: "+n.e+" distance: "+n.sim+" weight: "+w+" count["+
						s+"]: "+classCounts.get(s));
			}
		}
		if(tot==0||Double.isNaN(tot)){
			if(Double.isNaN(tot))
				log.warn("total similarity to neighbors is not defined for: "+instance);
			// if no neighbors, use class priors instead, by putting
			// them in classCounts
			tot=0;
			for(int i=0;i<schema.getNumberOfClasses();i++){
				String s=schema.getClassName(i);
				double d=index.size(s);
				classCounts.put(s,new Double(d));
				tot+=d;
			}
		}

		// create a new classlabel with log odds
		ClassLabel result=new ClassLabel();
		for(Iterator<String> i=classCounts.keySet().iterator();i.hasNext();){
			String s=i.next();
			double d=classCounts.get(s);
			result.add(s,Math.log(d/tot+0.001)-Math.log((tot-d)/tot+0.001));
		}
		return result;
	}

	private static class Neighbor implements Comparable<Neighbor>{

		Example e;

		double sim;

		public Neighbor(Example e,double sim){
			this.e=e;
			this.sim=sim;
		}

		@Override
		public int compareTo(Neighbor n){
			return MathUtil.sign(n.sim-sim);
		}
	}

	@Override
	public String explain(Instance instance){
		return "not implemented";
	}

	@Override
	public Explanation getExplanation(Instance instance){
		Explanation ex=new Explanation(explain(instance));
		return ex;
	}

	// cosine distance
	private double computeSimilarity(Instance a,Instance b){
		double aNorm=0,dotProd=0;
		for(Iterator<Feature> i=a.featureIterator();i.hasNext();){
			Feature f=i.next();
			double aw=a.getWeight(f);
			double bw=b.getWeight(f);
			aNorm+=aw*aw;
			dotProd+=aw*bw;
		}
		double bNorm=0;
		for(Iterator<Feature> i=b.featureIterator();i.hasNext();){
			Feature f=i.next();
			double bw=b.getWeight(f);
			bNorm+=bw*bw;
		}
		return dotProd/(Math.sqrt(aNorm)*Math.sqrt(bNorm));
	}
}
