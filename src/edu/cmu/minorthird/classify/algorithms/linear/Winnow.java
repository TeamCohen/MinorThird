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
 * Created on Sep 21, 2005
 * 
 * @author Vitor R. Carvalho
 * 
 * Winnow algorithm as described in "Learning Quickly when Irrelevant
 * Attributes Abound: a new linear-threshold algorithm", N. Littlestone, Machine
 * Learning, 1988. 
 * 
 * Notation and implementation details based on "Mistake-Driven
 * Learning in Text Categorization", I. Dagan, Y. Karov, D. Roth, EMNLP, 1997.
 * 
 * 
 * Additionally, it implements 2 optional features: 
 * 	(a) update when examples don't satisfy a margin requirement (margin parameter)
 *  (b) optionally, classify with a voting scheme. (voted parameter)
 * 
 */


public class Winnow extends OnlineBinaryClassifierLearner implements Serializable {
	private Hyperplane s_t, v_t;
	private double theta; //threshold parameter (positive value)
	private double alpha;//promotion parameter (positive value, bigger than 1)
	private double beta; //demotion parameter (positive value, between 0 and 1)
	private int excount;//number of examples presented to the learner so far
	private int numActiveFeatures;
	private double margin = 0.0;
	private boolean voted = true;

	public Winnow() {
		//this(4, 2, 0.5,true);
		this(10, 2, 0.5, true);
		//this(10,1.01,0.99,true);
	}

	/**
	 * If ignoreWeights is true, treat all weights as binary. For backward
	 * compatibility with an older buggy version. - need to confirm with Wcohen
	 */
	public Winnow(double t, double a, double b, boolean voted) {
		if((t<0)||(a < 1)||(b<0)||(b>1)){
			System.out.println("Error in BalancedWinnow initial parameters");
			System.out.println("Error: (theta<0)||(alpha < 1)||(beta<0)||(beta>1)");
			System.exit(0);
		}
		this.theta = t;
		this.alpha = a;
		this.beta = b;
		this.voted = voted;
		reset();
	}

	public void reset() {
		s_t = new Hyperplane();
		v_t = new Hyperplane();
	}

	public void addExample(Example example) {
		
		excount++;//examples counter
		
//		first, initialize the hyperplane with new features
		if(excount==1){
			numActiveFeatures = Math.max(example.featureIterator().estimatedSize(),2);
			System.out.println("Balanced Winnow parameters: Theta/Alpha/Beta = ("+theta+"/"+alpha+"/"+beta+")");
		}		
		//adding new features to hyperplane
		for (Feature.Looper j=example.featureIterator(); j.hasNext(); ) {
		    Feature f = j.nextFeature();
		    if(!s_t.hasFeature(f)){
		    	s_t.increment(f,theta/(double)numActiveFeatures);//starting according to EMNLP97's implementation
		    }
		}		
		
		//get label and prediction
		double y_t = example.getLabel().numericLabel();
		double y_t_hat = localscore(example.asInstance());
		if(y_t * y_t_hat<margin){//error occurred
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
		return "Winnow";
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


