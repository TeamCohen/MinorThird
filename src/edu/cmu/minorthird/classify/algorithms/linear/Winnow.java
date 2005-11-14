package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.classify.OnlineBinaryClassifierLearner;
import edu.cmu.minorthird.classify.algorithms.linear.MaxEntLearner.MyClassifier;
import edu.cmu.minorthird.classify.sequential.BeamSearcher;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created on Sep 21, 2005
 * 
 * @author Vitor R. Carvalho
 * 
 * Winnow algorithm as described in "Learning Quickly when Irrelevant
 * Attributes Abound: a new linear-threshold algorithm", N. Littlestone, Machine
 * Learning, 1988. 
 * 
 * Some implementation details also described in:
 * Learning in Text Categorization", I. Dagan, Y. Karov, D. Roth, EMNLP, 1997.
 *  
 * Additionally, it implements 2 optional features: 
 * 	(a) update when examples don't satisfy a margin requirement (margin parameter)
 *  (b) optionally, classify with a voting scheme. (voted parameter)
 * 
 */


public class Winnow extends OnlineBinaryClassifierLearner implements Serializable {
	private Hyperplane s_t, v_t;
	private double theta = 1.0; //threshold parameter-normalizing examples
	private double alpha;//promotion parameter (positive value, larger than 1)
	private double beta; //demotion parameter (positive value, between 0 and 1)
	private int excount;//number of examples presented so far
	private double margin = 0.0;
	private boolean voted = false;
	private double W_MAX = Math.pow(2,200), W_MIN = 1/Math.pow(2,200);//overflow-underflow ceiling
		
	public Winnow() {
		this(1.5, 0.5,false);//default
	}
	
	public Winnow(double a, double b, boolean voted) {
		if((a < 1)||(b<0)||(b>1)){
			System.out.println("Error in Winnow initial parameters");
			System.out.println("Error: (theta<0)||(alpha < 1)||(beta<0)||(beta>1)");
			System.exit(0);
		}
		this.alpha = a;
		this.beta = b;
		this.voted = voted;
		reset();
	}

	public void reset() {
		s_t = new Hyperplane();
		if(voted)v_t = new Hyperplane();
		excount = 0;
	}
	
	public void addExample(Example example1) {
		
		excount++;//examples counter
		
		//normalize weights (divide weights by absolute value of the sum of weights in example)		
		Example example = example1.normalizeWeights();
					
		for (Feature.Looper j=example.asInstance().featureIterator(); j.hasNext(); ) {
			Feature f = j.nextFeature();
			if(!s_t.hasFeature(f)) {
				s_t.increment(f,1.0);//initialize weights to 1
			}			
		}
		
		
		//get label and prediction
		double y_t = example.getLabel().numericLabel();
		double y_t_hat = s_t.score(example.asInstance()) - theta;
		if(y_t * y_t_hat<=margin){//error occurred
			if(example.getLabel().isPositive()){
				for (Feature.Looper j=example.featureIterator(); j.hasNext(); ) {
				    Feature f = j.nextFeature();
				    double w = s_t.featureScore(f);
				    if(w<W_MAX){			    
				    	s_t.multiply(f,alpha);
				    }
				}
			}
			else{
				for (Feature.Looper j=example.featureIterator(); j.hasNext(); ) {
				    Feature f = j.nextFeature();
				    double w = s_t.featureScore(f);
				    if(w>W_MIN){
				    	s_t.multiply(f,beta);
				    }
				}
			}
		}

		if(voted)v_t.increment( s_t, 1.0 );
	}

	public Classifier getClassifier() {		
		if(voted){//create the new voted hyperplane
			Hyperplane z = new Hyperplane();
			z.increment(v_t,1/(double)excount);
			return new MyClassifier(z,theta);
		}
		else{//no voting
			return new MyClassifier(s_t,theta);
		}		
	}
		
	public String toString() {
		return "Winnow, voted="+voted;
	}
		
	 public static class MyClassifier implements Classifier, Serializable,Visible
	 {
		static private final long serialVersionUID = 1;
		private final int CURRENT_SERIAL_VERSION = 1;
		
		private Hyperplane cl;
		private ExampleSchema schema;
		private double theta;
		
		public MyClassifier(Hyperplane cl,double mytheta) 
		{	
		    this.cl = cl;	
		    this.theta=mytheta;
		}
		
		//implements winnow decision rule
		public ClassLabel classification(Instance ins) 
		{
			Example ex = new Example(ins, new ClassLabel("POS"));
			Example example1 = filterFeat(ex);
			Example example2 = example1.normalizeWeights();
			Instance instance = example2.asInstance();			
			double dec = cl.score(instance)- theta;
			return dec>=0 ? ClassLabel.positiveLabel(dec) : ClassLabel.negativeLabel(dec);
		}
		
		//only consider features present in the hyperplane - disregard others
		//better normalization accuracy
		public Example filterFeat(Example ex){
			MutableInstance ins= new MutableInstance();
			for(Feature.Looper i=ex.asInstance().featureIterator(); i.hasNext();){
				Feature f = i.nextFeature();			
				if(cl.hasFeature(f)){
					ins.addNumeric(f,ex.getWeight(f));
				}
			}			
			return new Example(ins,ex.getLabel());
		}
		
		public String toString(){
			return cl.toString();
		}
		
		public String explain(Instance instance) 
		{
			return "Winnow: Not implemented yet";
		}
		
		public Explanation getExplanation(Instance instance) {
		    Explanation.Node top = new Explanation.Node("Winnow Explanation");
		    Explanation ex = new Explanation(top);		    
		    return ex;
		}

		public Viewer toGUI()
		{
		    Viewer v = new TransformedViewer(new SmartVanillaViewer()) {
			    public Object transform(Object o) {
				MyClassifier mycl = (MyClassifier)o;
				return (Classifier)mycl.cl;
			    }
			};
		    v.setContent(this);
		    return v;
		}
	 }
	 
}


