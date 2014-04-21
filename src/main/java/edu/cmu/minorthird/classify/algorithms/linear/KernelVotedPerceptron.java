package edu.cmu.minorthird.classify.algorithms.linear;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.OnlineBinaryClassifierLearner;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Voted perceptron algorithm.  As described in "Large Margin
 * Classification Using the Perceptron Algorithm", Yoav Freund and
 * Robert E. Schapire, Proceedings of the Eleventh Annual Conference
 * on Computational Learning Theory,
 * 1998. 
 * 
 * Polynomial kernel is implemented: K(x,y) = (coef0+gamma*<x,y>)^d 
 * Both "voted" and "averaged" modes are implemented (unnormalized). Poly degree = 0 
 * means that no kernel is used.
 * 
 * Therefore, mode "averaged" with degree=0 should be equivalent to results in 
 * VotedPerceptron.java (file with a faster implementation of the averaged nonnormalized perceptron)
 *
 * @author Vitor Carvalho
 */

public class KernelVotedPerceptron extends OnlineBinaryClassifierLearner
		implements Serializable{

	static final long serialVersionUID=20080128L;//serialization stuff

	private static Logger log=Logger.getLogger(KernelVotedPerceptron.class);

	private Hyperplane v_k; //current hypothesis

	private int c_k;//mistake counter

	private List<Hyperplane> listVK; //list with v_k,
	private List<Integer> listCK; //and list with c_k

	private String mode="voted";// "voted"(default) or "averaged"

	//poly kernel
	private int degree=3; //degree of poly kernel; default is 3

	private double gamma=10,coef0=1;//K(x,y) = (coef0+gamma*<x,y>)^d 

	//speeds up inference by using only last MAXVEC kernels. Approximate results.
	private boolean speedup=false;//false;//

	private int MAXVEC=300;//maximum of 1000 support vectors, for speed up

	/**
	 * Constructor: specifies degree of poly kernel and mode
	 * Example KernelVotedPerceptron(3,"averaged") or (5,"voted")
	 * @param degree
	 * @param mode
	 */
	public KernelVotedPerceptron(int degree,String mode){
		reset();
		this.degree=degree;
		this.mode=mode;
	}

	/**
	 * Default Constructor: degree=3 and mode="voted"
	 */
	public KernelVotedPerceptron(){
		reset();
	}

	/**
	 * set degree of poly kernel  K(x,y) = (coef0+ gamma*<x,y>)^d
	 * if set to 0, usual <x,v> crossproduct is used.
	 * @param d
	 */
	public void setKernel(int d){
		degree=d;
	}

	/**
	 * set params of poly kernel K(x,y) = (coef0+ gamma*<x,y>)^d
	 * @param coef0
	 * @param gamma
	 */
	public void setPolyKernelParams(double coef0,double gamma){
		this.coef0=coef0;
		this.gamma=gamma;
	}

	@Override
	public void reset(){
		v_k=new Hyperplane();
		listVK=new ArrayList<Hyperplane>();
		listCK=new ArrayList<Integer>();
		c_k=0;
	}

	//set mode: voted or averaged
	public void setModeVoted(){
		mode="voted";
	}

	public void setModeAveraged(){
		mode="averaged";
	}

	/**
	 * Set speed-up: use only last 300 support vectors in testing
	 */
	public void setSpeedUp(){
		speedup=true;
	}

	//store support vectors and their counts (number of mistakes)
	private void store(Hyperplane h,int count){
		Hyperplane hh=new Hyperplane();
		hh.increment(h);
		listVK.add(hh);
		listCK.add(new Integer(count));
	}

	//update rule for training: Figure 1 in Freund & Schapire paper
	@Override
	public void addExample(Example example){
		double y_t=example.getLabel().numericLabel();
		if(Kernel(v_k,example.asInstance())*y_t<=0){//prediction error occurred
			store(v_k,c_k);
			v_k.increment(example,y_t);
			c_k=1;
		}else{
			c_k++;
		}
	}

//	poly kernel function
	double Kernel(Hyperplane h,Instance ins){
		double score=h.score(ins);
		if(degree==0)
			return score; //no kernels
		else
			return Math.pow(coef0+(score*gamma),degree);
	}

	//TESTING ------------------------------------------------------------

	@Override
	public Classifier getClassifier(){
		return new MyClassifier(listVK,listCK);
	}

	public class MyClassifier implements Classifier,Serializable,Visible{

		static private final long serialVersionUID=1;

		List<Hyperplane> listVK;
		List<Integer> counts;

		public MyClassifier(List<Hyperplane> li,List<Integer> cc){
			this.listVK=li;
			this.counts=cc;
			log.info("info: KernelVotedPerceptron: number sup vectors = "+
					listVK.size()+" mode="+mode+" kernel="+degree);
		}

		//implements decision rule
		@Override
		public ClassLabel classification(Instance ins){
			double dec=0;
			if(mode.equalsIgnoreCase("voted")){
				dec=calculateVoted(ins);
			}else if(mode.equalsIgnoreCase("averaged")){
				dec=calculateAveraged(ins);
			}else{
				System.out.println("Mode("+mode+
						") is not allowed\n Please use either \"voted\" or \"averaged\"");
				System.exit(0);
			}
			return dec>=0?ClassLabel.positiveLabel(dec):ClassLabel.negativeLabel(dec);
		}

		//voted mode
		private double calculateVoted(Instance ins){
			double score=0;
			int FIRSTVEC=0;
			if(speedup){
				int MAX=Math.min(MAXVEC,listVK.size());
				FIRSTVEC=listVK.size()-MAX;
			}
			for(int i=FIRSTVEC;i<listVK.size();i++){
				Hyperplane v_k=listVK.get(i);//v_k
				int countt=(counts.get(i)).intValue();//c_k counts	
				double kernelScore=Kernel(v_k,(ins));
				double sign=(kernelScore>0)?+1:-1;//voting
				score+=countt*(sign);
			}
			return score;
		}

		//average unnormalized mode
		private double calculateAveraged(Instance ins){
			double score=0;
			int FIRSTVEC=0;
			if(speedup){
				int MAX=Math.min(MAXVEC,listVK.size());
				FIRSTVEC=listVK.size()-MAX;
			}
			for(int i=FIRSTVEC;i<listVK.size();i++){
				Hyperplane hp=listVK.get(i);
				int countt=(counts.get(i)).intValue();
				score+=countt*Kernel(hp,ins);
//					System.out.println(score);
			}
//				System.out.println("----------------------------");
			return score;
		}

		@Override
		public String explain(Instance instance){
			return "KernelVotedPerceptron: Not implemented yet";
		}

		@Override
		public Explanation getExplanation(Instance instance){
			Explanation.Node top=
					new Explanation.Node("Kernel Perceptron Explanation (not valid!)");
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

					//dummy hyperplane: return last perceptron
					Hyperplane hh=mycl.listVK.get(listVK.size()-1);
					return hh;
				}
			};
			v.setContent(this);
			return v;
		}
	}

	@Override
	public String toString(){
		return "Kernel Voted Perceptron";
	}
}
