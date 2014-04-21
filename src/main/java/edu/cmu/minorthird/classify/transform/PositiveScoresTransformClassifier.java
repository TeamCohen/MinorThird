package edu.cmu.minorthird.classify.transform;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetClassifierTeacher;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Expt;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.util.LineProcessingUtil;

/**
 * @author Vitor R. Carvalho
 *
 * Classifiers that, from a linear binary classfier, perform the classification of new instances
 * by disregarding all features whose hyperplane axis are negative.
 *
 * 
 */
public class PositiveScoresTransformClassifier implements Classifier,
		Serializable{

	static private final long serialVersionUID=20080201L;;

	private double threshold; //positive threshold, instead of zero

	private Hyperplane hyp;

	private final double minFeatScore=0;

	//constructor
	public PositiveScoresTransformClassifier(Classifier c,Dataset data){
		hyp=(Hyperplane)c;
		//optional: just for fun, change meanFeatScore - never tested
		//minFeatScore = calculateMinFeatScore(data, 0.5);

		threshold=calculatePositiveThreshold(data);
		System.out.println("Threshold = "+threshold);
	}

	@Override
	public ClassLabel classification(Instance instance){
		double s=score(instance,minFeatScore);
		double th=s-threshold;
		return s>=threshold?ClassLabel.positiveLabel(th):ClassLabel
				.negativeLabel(th);
		//need to change
		//return s>=threshold ? ClassLabel.positiveLabel(s) : ClassLabel.negativeLabel(s);
	}

	/**
	 *  iterate over training data to discover positive threshold.
	 * the threshold is the weighted average between positive and negative mean scores
	 */
	public double calculatePositiveThreshold(Dataset data){
		double posScore=0.0,negScore=0.0;
		int numPos=0,numNeg=0;

		for(Iterator<Example> i=data.iterator();i.hasNext();){
			Example ex=i.next();
			Instance inst=ex.asInstance();
			if(ex.getLabel().isPositive()){
				numPos++;
				posScore+=score(inst,minFeatScore);
			}else{
				numNeg++;
				negScore+=score(inst,minFeatScore);
			}
		}
		//calculate means
		double negTh=(negScore/numNeg);
		double posTh=(posScore/numPos);
		//System.out.println("posTh/negTh = "+posTh+" / "+negTh);
		//double myTh = (negTh+posTh)/2; //simple average
		double myTh=(negTh*numPos+posTh*numNeg)/((numPos+numNeg)*2);//a weighted average
		return myTh;
	}

	/**
	 * Finds the maximum positive axis weigth of hyperplane and 
	 * calculates minFeatScore based on percentage this value
	 *
	 * this was never tested! 
	 * 
	 */
//	private double calculateMinFeatScore(Dataset data,double percent){
//		double lastMax=0.0;
//		if((percent>1)||(percent<0)){
//			System.out.println("ERROR ; percentage should be a valid number[0,1]");
//			return 0;
//		}
//		for(Iterator<Feature> i=hyp.featureIterator();i.hasNext();){
//			Feature f=i.next();
//			double aa=hyp.featureScore(f);
//			if(lastMax<aa)
//				lastMax=aa;
//		}
//		double b=lastMax*percent;
//		System.out.println("lastMAx = "+b);
//		return b;
//
//	}

	public double score(Instance instance){
		return score(instance,0);
	}

	/** Inner product of hyperplane and instance weights, disregarding 
	 * the negative dimensions of hyperplane. */
	public double score(Instance instance,double minFeatScore){
		double score=0.0;
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();

			//only positive dimensions of hyperplane
			if(hyp.featureScore(f)>minFeatScore){
				score+=instance.getWeight(f)*hyp.featureScore(f);
			}
		}
		score+=hyp.featureScore(Hyperplane.BIAS_TERM);
		return score;
	}

	@Override
	public String explain(Instance instance){
		return "classify using only features with positive hyperplane weights";
	}

	@Override
	public Explanation getExplanation(Instance instance){
		Explanation ex=new Explanation(explain(instance));
		return ex;
	}

//	private static void usage(){
//		System.out.println("PositiveScoresTransformClassifier dataset");
//	}

	public static void main(String[] args){
		if((args.length<1)||(args.length>1)){
			System.out.println("Usage: PositiveScoresTransformClassifier classname");
			return;
		}
		String mytag=args[0];
		Dataset dataset=new BasicDataset();
		SpanFeatureExtractor fe=
				edu.cmu.minorthird.text.learn.SampleFE.BAG_OF_LC_WORDS;
		TextLabels labels;
		try{
			labels=
					LineProcessingUtil.readBsh(new File("C:/m3test/total/data/"),
							new File("C:/m3test/total/env/all"+mytag+".env"));
			TextBase tb=labels.getTextBase();
			for(Iterator<Span> it=tb.documentSpanIterator();it.hasNext();){
				Span docspan=it.next();
				//String docid=docspan.getDocumentId();
				MutableInstance ins=(MutableInstance)fe.extractInstance(labels,docspan);
				ClassLabel mylabel=new ClassLabel();
				mylabel=
						labels.hasType(docspan,mytag)?ClassLabel.binaryLabel(+1):ClassLabel
								.binaryLabel(-1);
				dataset.add(new Example(ins,mylabel));
			}

			//only works for linear classifiers 
			ClassifierLearner learner=new VotedPerceptron();
//	     ClassifierLearner learner = new MarginPerceptron();	        	        
//       ClassifierLearner learner = new NaiveBayes();
			Splitter<Example> split=Expt.toSplitter("k2");

			Evaluation v=new Evaluation(dataset.getSchema());
			Dataset.Split s=dataset.split(split);
			for(int k=0;k<s.getNumPartitions();k++){
				Dataset trainData=s.getTrain(k);
				Dataset testData=s.getTest(k);
				System.out.println("splitting with "+split+", preparing to train on "+
						trainData.size()+" and test on "+testData.size());
				Classifier cc=new DatasetClassifierTeacher(trainData).train(learner);

				//	apply transformation
				Classifier cc_transformed=
						new PositiveScoresTransformClassifier(cc,trainData);
				v.extend(cc_transformed,testData,k);

				//or, without the transformation
				//	v.extend( cc, testData, k );
			}

			v.summarize();
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Usage: PositiveScoresTransformClassifier classname");
			System.out.println("for instance, nameclass = Req, Dlv, Cmt, POS, etc");
		}

	}
}
