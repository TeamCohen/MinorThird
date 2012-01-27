/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.ranking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * A boosted version for ranking.
 * An implementation of - "Michael Collins and Terry Koo, Discriminative Reranking for Natural Language Parsing.
 *                                          Computational Linguistics, March 2005", see page 45.
 *
 * Requirements of this class:
 *  - Considers only binary features
 *  - Requires a particular cont. feature named "walkerScore", that contains the original log-probability for an example
 *
 * To do:
 *  - Handle multiple positive answers.
 *  - Automatically discretize real-value features into binary.
 *  - It is possible to incorporate example 'importance' weights, according to some 'goodness' evaluation measure. See Collins'.
 *
 * @author Einat Minkov
 */


public class RankingBoosted extends BatchRankingLearner
{
	private int numEpochs;
	private int exampleSize=20;        // All examples are trimmed to have the same ranked list size.
	private Map<Feature,Set<Index>> A_pos = new HashMap<Feature,Set<Index>>();
	private Map<Feature,Set<Index>> A_neg = new HashMap<Feature,Set<Index>>();
	private Set<Feature> features = new HashSet<Feature>();
	private double SMOOTH_PARAM = 0.005;
	private double[][] margins;
	private Feature score = new Feature("walkerScore");

	// note: the initial score/prob. is turned into log(score).

	public RankingBoosted()
	{
		this(500,20);
	}

	public RankingBoosted(int numEpochs, int exampleSize)
	{
		this.numEpochs=numEpochs;
		this.exampleSize=exampleSize;
	}

	@Override
	public Classifier batchTrain(Dataset data)
	{
//		int numUpdates = 0;

		Map<String,List<Example>> rankingMap = splitIntoRankings(data);

		//Put all ranked lists in a double array, to allow non-sequential access
		Example[][] rankedExamples = new Example[rankingMap.size()][exampleSize];
		int index=0;
		for (Iterator<String> i=rankingMap.keySet().iterator(); i.hasNext(); ) {
			String subpop = i.next();
			List<Example> ranking = orderExamplesList(rankingMap.get(subpop));
			for (int j=0; j<exampleSize;j++)
				rankedExamples[index][j]=ranking.get(j);
			index++;
		}
		Hyperplane s = populate_A(rankedExamples,new Hyperplane());
		s.increment(score,best_w0(rankedExamples));
		margins = initializeMargins(rankedExamples,s);

		ProgressCounter pc = new ProgressCounter("boosted perceptron training", "epoch", numEpochs);
		for (int e=0; e<numEpochs; e++) {
			//System.out.println("epoch "+e+"/"+numEpochs);
			s = batchTrain(s);
			pc.progress();
		}
		pc.finished();
		new ViewerFrame("hyperplane", s.toGUI());
		return s;
	}

	// Map example indexes into A_Pos, A_Neg sets per feature, where
	// A_Neg: Feature that is included in example i, but no in the correct answer example.
	// A_Pos: oppositve same.
	private Hyperplane populate_A(Example[][] rankedExamples, Hyperplane s){
		for (int i=0; i<rankedExamples.length; i++){
			Example correctEx = rankedExamples[i][0];
			Set<Feature> correctFtrs = new HashSet<Feature>();
			for (Iterator<Feature> it=correctEx.binaryFeatureIterator(); it.hasNext();)
				correctFtrs.add(it.next());
			for (int j=1; j<exampleSize; j++){
				Example ex=rankedExamples[i][j];
				Set<Feature> actualFtrs = new HashSet<Feature>();
				for (Iterator<Feature> it=ex.binaryFeatureIterator(); it.hasNext(); ){
					Feature ftr = it.next();
					if (!correctFtrs.contains(ftr)) update_A(A_neg,ftr,i,j);
					actualFtrs.add(ftr);
					features.add(ftr);
				}
				for (Iterator<Feature> it=correctEx.binaryFeatureIterator(); it.hasNext(); ){
					Feature ftr = it.next();
					if (!actualFtrs.contains(ftr)) update_A(A_pos,ftr,i,j);
					features.add(ftr);
				}
			}
		}
		s.multiply(0);
		return s;
	}

	private Map<Feature,Set<Index>> update_A(Map<Feature,Set<Index>> map,Feature ftr,int i,int j){
		Set<Index> set = new HashSet<Index>();
		if (map.containsKey(ftr)) set = map.get(ftr);
		set.add(new Index(i,j));
		map.put(ftr,set);
		return map;
	}


	//Choose weight that minimizes the exp-loss of initial assigned probabilities, using brute-force search
	// (this weight - named here as w0 - is not modified later.)
	private double best_w0(Example[][] rankedExamples){
		double w0 = 0.001;
		double minExpLoss = 100000000;
		for (double w=0.001; w<10; w=w+0.001){
			double expLoss = initialExpLoss(w,rankedExamples);
			if (expLoss<minExpLoss){
				w0=w;
				minExpLoss = expLoss;
			}
		}
		return w0;
	}

