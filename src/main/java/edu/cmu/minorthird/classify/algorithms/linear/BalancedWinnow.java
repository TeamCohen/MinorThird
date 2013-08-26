package edu.cmu.minorthird.classify.algorithms.linear;

import java.io.Serializable;
import java.util.Iterator;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.classify.OnlineBinaryClassifierLearner;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Created on Sep 22, 2005
 * 
 * @author Vitor R. Carvalho
 * 
 * Balanced Winnow algorithm as described in "Learning Quickly when Irrelevant
 * Attributes Abound: a new linear-threshold algorithm", N. Littlestone, Machine
 * Learning, 1988. 
 * 
 * Notation and some implementation details from "Mistake-Driven
 * Learning in Text Categorization", I. Dagan, Y. Karov, D. Roth, EMNLP, 1997
 *  
 * Additionally, it implements 2 optional features: 
 * 	(a) update when examples don't satisfy a margin requirement (margin parameter)
 *  (b) optionally, classify with a voting scheme. (voted parameter)
 * 
 */

public class BalancedWinnow extends OnlineBinaryClassifierLearner implements
		Serializable{

	static final long serialVersionUID=20080128L;
	
	private Hyperplane pos_t,neg_t;//positive and negative hyperplanes 

	private Hyperplane vpos_t,vneg_t;//voted hyperplanes

	private double theta=1.0; //threshold parameter (positive value)

	private double alpha;//promotion parameter (positive value, bigger than 1)

	private double beta; //demotion parameter (positive value, between 0 and 1)

	private int excount,votedCount;//number of examples presented to the learner so far

	private double margin=0.0;

	private boolean voted=false;

	private double W_MAX=Math.pow(2,200),W_MIN=1/Math.pow(2,200);

	public BalancedWinnow(){
		this(1.5,0.5,false);
	}

	/**
	 * Constructor: voted="true" (more stable) or "false"(more aggressive)
	 * @param voted
	 */
	public BalancedWinnow(boolean voted){
		this(1.5,0.5,voted);
	}

	public BalancedWinnow(double a,double b,boolean voted){
		if((a<1)||(b<0)||(b>1)){
			System.out.println("Error in BalancedWinnow initial parameters");
			System.out
					.println("This should never happen: (theta<0)||(alpha < 1)||(beta<0)||(beta>1)");
			System.exit(0);
		}
		this.alpha=a;
		this.beta=b;
		this.voted=voted;
		reset();
	}

	@Override
	public void reset(){
		pos_t=new Hyperplane();
		neg_t=new Hyperplane();
		excount=0;
		if(voted){
			votedCount=0;
			vpos_t=new Hyperplane();
			vneg_t=new Hyperplane();
		}
	}

	@Override
	public void addExample(Example example2){
		excount++;
		Example example=Winnow.normalizeWeights(example2,true);

		for(Iterator<Feature> j=example.asInstance().featureIterator();j.hasNext();){
			Feature f=j.next();
			if(!pos_t.hasFeature(f)){
				pos_t.increment(f,2.0);//initialize pos weights to 2
				neg_t.increment(f,1.0);
			}
		}

		//get label and prediction
		double y_t=example.getLabel().numericLabel();
		double y_t_hat=localscore(example.asInstance());
		//update rule
		if(y_t*y_t_hat<=margin){//error occurred

			if((voted)){
				if(votedCount==0)
					updateVotedHyperplane(1);
				else
					updateVotedHyperplane(votedCount);
				votedCount=1;
			}

			if(example.getLabel().isPositive()){
				for(Iterator<Feature> j=example.featureIterator();j.hasNext();){
					Feature f=j.next();
					//under and overflow - ceiling
					if(pos_t.featureScore(f)<W_MAX)
						pos_t.multiply(f,alpha);
					if(neg_t.featureScore(f)>W_MIN)
						neg_t.multiply(f,beta);
				}
			}else{
				for(Iterator<Feature> j=example.featureIterator();j.hasNext();){
					Feature f=j.next();
					if(pos_t.featureScore(f)>W_MIN)
						pos_t.multiply(f,beta);
					if(neg_t.featureScore(f)<W_MAX)
						neg_t.multiply(f,alpha);
				}
			}
		}else{
			votedCount++;
		}
	}

	public void updateVotedHyperplane(double count){
		vpos_t.increment(pos_t,count);
		vneg_t.increment(neg_t,count);
		votedCount=0;
	}

	@Override
	public Classifier getClassifier(){
		if(voted){
			updateVotedHyperplane(votedCount);//first, update it
			Hyperplane zpos=new Hyperplane();
			Hyperplane zneg=new Hyperplane();
			zpos.increment(vpos_t,1/(double)excount);
			zneg.increment(vneg_t,1/(double)excount);
			return new MyClassifier(zpos,zneg,theta);
		}else{
			return new MyClassifier(pos_t,neg_t,theta);
		}
	}

	public double localscore(Instance ins){
		return(pos_t.score(ins)-neg_t.score(ins)-theta);
	}

	@Override
	public String toString(){
		return "BalancedWinnow, voted="+voted;
	}

	public class MyClassifier implements Classifier,Serializable,Visible{

		static private final long serialVersionUID=20080128L;

		private Hyperplane pos_h,neg_h;

		//private ExampleSchema schema;

		private double mytheta;

		public MyClassifier(Hyperplane pos_h,Hyperplane neg_h,double mytheta){
			this.pos_h=pos_h;
			this.neg_h=neg_h;
			this.mytheta=mytheta;
		}

		//implements winnow decision rule
		@Override
		public ClassLabel classification(Instance instance1){
			//winnow decision rule
			Example a1=new Example(instance1,new ClassLabel("POS"));
			Example aa=filterFeat(a1);
			Example example1=Winnow.normalizeWeights(aa,true);
			Instance instance=example1.asInstance();
			double dec=pos_h.score(instance)-neg_h.score(instance)-mytheta;
			return dec>=0?ClassLabel.positiveLabel(dec):ClassLabel.negativeLabel(dec);
		}

//		only consider features in the hyperplane - disregard others
		public Example filterFeat(Example ex){
			MutableInstance ins=new MutableInstance();
			for(Iterator<Feature> i=ex.asInstance().featureIterator();i.hasNext();){
				Feature f=i.next();
				if(pos_h.hasFeature(f)){
					ins.addNumeric(f,ex.getWeight(f));
				}
			}
			return new Example(ins,ex.getLabel());
		}

		@Override
		public String toString(){
			return "POS = "+pos_h.toString()+"\nNEG = "+neg_h.toString();
		}

		@Override
		public String explain(Instance instance){
			return "BalancedWinnow: Not implemented yet";
		}

		@Override
		public Explanation getExplanation(Instance instance){
			Explanation.Node top=new Explanation.Node("BalancedWinnow Explanation");
			Explanation ex=new Explanation(top);
			return ex;
		}

		@Override
		public Viewer toGUI(){
			Viewer v=new TransformedViewer(new SmartVanillaViewer()){
				static final long serialVersionUID=20080128L;
				@Override
				public Object transform(Object o){
					MyClassifier mycl=(MyClassifier)o;
					return mycl.pos_h;//bug!
				}
			};
			v.setContent(this);
			return v;
		}
	}

	//main unit test routine
	public static void main(String[] args){
		BalancedWinnow mywinnow=new BalancedWinnow();

		//making examples
		ClassLabel c=ClassLabel.positiveLabel(1);
		MutableInstance instance=new MutableInstance();
		instance.addNumeric(new Feature("f2"),2);
		instance.addNumeric(new Feature("f3"),3);
		instance.addNumeric(new Feature("f4"),4);
		Example ex=new Example(instance,c);
		mywinnow.addExample(ex);

		Classifier hp=mywinnow.getClassifier();
		System.out.println("BWinnow Hyperplane = "+hp.toString());

		ClassLabel c1=ClassLabel.negativeLabel(-1);
		MutableInstance instance1=new MutableInstance();
		instance1.addNumeric(new Feature("f3"),1);
		instance1.addNumeric(new Feature("f4"),2);
		instance1.addNumeric(new Feature("f5"),3);
		Example ex1=new Example(instance1,c1);
		mywinnow.addExample(ex1);

		hp=mywinnow.getClassifier();
		System.out.println("BalancedWinnow Hyperplane = "+hp.toString());

		ClassLabel c2=ClassLabel.positiveLabel(1);
		//ClassLabel c2 = ClassLabel.negativeLabel(-1);
		MutableInstance instance2=new MutableInstance();
		instance2.addNumeric(new Feature("f3"),-5);
		instance2.addNumeric(new Feature("f4"),-12);
		instance2.addNumeric(new Feature("f5"),-34);
		Example ex2=new Example(instance2,c2);
		mywinnow.addExample(ex2);

		hp=mywinnow.getClassifier();
		System.out.println("BalancedWinnow Hyperplane = "+hp.toString());

		ClassLabel c3=ClassLabel.positiveLabel(1);
		//ClassLabel c2 = ClassLabel.negativeLabel(-1);
		MutableInstance instance3=new MutableInstance();
		instance3.addNumeric(new Feature("f3"),-5);
		instance3.addNumeric(new Feature("f4"),-12);
		instance3.addNumeric(new Feature("f5"),-34);
		instance.addNumeric(new Feature("f2"),-2);
		Example ex3=new Example(instance3,c3);
		mywinnow.addExample(ex3);

		hp=mywinnow.getClassifier();
		System.out.println("BWinnow Hyperplane = "+hp.toString());

	}
}
