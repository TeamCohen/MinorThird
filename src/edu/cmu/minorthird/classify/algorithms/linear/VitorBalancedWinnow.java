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
 * Implements the modified version of the Balanced Winnow algorithm (MBW), following the 
 * ideas described in "Improving Winnow for NLP Tasks: Voting Schemes,
 * Regret Minimization and Online Feature Selection", Vitor Carvalho, William Cohen 
 * and Avrim Blum, CMU Technical Report 2006
 * 
 * A Modified version of the Balanced Winnow algorithm, as described in "Learning Quickly when Irrelevant
 * Attributes Abound: a new linear-threshold algorithm", N. Littlestone, Machine
 * Learning, 1988. One of the modifications is based on the "thick-Margin trick used in 
 * "Mistake-Driven Learning in Text Categorization", I. Dagan, Y. Karov, D. Roth, EMNLP, 1997
 *  
 * Additionally, it implements the Voting scheme (averaged hypothesis). 
 * Please check the "voted" parameter for that.
 *  
 */

public class VitorBalancedWinnow extends OnlineBinaryClassifierLearner
		implements Serializable{
	
	static final long serialVersionUID=20071130L;

	private Hyperplane pos_t,neg_t;//positive hyperplane

	private Hyperplane vpos_t,vneg_t;//voted hyperplanes

	private double theta=1.0; //threshold parameter (positive value)

	private double alpha;//promotion parameter (positive value, larger than 1)

	private double beta; //demotion parameter (positive value, between 0 and 1)

	private int excount;//example counter: number of examples presented to the learner so far

	private double margin=1.0;

	private boolean voted;//voted mode

	private double W_MAX=Math.pow(2,200),W_MIN=1/Math.pow(2,200);//over-underflow ceiling

	private int votedCount=0;//number of hyperplanes to vote

	/**
	 * Constructor
	 */
	public VitorBalancedWinnow(){
		this(1.5,0.5,false);//non-voted: recommended
//		this(1.5, 0.5, true);//voted: recommended
	}

	/**
	 * Constructor: voted="true" (more stable) or "false"(more aggressive)
	 * @param voted
	 */
	public VitorBalancedWinnow(boolean voted){
		this(1.5,0.5,voted);
	}

	public VitorBalancedWinnow(double alpha,double beta,boolean voted){
		if((alpha<1)||(beta<0)||(beta>1)){
			System.err.println("Error in BalancedWinnow initial parameters");
			throw new IllegalArgumentException(
					"invalid parameter initializing VitorBalancedWinnow. Possible Problema: (theta<0)||(alpha < 1)||(beta<0)||(beta>1)");
		}
		this.alpha=alpha;
		this.beta=beta;
		this.voted=voted;
		reset();
	}

	@Override
	public void reset(){
		pos_t=new Hyperplane();
		neg_t=new Hyperplane();
		excount=0;
		votedCount=0;
		if(voted){
			vpos_t=new Hyperplane();
			vneg_t=new Hyperplane();
		}
	}

	@Override
	public void addExample(Example example2){
		excount++; //example counter
		Example example=Winnow.normalizeWeights(example2,true);

		//add new feautures to hyperplane
		for(Iterator<Feature> j=example.asInstance().featureIterator();j.hasNext();){
			Feature f=j.next();
			if(!pos_t.hasFeature(f)){
				pos_t.increment(f,2.0);//initialize weights to 2
				neg_t.increment(f,1.0);//initialize weights to 1
			}
		}

		//get label and prediction
		double y_t=example.getLabel().numericLabel();
		double y_t_hat=
				pos_t.score(example.asInstance())-neg_t.score(example.asInstance())-
						theta;

		//winnow update rule
		if(y_t*y_t_hat<=margin){//if error occurred

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
						pos_t.multiply(f,(1+example.getWeight(f))*(alpha));
					if(neg_t.featureScore(f)>W_MIN)
						neg_t.multiply(f,(1-example.getWeight(f))*(beta));
				}
			}else{
				for(Iterator<Feature> j=example.featureIterator();j.hasNext();){
					Feature f=j.next();
					if(pos_t.featureScore(f)>W_MIN)
						pos_t.multiply(f,(1-example.getWeight(f))*(beta));
					if(neg_t.featureScore(f)<W_MAX)
						neg_t.multiply(f,(1+example.getWeight(f))*alpha);
				}
			}
		}else{//no error occurred
			if(voted){
				votedCount++;
			}
		}
	}

	//add pos_t*count to the official hyperplane
	public void updateVotedHyperplane(int count){
		vpos_t.increment(pos_t,count);//vpos_t = vpos_t + (pos_t*count)
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

	@Override
	public String toString(){
		return "VitorBalancedWinnow: voted="+voted;
	}

	public class MyClassifier implements Classifier,Serializable,Visible{

		static final long serialVersionUID=20071130L;

		private Hyperplane lpos_h,lneg_h;

		//private ExampleSchema schema;

		private double mytheta;//theta parameter from Winnow

		public MyClassifier(Hyperplane pos_h,Hyperplane neg_h,double atheta){
			this.lpos_h=pos_h;
			this.lneg_h=neg_h;
			this.mytheta=atheta;
		}

		//implements the balanced winnow decision rule for a new incoming instance
		@Override
		public ClassLabel classification(Instance instance1){
			Example a1=new Example(instance1,new ClassLabel("POS"));//dummy label POS
			Example aa=filterFeat(a1); //remove features not in hyperplane lpos_h
			Example example1=Winnow.normalizeWeights(aa,true);
			Instance instance=example1.asInstance();
			//decision rule
			double dec=lpos_h.score(instance)-lneg_h.score(instance)-mytheta;
			return dec>=0?ClassLabel.positiveLabel(dec):ClassLabel.negativeLabel(dec);
		}

//		only consider features if also present in the hyperplane;
		//the features that cannot be found in hyperplane are deleted from the example.
		public Example filterFeat(Example ex){
			MutableInstance ins=new MutableInstance();
			for(Iterator<Feature> i=ex.asInstance().featureIterator();i.hasNext();){
				Feature f=i.next();
				if((lpos_h.hasFeature(f))){
					ins.addNumeric(f,ex.getWeight(f));
				}
			}
			return new Example(ins,ex.getLabel());
		}

		@Override
		public String toString(){
			return "POS = "+lpos_h.toString()+" NEG = "+lneg_h.toString();
		}

		@Override
		public String explain(Instance instance){
			return "VitorBalancedWinnow: Not implemented yet";
		}

		@Override
		public Explanation getExplanation(Instance instance){
			Explanation.Node top=
					new Explanation.Node("VitorBalancedWinnow Explanation");
			Explanation ex=new Explanation(top);
			return ex;
		}

		@Override
		public Viewer toGUI(){
			Viewer v=new TransformedViewer(new SmartVanillaViewer()){
				
				static final long serialVersionUID=20071130L;

				@Override
				public Object transform(Object o){
					MyClassifier mycl=(MyClassifier)o;
					return mycl.lpos_h;//bug!
				}
			};
			v.setContent(this);
			return v;
		}
	}
}
