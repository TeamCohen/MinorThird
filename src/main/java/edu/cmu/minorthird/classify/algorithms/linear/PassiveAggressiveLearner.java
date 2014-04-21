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
 * Created on Sep 26, 2005
 * 
 * @author Vitor R. Carvalho
 * 
 * Passive Aggressive algorithm as described in "Online Passive Agressive Algorithms"
 * Cramer, Dekel, Shalev-Shwartz, Singer, NIPS 2003. 
 * 
 * Classification mode.
 *   
 * Additionally, it implements optionally a voting scheme. (voted parameter)
 * 
 */


public class PassiveAggressiveLearner extends OnlineBinaryClassifierLearner implements Serializable {
	static final long serialVersionUID=20080130L;
	private Hyperplane pos_t;//positive and negative hyperplanes 
	private Hyperplane vpos_t;//voted hyperplane
	private double eta; //insensitivity parameter (positive value)
	private double gamma; //parameter for the unrealizable case
	private int excount;//number of examples presented to the learner so far
	//private int numActiveFeatures;//number of active features in first example
	private boolean voted;

	public PassiveAggressiveLearner() {
		//this(4, 2, 0.5, true);
		this(1, 0.1, true);//recommended
		//this(10,1.1,0.9, true);
	}

	public PassiveAggressiveLearner(double eta, double gamma, boolean voted) {
		this.eta = eta;
		this.gamma = gamma;
		this.voted = voted;
		reset();		
	}

	@Override
	public void reset() {
		pos_t = new Hyperplane();
		if(voted){
			vpos_t = new Hyperplane();
		}		
	}

	@Override
	public void addExample(Example example) {
		
		excount++;
		
		//get label and prediction
		double y_t = example.getLabel().numericLabel();
		double y_t_hat = pos_t.score(example.asInstance());
		//update rule
		if(y_t * y_t_hat < eta){
			double loss = eta - (y_t * y_t_hat);
			double normsqr = getNormSquared(example.asInstance());
			double weight = y_t*(loss/(normsqr + gamma));
			pos_t.increment(example.asInstance(),weight);
		}
		//averaging trick
		if(voted){
			vpos_t.increment( pos_t, 1.0);//smooth
		}		
	}
	
	public double getNormSquared(Instance ins){
		double tmp = 0.0;
		for (Iterator<Feature> j=ins.featureIterator(); j.hasNext(); ) {
		    Feature f = j.next();
		    double val = ins.getWeight(f);
		    tmp += val*val;
		}
		return tmp;
	}

	@Override
	public Classifier getClassifier() {	
		Hyperplane z = new Hyperplane();
		if(voted) z.increment(vpos_t);
		else z.increment(pos_t);
		return z;
	}
	
	@Override
	public String toString() {
		return "PassiveAggressive Algorithm";
	}
	
		//main unit test routine
	public static void main(String[] args) {
			PassiveAggressiveLearner pa = new PassiveAggressiveLearner();
			
			//making examples
			ClassLabel c = ClassLabel.positiveLabel(1);
			MutableInstance instance = new MutableInstance();
			instance.addNumeric(new Feature("f2"), 2);
			instance.addNumeric(new Feature("f3"), 3);
			instance.addNumeric(new Feature("f4"), 4);
			Example ex = new Example(instance, c);			
			pa.addExample(ex);
			
			Classifier hp  = pa.getClassifier() ;
			System.out.println("Winnow Hyperplane = "+hp.toString());
			
			ClassLabel c1 = ClassLabel.negativeLabel(-1);
			MutableInstance instance1 = new MutableInstance();
			instance1.addNumeric(new Feature("f3"), 1);
			instance1.addNumeric(new Feature("f4"), 2);
			instance1.addNumeric(new Feature("f5"), 3);
			Example ex1 = new Example(instance1, c1);			
			pa.addExample(ex1);
			
			hp  = pa.getClassifier() ;
			System.out.println("Winnow Hyperplane = "+hp.toString());
			
			ClassLabel c2 = ClassLabel.positiveLabel(1);
			//ClassLabel c2 = ClassLabel.negativeLabel(-1);
			MutableInstance instance2 = new MutableInstance();
			instance2.addNumeric(new Feature("f3"), -5);
			instance2.addNumeric(new Feature("f4"), -12);
			instance2.addNumeric(new Feature("f5"), -34);
			Example ex2 = new Example(instance2, c2);			
			pa.addExample(ex2);
			
			hp  = pa.getClassifier() ;
			System.out.println("Winnow Hyperplane = "+hp.toString());
			
			ClassLabel c3 = ClassLabel.positiveLabel(1);
			//ClassLabel c2 = ClassLabel.negativeLabel(-1);
			MutableInstance instance3 = new MutableInstance();
			instance3.addNumeric(new Feature("f3"), -5);
			instance3.addNumeric(new Feature("f4"), -12);
			instance3.addNumeric(new Feature("f5"), -34);
			instance.addNumeric(new Feature("f2"), -2);
			Example ex3 = new Example(instance3, c3);			
			pa.addExample(ex3);
			
			hp  = pa.getClassifier() ;
			System.out.println("Winnow Hyperplane = "+hp.toString());
			
		}
}


