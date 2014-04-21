package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.OnlineBinaryClassifierLearner;

import java.io.*;

/**
 * Voted perceptron algorithm.  As described in "Large Margin
 * Classification Using the Perceptron Algorithm", Yoav Freund and
 * Robert E. Schapire, Proceedings of the Eleventh Annual Conference
 * on Computational Learning Theory,
 * 1998. 
 *
 * @author William Cohen
 */

/*
 Following the notation of F & S, the voted perceptron will maintain
 weight vectors S_t and W_t as follows:

 W_t = d_t x_t + W_{t-1}
 S_t = W_t + S_{t-1}

 where d_t = (prediction error on x_t) ? y_t : 0

 the prediction score of averaged perceptron on x is inner product <S_t,x> 
 the prediction score of perceptron on x is inner product <W_t,x> 

 For kernels, we would compute for each x after training on x1,..,x_T

 for t = 1...T
 KW_t(x) = KW_{t-1}(x) + d_t K(x_t,x)
 KS_t(x) = KS_{t-1}(x) + KW_t(x)

 But that's not implemented here.

 April 2007: see KernelVotedPerceptron.java for implementation of this algorithm with kernels.
 */

public class VotedPerceptron extends OnlineBinaryClassifierLearner implements
		Serializable{

	static final long serialVersionUID=20080130L;
	
	private Hyperplane s_t,w_t;

	private boolean ignoreWeights=false;

	//private long mistakeCount;

	public VotedPerceptron(){
		this(false);
	}

	/** If ignoreWeights is true, treat all weights as binary. For
	 * backward compatibility with an older buggy version.
	 */
	public VotedPerceptron(boolean ignoreWeights){
		this.ignoreWeights=ignoreWeights;
		reset();
	}

	@Override
	public void reset(){
		s_t=new Hyperplane();
		w_t=new Hyperplane();
		if(ignoreWeights){
			s_t.startIgnoringWeights();
			w_t.startIgnoringWeights();
		}
		//mistakeCount=0;
	}

	@Override
	public void addExample(Example example){
		double y_t=example.getLabel().numericLabel();
		if(w_t.score(example.asInstance())*y_t<=0){
			w_t.increment(example,y_t);
		}
		s_t.increment(w_t,1.0);
	}

	@Override
	public Classifier getClassifier(){
		return s_t;
	}

	//-------------------------------------------------------
	//Faster implementation. Not tested yet.
	//
//	public void addExample(Example example)
//	{
//		double y_t = example.getLabel().numericLabel();
//		if (w_t.score(example.asInstance()) * y_t <= 0) {			
//	        updateVotedHyperplane(mistakeCount);
//			mistakeCount =1;			
//			w_t.increment( example, y_t );
//		}
//		else{
//			mistakeCount++;
//		}
//	}	
//	public void updateVotedHyperplane(double count){		
//		s_t.increment(w_t,count);		
//	}	
//	public Classifier getClassifier() 
//	{
//		updateVotedHyperplane(mistakeCount);
//		mistakeCount = 0;
//		return s_t;
//	}
	//---------------------------------------------------------

	@Override
	public String toString(){
		return "VotedPerceptron";
	}
}
