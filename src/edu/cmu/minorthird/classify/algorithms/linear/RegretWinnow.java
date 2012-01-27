package edu.cmu.minorthird.classify.algorithms.linear;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
 * Notation and implementation details based on "Mistake-Driven
 * Learning in Text Categorization", I. Dagan, Y. Karov, D. Roth, EMNLP, 1997
 *  
 * Additionally, it implements 2 optional features: 
 * 	(a) update when examples don't satisfy a margin requirement (margin parameter)
 *  (b) optionally, classify with a voting scheme. (voted parameter)
 *  (c) use of some types of regret minimization updates, based on "From External to Internal Regret, 
 *  Avrim Blum and Yishay Mansour, COLT 2005.
 * 
 */

public class RegretWinnow extends OnlineBinaryClassifierLearner implements
		Serializable{

	static final long serialVersionUID=20080130L;

	private Hyperplane pos_t,numGivenPos,numGivenNeg;//positive hyperplane and feature count hyperplanes

	private Hyperplane vpos_t;//voted hyperplane

	private double theta=1.0; //threshold parameter (positive value)

	private double alpha;//promotion parameter (positive value, bigger than 1)

	private double beta; //demotion parameter (positive value, between 0 and 1)

	private int excount;//number of examples presented to the learner so far

	private double margin=0.0;

	private boolean voted,regret;//voted mode and/or regret mode

	private Hyperplane lossH,lossF; //regret loss accumulated

	private double W_MAX=Math.pow(2,200),W_MIN=1/Math.pow(2,200);//over-underflow ceiling

	double beta2=0.95;//regret beta

	private int votedCount=0;//number of hyperplanes to vote

	private int mode;//regret mode (0 means no regret updates. 1,2,3,4,5: different feature losses)

	private final int LIST_SIZE=5;//history for regret mode 4

	private Map<Feature,List<ClassLabel>> fmap;//hash for regret mode 4

	public RegretWinnow(){
		this(1.5,0.5,false,1);
	}

	public RegretWinnow(double a,double b,boolean voted,int mode){
		if((a<1)||(b<0)||(b>1)){
			System.out.println("Error in BalancedWinnow initial parameters");
			System.out
					.println("Possible problem: (theta<0)||(alpha < 1)||(beta<0)||(beta>1)");
			System.exit(0);
		}
		this.alpha=a;
		this.beta=b;
		this.voted=voted;//improves performance and convergence
		if(mode==0){
			this.regret=false;
		}else{
			this.regret=true;
			this.mode=mode;
		}
		reset();
	}

	@Override
	public void reset(){
		pos_t=new Hyperplane();
		excount=0;
		votedCount=0;
		if(voted){
			vpos_t=new Hyperplane();
		}
		if(regret){
			lossH=new Hyperplane();
			lossF=new Hyperplane();
			if(mode==4){
				fmap=new HashMap<Feature,List<ClassLabel>>();
			}
			if(mode==2){
				numGivenPos=new Hyperplane();
				numGivenNeg=new Hyperplane();
			}
		}
	}

	@Override
	public void addExample(Example example2){

		excount++;
		Example example=Winnow.normalizeWeights(example2,true);

		//bug
		for(Iterator<Feature> j=example.asInstance().featureIterator();j.hasNext();){
			Feature f=j.next();
			if(mode==2){
				if(example.getLabel().isPositive()){
					numGivenPos.increment(f,1.0);
				}else{
					numGivenNeg.increment(f,1.0);
				}
			}
			if(!pos_t.hasFeature(f)){
				pos_t.increment(f,1.0);//initialize weights to 1

				if((mode==4)&&(!fmap.containsKey(f))){
					List<ClassLabel> mylist=new ArrayList<ClassLabel>(LIST_SIZE+1);
					fmap.put(f,mylist);
				}
			}
			if(mode==4){
				List<ClassLabel> ll=fmap.get(f);
				ll.add(0,example.getLabel());
				if(ll.size()>LIST_SIZE+1){
					ll.remove(LIST_SIZE+1);
				}
			}
		}

		//get label and prediction
		double y_t=example.getLabel().numericLabel();
		double y_t_hat=pos_t.score(example.asInstance())-theta;
//		 
		//winnow update rule
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
					if(pos_t.featureScore(f)<W_MAX)
						pos_t.multiply(f,alpha);
				}
			}else{
				for(Iterator<Feature> j=example.featureIterator();j.hasNext();){
					Feature f=j.next();
					if(pos_t.featureScore(f)>W_MIN)
						pos_t.multiply(f,beta);
				}
			}
		}else{//no error occurred
			if(voted){
				votedCount++;
			}
		}

