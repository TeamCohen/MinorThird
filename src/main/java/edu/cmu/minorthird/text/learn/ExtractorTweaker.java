/* Copyright 2004, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.learn;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import com.lgc.wsh.inv.ScalarSolver;

import edu.cmu.minorthird.classify.sequential.CMM;
import edu.cmu.minorthird.classify.sequential.CMMTweaker;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;
import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.SpanDifference;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.util.BasicCommandLineProcessor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.StringUtil;

/**
 * Allows one to adjust the parameters of a learned extractor.
 * 
 * 
 * @author William Cohen
 */

public class ExtractorTweaker{

	private CMMTweaker cmmTweaker=new CMMTweaker();

	//
	// the getNewBias, getOldBias are here if anyone wants to use the
	// tweak(annotator,biasValue) interface as part of their own
	// optimization routine. Notice there's no easy way to find out
	// what the existing bias is.
	//

	/**
	 * Return the value of bias term before the last tweak.
	 */
	public double getOldBias(){
		return cmmTweaker.oldBias();
	}

	/**
	 * Return the value of bias term after the last tweak.
	 */
	public double getNewBias(){
		return cmmTweaker.newBias();
	}

	/**
	 * Return a modified copy of the annotator. Only works for annotators learned
	 * by the voted perceptron and/or CRF learners.
	 */
	public ExtractorAnnotator tweak(ExtractorAnnotator annotator,double bias){
		if(annotator instanceof SequenceAnnotatorLearner.SequenceAnnotator){
			SequenceAnnotatorLearner.SequenceAnnotator sa=
					(SequenceAnnotatorLearner.SequenceAnnotator)annotator;
			SequenceClassifier sc=sa.getSequenceClassifier();
			if((sc instanceof CMM)){
				CMM cmm=(CMM)sc;
				return new SequenceAnnotatorLearner.SequenceAnnotator(cmmTweaker.tweak(
						cmm,bias),sa.getSpanFeatureExtractor(),sa.getReduction(),sa
						.getSpanType());
			}else{
				throw new IllegalArgumentException(
						"can't tweak annotator based on sequence classifier of type "+
								sc.getClass());
			}
		}else{
			throw new IllegalArgumentException("can't tweak annotator of type "+
					annotator.getClass());
		}
	}

	//
	// command-line processing
	//
	private File fromFile=null;

	private File toFile=null;

	private TextLabels textLabels=null;

	private String spanType=null;

	private double newBias=0,lo=-999,hi=999;

	private double beta=1.0;

	private boolean biasSpecified=false,loSpecified=false,hiSpecified=false;

	public class MyCLP extends BasicCommandLineProcessor{

		public void loadFrom(String s){
			fromFile=new File(s);
		}

		public void saveAs(String s){
			toFile=new File(s);
		}

		public void labels(String s){
			textLabels=FancyLoader.loadTextLabels(s);
		}

		public void spanType(String s){
			spanType=s;
		}

		public void newBias(String s){
			newBias=StringUtil.atof(s);
			biasSpecified=true;
		}

		public void loBias(String s){
			lo=StringUtil.atof(s);
			loSpecified=true;
		}

		public void hiBias(String s){
			hi=StringUtil.atof(s);
			hiSpecified=true;
		}

		public void beta(String s){
			beta=StringUtil.atof(s);
		}

		@Override
		public void usage(){
			for(int i=0;i<USAGE.length;i++)
				System.out.println(USAGE[i]);
		}
	}

	public CommandLineProcessor getCLP(){
		return new MyCLP();
	}

	static private final String[] USAGE=
			{
					"ExtractorTweaker: modify the recall/precision of a previously-learned extractor",
					"",
					"Parameters:",
					" -loadFrom FILE                where to load a previously-learner extractor from",
					" [-saveAs FILE]                where to save the 'tweaked' version of the extractor",
					" [-newBias NUM]                new value that replaces the hyperplane_bias term of the NEG hyperplane",
					" [-loBias NUM]                 lower limit of search for best bias term",
					" [-hiBias NUM]                 lower limit of search for best bias term",
					"",
					"If -newBias is NOT specified, then ExtractorTweaker will try and find a 'good' value",
					"on its own, using bisection search, guided by the following additional parameters:",
					"  -labels KEY -spanType TYPE [-beta BETA]",
					"where -labels KEY -spanType TYPE specifies the dataset to use in opimizing the extractor",
					"and -beta BETA determines the function to optimize, namely token-level F_beta (default, beta=1)",
					"It seems to work ok to optimize performance on the dataset used for training.",
					"",};

	private void doMain(){
		if(fromFile==null)
			throw new IllegalStateException("need to specify -loadFrom");

		ExtractorAnnotator annotator=null;
		try{
			System.out.println("loading from: "+fromFile);
			annotator=(ExtractorAnnotator)IOUtil.loadSerialized(fromFile);
		}catch(IOException ex){
			System.out.println("can't load "+fromFile+": "+ex);
		}
		ExtractorAnnotator tweaked=null;
		if(biasSpecified){
			// just tweak the bias as given
			tweaked=tweak(annotator,newBias);
		}else if(!biasSpecified&&textLabels!=null&&spanType!=null){
			// try and optimize f1 on the provided set of text labels

			// figure out initial bounds
			if(!loSpecified||!hiSpecified){
				tweak(annotator,0);
				double v=getOldBias();
				if(v<0)
					v=-v;
				if(v==0)
					v=0.1;
				if(!loSpecified)
					lo=-10*v;
				if(!hiSpecified)
					hi=10*v;
				System.out.println("oldBias term was "+v+" testing between "+lo+
						" and "+hi);
			}

			System.out.println("try to maximize token F[beta] for beta="+beta+
					" (b>1 rewards recall, b<1 precision)");
			AnnTester annTester=new AnnTester(annotator,beta);
			ScalarSolver solver=new ScalarSolver(annTester);

			double optBias=solver.solve(lo,hi,0.01,0.01,40,null);
			tweaked=tweak(annotator,optBias);
		}else{
			System.out.println("illegal usage, use -help for help");
		}
		try{
			if(toFile!=null)
				IOUtil.saveSerialized((Serializable)tweaked,toFile);
		}catch(IOException ex){
			System.out.println("can't save to "+toFile+": "+toFile);
		}
	}

	//
	// a function to optimize - returns token-level F_beta
	//

	private class AnnTester implements ScalarSolver.Function{

		private ExtractorAnnotator ann;

		private double beta=1.0;

		public AnnTester(ExtractorAnnotator annotator,double beta){
			this.ann=annotator;
			this.beta=beta;
		}

		@Override
		public double function(double d){
			ExtractorAnnotator tweakedAnn=tweak(ann,d);
			TextLabels annLabels=tweakedAnn.annotatedCopy(textLabels);
			SpanDifference sd=
					new SpanDifference(annLabels.instanceIterator(ann.getSpanType()),
							annLabels.instanceIterator(spanType),annLabels
									.closureIterator(spanType));
			double f=0;
			double p=sd.tokenPrecision();
			double r=sd.tokenRecall();
			if(p!=0||p!=0){
				f=(beta*beta+1.0)*p*r/(beta*beta*p+r);
			}
			System.out.println("after testing bias "+d+" yields f["+beta+"]="+f+
					" for p/r of "+p+"/"+r);
			return -f; // scalar solver tries to minimize this
		}
	}

	/** 
	 */
	public static void main(String[] args){
		try{
			ExtractorTweaker xt=new ExtractorTweaker();
			xt.getCLP().processArguments(args);
			xt.doMain();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
