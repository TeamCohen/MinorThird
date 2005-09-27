package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
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

/**
 * Created on Sep 27, 2005
 * 
 * @author Vitor R. Carvalho
 * 
 * "Regret Winnow" algorithm. A Winnow algorithm modified in order to minimize regret
 * according to "From External to Internal Regret", Avrim Blum & Yishay Mansour, COLT 2005
 * 
 * Additionally, it implements an optional voting scheme. (voted parameter)
 * 
 */


public class RegretWinnow extends OnlineBinaryClassifierLearner implements Serializable {
	static private final long serialVersionUID = 1;
	private final int CURRENT_SERIAL_VERSION = 1;
	
	private Hyperplane s_t, v_t;
	private double theta; //threshold parameter of Winnow (positive value)
	private double eps;//Epsilon parameter (positive value), will originate alpha and beta
	private double alpha; //Winnow promotion parameter (positive value, larger than 1, derived from eps)
	private double beta; //Winnow demotion parameter (positive value, between 0 and 1, derived from eps)
	private int excount;//number of examples presented to the learner so far
	private int numActiveFeatures;
	private double margin = 0.0;
	private boolean voted = false;
	
	//for the moment, use a hyperplane to store last values of Loss
	Hyperplane lossH, lossF;//one for the algorithm loss (only when features are awaken) and one for the features loss

	public RegretWinnow() {
		//this(4, 2, 0.5,true);
		this(1, false);
		//this(10,1.01,0.99,true);
	}

	
	public RegretWinnow(double epsilon, boolean voted) {
		if((epsilon<0)){
			System.out.println("Error in RegretWinnow initial parameters: epsilon < 0");
			System.exit(0);
		}
		this.eps = epsilon;
		this.voted = voted;
		reset();
	}

	public void reset() {
		s_t = new Hyperplane();
		v_t = new Hyperplane();
		lossF = new Hyperplane();
		lossH = new Hyperplane();
	}

	public void addExample(Example example) {
		
		excount++;//examples counter
		
//		first, initialize the hyperplane with new features
		if(excount==1){
			numActiveFeatures = Math.max(example.featureIterator().estimatedSize(),2);
			theta = Math.max(Math.ceil(numActiveFeatures/2),4);
			alpha = 1 + eps;
			beta = 1/(1+eps);			
			System.out.println("RegretWinnow parameters: Theta/Alpha/Beta = ("+theta+"/"+alpha+"/"+beta+")");
		}		
		//adding new features to hyperplane
		for (Feature.Looper j=example.featureIterator(); j.hasNext(); ) {
		    Feature f = j.nextFeature();
		    if(!s_t.hasFeature(f)){
		    	s_t.increment(f,1.0);//initialize weights to 1
		    }
		}		
		
		//get label and prediction - winnow 
		double y_t = example.getLabel().numericLabel();
		double y_t_hat = localscore(example.asInstance());
		double decision = y_t * y_t_hat;
		if(decision<margin){//error occurred
			if(example.getLabel().isPositive()){
				for (Feature.Looper j=example.featureIterator(); j.hasNext(); ) {
				    Feature f = j.nextFeature();
				    s_t.multiply(f,alpha);
				}
			}
			else{
				for (Feature.Looper j=example.featureIterator(); j.hasNext(); ) {
				    Feature f = j.nextFeature();
				    s_t.multiply(f,beta);
				}
				
			}
		}		
			
		//regret updates
		for (Feature.Looper j=example.featureIterator(); j.hasNext(); ) {
		    Feature ff = j.nextFeature();
		    if(decision > 0){
		    	lossH.increment(ff,0.0);
		    }
		    else{
		    	lossH.increment(ff,1.0);
		    }		    
		    //weight of the feature predicts the true label correctly
		    if(y_t*(example.getWeight(ff))>0){
		    	lossF.increment(ff,0);
		    }
		    else{
		    	lossF.increment(ff,1);
		    }
		    
		    //update weights
		    double deltaLoss = lossF.featureScore(ff)-(beta*lossH.featureScore(ff));		    
		    double factor = Math.pow(beta,deltaLoss);
		    s_t.multiply(ff,factor);
		}	
		
		
		
		//voting trick
		if(voted)v_t.increment( s_t, 1.0 );
	}

