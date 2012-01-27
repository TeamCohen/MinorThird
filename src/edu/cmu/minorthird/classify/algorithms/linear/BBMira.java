package edu.cmu.minorthird.classify.algorithms.linear;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.OnlineBinaryClassifierLearner;

/**
 * A budgeted version of binary MIRA.
 *
 * Status: this doesn't seem to work too well in tests.
 * The algorithm might be buggy.
 *
 * @author William Cohen
 */

public class BBMira extends OnlineBinaryClassifierLearner{

	private static Logger log=Logger.getLogger(BBMira.class);

	//private static final boolean DEBUG = false;

	private List<WeightedExample> cache;

	private Hyperplane w_t;

	private double minimalMargin=1;

	private boolean useBudget=true;

	private Set<Feature> usedFeatures;

	public BBMira(boolean useBudget,double minimalMargin){
		this.useBudget=useBudget;
		this.minimalMargin=minimalMargin;
		reset();
	}

	public BBMira(){
		this(true,1.0);
	}

	@Override
	public void reset(){
		cache=new LinkedList<WeightedExample>();
		w_t=new Hyperplane();
		usedFeatures=new TreeSet<Feature>();
	}

	@Override
	public void addExample(Example example){
		double y=example.getLabel().numericLabel();
		Instance x=example.asInstance();
		// simulate initialization of w_t to unit values for each feature
		for(Iterator<Feature> i=x.featureIterator();i.hasNext();){
			Feature f=i.next();
			if(usedFeatures.add(f)){
				w_t.increment(f,1.0);
			}
		}

		double s=w_t.score(x);
		if(log.isDebugEnabled())
			log.debug("y="+y+" s="+s+" for "+x);
		if(y*s<=minimalMargin){
			double tau_t=truncateG(-y*s/kernel(x,x));
			log.debug("update: y*s = "+y*s+" ||x||^2 = "+kernel(x,x)+" tau_t = "+
					tau_t);
			if(tau_t!=0){
				w_t.increment(example,y*tau_t);
				cache.add(new WeightedExample(example,tau_t));
				if(log.isDebugEnabled())
					log.debug("into cache, useBudget="+useBudget+" tau="+tau_t+" :"+x);
				if(useBudget)
					distillCache();
			}
		}
	}

	@Override
	public Classifier getClassifier(){
		return w_t;
	}

	// inner product of x1 and x2
	private static double kernel(Instance x1,Instance x2){
		double result=0;
		for(Iterator<Feature> i=x1.featureIterator();i.hasNext();){
			Feature f=i.next();
			result+=x1.getWeight(f)*x2.getWeight(f);
		}
		// correction of +1 is for the hyperplaneBias feature, which is
		// always invisibly present
		return result+1.0;
	}

	// Crammer & Singer's G function
	private double truncateG(double z){
		if(z<0)
			return 0.0;
		else if(z>1)
			return 1.0;
		else
			return z;
	}

	//
	// the cache
	//
	private static class WeightedExample{

		public Example example;

		public double alpha;

		public WeightedExample(Example example,double alpha){
			this.example=example;
			this.alpha=alpha;
		}

		@Override
		public String toString(){
			return "[WX: "+example+" alpha="+alpha+"]";
		}
	}

	private void distillCache(){
		log.info("distilling cache, size="+cache.size());
		boolean somethingRemoved=true;
		while(somethingRemoved){
			somethingRemoved=false;
			for(ListIterator<WeightedExample> i=cache.listIterator();i.hasNext();){
				WeightedExample wx=i.next();
				double y=wx.example.getLabel().numericLabel();
				Instance x=wx.example.asInstance();
				//double currentPrediction = cacheScore(cache,x);
				double currentPrediction=w_t.score(x);
				double wxContribution=kernel(x,x)*y*wx.alpha;
				if((currentPrediction-wxContribution)*y>=minimalMargin){
					i.remove();
					somethingRemoved=true;
					w_t.increment(x,-y*wx.alpha);
					log.info("reduced cache to "+cache.size()+" entries");
				}
			}
		}
	}

	@Override
	public String toString(){
		return "[BBMira "+useBudget+";"+minimalMargin+"]";
	}
}