//		regret updates
		if(regret){
			for(Iterator<Feature> j=example.featureIterator();j.hasNext();){
				double localF=0,localH=0;
				Feature ff=j.next();
				if(y_t*y_t_hat>margin){
					//		    	lossH.increment(ff,0.0);
				}else{
					lossH.increment(ff,1.0);
					localH++;
				}

				//weight of the feature predicts the true label correctly
				//criterion 1			    
				if(mode==1){
					if(y_t*(example.getWeight(ff))>0){
						//		    	lossF.increment(ff,0);
					}else{
						lossF.increment(ff,1);
						localF++;
					}
				}

				//criterion 2
				if(mode==2){
					double posFactor=numGivenPos.featureScore(ff);
					double negFactor=numGivenNeg.featureScore(ff);
					double total=posFactor+negFactor;
					if(example.getLabel().isPositive()){
						double coef=1.0-(posFactor/total);
//				    	if(posFactor < 3) coef = 1.0;
						lossF.increment(ff,coef);
					}else{
						double coef=1.0-(negFactor/total);
//				    	if(negFactor<3) coef = 1.0;
						lossF.increment(ff,coef);
					}
				}

				//criterion 3
				if(mode==3){
					int exampleSize=example2.numFeatures();
					if((example.getLabel().isNegative())&&
							(example.getWeight(ff)*pos_t.featureScore(ff)*exampleSize>1.0)){
						lossF.increment(ff,1.0);
						localF++;
					}else if((example.getLabel().isPositive())&&
							(example.getWeight(ff)*pos_t.featureScore(ff)*exampleSize<1.0)){
						lossF.increment(ff,1.0);
						localF++;
					}
				}

				if(mode==4){
					List<ClassLabel> lu=fmap.get(ff);
					int deci=getHistory(lu);
					if(y_t*deci>=0){
//			    		
					}else{
						lossF.increment(ff,1.0);
						localF++;
					}
				}

//			  criterion 5
				if(mode==5){
					lossF.increment(ff,Math.random());
				}

				//update weights
//			    System.out.println(ff.toString()+" "+lossF.featureScore(ff)+ " "+lossH.featureScore(ff));
//			    double deltaLoss = localF-(beta2*localH);
				double deltaLoss=lossF.featureScore(ff)-(beta2*lossH.featureScore(ff));
				double factor=Math.pow(beta2,deltaLoss);

				if((factor>1.0)&&(factor<W_MAX)){
					if(pos_t.featureScore(ff)<W_MAX)
						pos_t.multiply(ff,factor);
				}else if((factor<1.0)&&(factor>W_MIN)){
					if(pos_t.featureScore(ff)>W_MIN)
						pos_t.multiply(ff,factor);
				}
			}//end of feature iterator
		}//end of if(regret)		
	}//end of addExample() method

	public void updateVotedHyperplane(int count){
		vpos_t.increment(pos_t,count);
		votedCount=0;
	}

	@Override
	public Classifier getClassifier(){

		if(voted){
			updateVotedHyperplane(votedCount);//first, update it			
			Hyperplane zpos=new Hyperplane();
			zpos.increment(vpos_t,1/(double)excount);
			return new MyClassifier(zpos,theta);
		}else{
			return new MyClassifier(pos_t,theta);
		}
	}

	//returns positive number if majority of feature history was positive, and negative otherwise
	public int getHistory(List<ClassLabel> ll){
		int tmp=0;
		for(int i=1;i<ll.size();i++){
			if((ll.get(i)).isPositive())
				tmp++;
			else
				tmp--;
		}
		return (tmp==0)?+1:tmp;
	}

	@Override
	public String toString(){
		return "RegretWinnow: voted="+voted+", regret="+mode;
	}

	public class MyClassifier implements Classifier,Serializable,Visible{

		static private final long serialVersionUID=20080130L;

//		private final int CURRENT_SERIAL_VERSION=1;

		private Hyperplane lpos_h;
		//private Hyperplane lneg_h;

		//private ExampleSchema schema;

		private double mytheta;//theta parameter from Winnow

		public MyClassifier(Hyperplane pos_h,double mytheta){
			this.lpos_h=pos_h;
			this.mytheta=mytheta;
		}

		//implements winnow decision rule
		@Override
		public ClassLabel classification(Instance instance1){
			//winnow decision rule
			Example a1=new Example(instance1,new ClassLabel("POS"));//dummy label
			Example aa=filterFeat(a1);
			Example example1=Winnow.normalizeWeights(aa,true);
			Instance instance=example1.asInstance();
			double dec=lpos_h.score(instance)-mytheta;
			return dec>=0?ClassLabel.positiveLabel(dec):ClassLabel.negativeLabel(dec);
		}

//		only consider features in the hyperplane - disregard others
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
			return "POS = "+lpos_h.toString();
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
				static final long serialVersionUID=20080130L;
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