	public Classifier getClassifier() {
		
		if(voted){//create the new voted hyperplane
			Hyperplane z = new Hyperplane();
			z.increment(v_t);
			z.multiply(1/(double)excount);
			Classifier c = new MyClassifier(z,theta);
			//System.out.println("Hyper= "+c.toString());
			return c;
		}
		else{		//no voting
			Classifier c = new MyClassifier(s_t,theta);
			//System.out.println("Hyper= "+c.toString());
			return c;
		}
	}
	
	public double localscore(Instance ins){
		return (s_t.score(ins)-theta);
	}

	public String toString() {
		return "RegretWinnow";
	}
	
	 public class MyClassifier implements Classifier, Serializable,Visible
	 {
		static private final long serialVersionUID = 1;
		private final int CURRENT_SERIAL_VERSION = 1;
		
		private Hyperplane cl;
		private ExampleSchema schema;
		private double mytheta;//theta parameter from Winnow
		
		public MyClassifier(Hyperplane cl,double mytheta) 
		{	
		    this.cl = cl;	
		    this.mytheta=mytheta; 
		}
		//implements winnow decision rule
		public ClassLabel classification(Instance instance) 
		{
			//winnow decision rule
			double dec = cl.score(instance)- mytheta;// can be used to get probabilities?
			if(dec>=0){
		    	return new ClassLabel(ExampleSchema.POS_CLASS_NAME);//no value for the moment
		    }
		    else{
		    	return new ClassLabel(ExampleSchema.NEG_CLASS_NAME);
		    }
		}
		
		public String toString(){
			return cl.toString();
		}
		
		public String explain(Instance instance) 
		{
			return "RegretWinnow: Not implemented yet";
		}
		
		public Explanation getExplanation(Instance instance) {
		    Explanation.Node top = new Explanation.Node("RegretWinnow Explanation");
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
	 
	//main unit test routine
	public static void main(String[] args) {
			Winnow mywinnow = new Winnow();
			
			//making examples
			ClassLabel c = ClassLabel.positiveLabel(1);
			MutableInstance instance = new MutableInstance();
			instance.addNumeric(new Feature("f2"), 2);
			instance.addNumeric(new Feature("f3"), 3);
			instance.addNumeric(new Feature("f4"), 4);
			Example ex = new Example(instance, c);			
			mywinnow.addExample(ex);
			
			Classifier hp  = mywinnow.getClassifier() ;
			System.out.println("Winnow Hyperplane = "+hp.toString());
			
			ClassLabel c1 = ClassLabel.negativeLabel(-1);
			MutableInstance instance1 = new MutableInstance();
			instance1.addNumeric(new Feature("f3"), 1);
			instance1.addNumeric(new Feature("f4"), 2);
			instance1.addNumeric(new Feature("f5"), 3);
			Example ex1 = new Example(instance1, c1);			
			mywinnow.addExample(ex1);
			
			hp  = mywinnow.getClassifier() ;
			System.out.println("Winnow Hyperplane = "+hp.toString());
			
			ClassLabel c2 = ClassLabel.positiveLabel(1);
			//ClassLabel c2 = ClassLabel.negativeLabel(-1);
			MutableInstance instance2 = new MutableInstance();
			instance2.addNumeric(new Feature("f3"), -5);
			instance2.addNumeric(new Feature("f4"), -12);
			instance2.addNumeric(new Feature("f5"), -34);
			Example ex2 = new Example(instance2, c2);			
			mywinnow.addExample(ex2);
			
			hp  = mywinnow.getClassifier() ;
			System.out.println("Winnow Hyperplane = "+hp.toString());
			
			ClassLabel c3 = ClassLabel.positiveLabel(1);
			//ClassLabel c2 = ClassLabel.negativeLabel(-1);
			MutableInstance instance3 = new MutableInstance();
			instance3.addNumeric(new Feature("f3"), -5);
			instance3.addNumeric(new Feature("f4"), -12);
			instance3.addNumeric(new Feature("f5"), -34);
			instance.addNumeric(new Feature("f2"), -2);
			Example ex3 = new Example(instance3, c3);			
			mywinnow.addExample(ex3);
			
			hp  = mywinnow.getClassifier() ;
			System.out.println("Winnow Hyperplane = "+hp.toString());
			
		}
}