	public double initialExpLoss(double w0, Example[][] rankedExamples)
	{
		double expLoss = 0;
		for (int i=0; i<rankedExamples.length; i++) {
			for (int j=0; j<exampleSize; j++) {
				if (rankedExamples[i][j].getLabel().toString().endsWith("NEG 1.0]"))
					expLoss += Math.exp(-w0*(Math.log(rankedExamples[i][0].getWeight(score))-Math.log(rankedExamples[i][j].getWeight(score))));
			}
		}
		return expLoss;
	}


	private double expLoss(double[][] margins){
		double expLoss = 0;
		for (int i=0; i<margins.length; i++)
			for (int j=0; j<exampleSize; j++)
				expLoss += Math.exp(-1*margins[i][j]);
		return expLoss;
	}


	private double[][] initializeMargins(Example[][] rankedExamples, Hyperplane s){
		double[][] margins = new double[rankedExamples.length][exampleSize];
		for (int i=0; i<margins.length;i++){
			for (int j=0; j<exampleSize;j++){
				margins[i][j] = s.featureScore(score)
				*(Math.log(rankedExamples[i][0].getWeight(score)) - Math.log(rankedExamples[i][j].getWeight(score)));
				//*((rankedExamples[i][0].getWeight(score)) - (rankedExamples[i][j].getWeight(score)));
				System.out.println("margins: "+ i + " " + j + " " + margins[i][j]);
			}
		}
		return margins;
	}

	// return the number of times h has been updated
	private Hyperplane batchTrain(Hyperplane s)
	{
//		int updates = 0;
		Feature bestFeature = null ;
		double maxGain =0;
		double W_Pos=0, W_Neg=0;
		for (Iterator<Feature> it=features.iterator();it.hasNext();){
			Feature ftr = it.next();
			double cur_W_Pos=0, cur_W_Neg=0;
			if (A_pos.containsKey(ftr)){
				for (Iterator<Index> itIndex=A_pos.get(ftr).iterator();itIndex.hasNext();){
					Index index = itIndex.next();
					cur_W_Pos += Math.exp(-1*margins[index.i][index.j]);
				}
			}
			if (A_neg.containsKey(ftr)){
				for (Iterator<Index> itIndex=A_neg.get(ftr).iterator();itIndex.hasNext();){
					Index index = itIndex.next();
					cur_W_Neg += Math.exp(-1*margins[index.i][index.j]);
				}
			}
			double gain = Math.abs(Math.sqrt(cur_W_Pos)-Math.sqrt(cur_W_Neg));
			if (gain>maxGain){
				maxGain = gain;
				bestFeature = ftr;
				W_Pos = cur_W_Pos; W_Neg = cur_W_Neg;
			}
		}
		if (bestFeature!=null){
			double Z = expLoss(margins);
			double delta = 0.5*Math.log((W_Pos+SMOOTH_PARAM*Z)/(W_Neg+SMOOTH_PARAM*Z));
			/**
        System.out.println("best feature: " + bestFeature + " " + delta);
        System.out.println("W_Pos: " + W_Pos);
        System.out.println("W_Neg: " + W_Neg);
        System.out.println("Z: " + Z);
			 **/
			updateMargins(bestFeature,delta);
			s.increment(bestFeature,delta);
		}
		return s;
	}


	//update margins, for examples that are in A_Pos and A_Neg per the selected feature.
	private void updateMargins(Feature feature, double delta){
		Set<Index> pos = A_pos.get(feature);
		Set<Index> neg = A_neg.get(feature);
		if (pos != null){
			for (Iterator<Index> it = pos.iterator(); it.hasNext();){
				Index ij = it.next();
				margins[ij.i][ij.j] += delta;
			}
		}
		if (neg != null){
			for (Iterator<Index> it = neg.iterator(); it.hasNext();){
				Index ij = it.next();
				margins[ij.i][ij.j] -= delta;
			}
		}
	}


	private List<Example> orderExamplesList(List<Example> ranking){
		Set<Example> correct = new HashSet<Example>();
		Set<Example> incorrect = new HashSet<Example>();
		for (int i=0; i<ranking.size(); i++){
			Example ex = ranking.get(i);
			if (ex.getLabel().toString().endsWith("POS 1.0]")) correct.add(ex);
			else incorrect.add(ex);
		}
		List<Example> ordered = new LinkedList<Example>();
		for (Iterator<Example> it=correct.iterator();it.hasNext();) ordered.add(it.next());
		for (Iterator<Example> it=incorrect.iterator();it.hasNext();) ordered.add(it.next());
		return ordered;
	}


	private class Index {
		int i;
		int j;
		public Index(int i, int j){
			this.i=i;
			this.j=j;
		}
	}

}
