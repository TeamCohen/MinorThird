package edu.cmu.minorthird.classify.algorithms.linear;

import java.io.Serializable;
import java.util.Iterator;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.classify.OnlineBinaryClassifierLearner;

/**
 * Created on Sep 27, 2005
 * 
 * @author Vitor R. Carvalho
 * 
 * ROMMA (Relaxed Online Maximum Margin Algorithm) algorithm as described in
 * "The Relaxed Online Maximum Margin Algorithm", by Yi Li and Philip M. Long,
 * Machine Learning, 46(1-3):361-387,2002.
 * 
 * Additionally, it implements the Aggressive version(Aggressive ROMMA). (parameter aggressive) 
 * Additionally, it implements optionally a voting scheme. (voted parameter)
 *  
 */

public class ROMMALearner extends OnlineBinaryClassifierLearner implements
		Serializable {
	static final long serialVersionUID=20080130L;
	private Hyperplane w_t;//positive and negative hyperplanes
	private Hyperplane vpos_t;//voted hyperplane
	private int excount;//number of examples presented to the learner so far
//	private int numActiveFeatures;//number of active features in first example
	private double margin = 0.0;
	private boolean aggressive = false;
	private boolean voted;
	

	public ROMMALearner() {
		//this(false, false);
		//this(true, false);
		this(true, true);//recommended
	}

	public ROMMALearner(boolean agg, boolean voted) {
		this.aggressive = agg;
		if(aggressive) margin = 1.0;
		this.voted = voted;
		reset();
	}

	@Override
	public void reset() {
		w_t = new Hyperplane();
		if (voted) {
			vpos_t = new Hyperplane();
		}
	}

	@Override
	public void addExample(Example example) {

		excount++;
		
//		adding new features to hyperplane
		for (Iterator<Feature> j=example.featureIterator(); j.hasNext(); ) {
		    Feature f = j.next();
		    if(!w_t.hasFeature(f)){
		    	w_t.increment(f,1.0);
		    }
		}
		//System.out.println("Hyperplane = "+w_t.toString());

		//get label and prediction
		double y_t = example.getLabel().numericLabel();
		double y_t_hat = w_t.score(example.asInstance());
		double decision = y_t * y_t_hat; 
		//update rule
		if (decision < margin) {
			double x2 = getNormSquared(example.asInstance());
			double w2 = getHyperplaneNormSquared(w_t);			
			double denom = ((x2*w2)-(y_t_hat*y_t_hat));
			
			double c_t = ((x2*w2)-(decision));
			double d_t = (w2*(y_t-y_t_hat));
			
			if((aggressive)&&(decision>=w2*x2)){
				w_t = new Hyperplane();
				w_t.increment(example.asInstance(),y_t/x2);
			}
			else{
				//System.out.println("x2,w2,ct,dt,decision ="+x2+","+w2+","+c_t+","+d_t+","+decision);
				if((denom>0)||(denom<0)){
					if ((c_t>0)||(c_t<0))w_t.multiply(c_t/denom);
					if ((d_t>0)||(d_t<0))w_t.increment(example.asInstance(),d_t/denom);
				}
			}
		}
		
		//averaging trick
		if (voted) {
			vpos_t.increment(w_t, 1.0);//smooth
		}
	}

	public double getNormSquared(Instance ins) {
		double tmp = 0.0;
		for (Iterator<Feature> j = ins.featureIterator(); j.hasNext();) {
			Feature f = j.next();
			double val = ins.getWeight(f);
			tmp += val * val;
		}
		return tmp;
	}
	
	public double getHyperplaneNormSquared(Hyperplane hyp) {
		double tmp = 0.0;
		for (Iterator<Feature> j = hyp.featureIterator(); j.hasNext();) {
			Feature f = j.next();
			double val = hyp.featureScore(f);
			tmp += val * val;
		}
		return tmp;
	}

	@Override
	public Classifier getClassifier() {
		Hyperplane z = new Hyperplane();
		if (voted)
			z.increment(vpos_t);
		else
			z.increment(w_t);
		return z;
	}

	@Override
	public String toString() {
		return "ROMMA Algorithm";
	}

	//main unit test routine
	public static void main(String[] args) {
		ROMMALearner pa = new ROMMALearner();

		//making examples
		ClassLabel c = ClassLabel.positiveLabel(1);
		MutableInstance instance = new MutableInstance();
		instance.addNumeric(new Feature("f2"), 2);
		instance.addNumeric(new Feature("f3"), 3);
		instance.addNumeric(new Feature("f4"), 4);
		Example ex = new Example(instance, c);
		pa.addExample(ex);

		Classifier hp = pa.getClassifier();
		System.out.println("Hyperplane = " + hp.toString());

		ClassLabel c1 = ClassLabel.negativeLabel(-1);
		MutableInstance instance1 = new MutableInstance();
		instance1.addNumeric(new Feature("f3"), 1);
		instance1.addNumeric(new Feature("f4"), 2);
		instance1.addNumeric(new Feature("f5"), 3);
		Example ex1 = new Example(instance1, c1);
		pa.addExample(ex1);

		hp = pa.getClassifier();
		System.out.println("Hyperplane = " + hp.toString());

		ClassLabel c2 = ClassLabel.positiveLabel(1);
		//ClassLabel c2 = ClassLabel.negativeLabel(-1);
		MutableInstance instance2 = new MutableInstance();
		instance2.addNumeric(new Feature("f3"), -5);
		instance2.addNumeric(new Feature("f4"), -12);
		instance2.addNumeric(new Feature("f5"), -34);
		Example ex2 = new Example(instance2, c2);
		pa.addExample(ex2);

		hp = pa.getClassifier();
		System.out.println("Hyperplane = " + hp.toString());

		ClassLabel c3 = ClassLabel.positiveLabel(1);
		//ClassLabel c2 = ClassLabel.negativeLabel(-1);
		MutableInstance instance3 = new MutableInstance();
		instance3.addNumeric(new Feature("f3"), -5);
		instance3.addNumeric(new Feature("f4"), -12);
		instance3.addNumeric(new Feature("f5"), -34);
		instance.addNumeric(new Feature("f2"), -2);
		Example ex3 = new Example(instance3, c3);
		pa.addExample(ex3);

		hp = pa.getClassifier();
		System.out.println("Hyperplane = " + hp.toString());

	}
}

