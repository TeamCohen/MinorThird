package edu.cmu.minorthird.classify.ranking;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.util.ProgressCounter;

/**
 * Implements the Listwise Ranking algorithm proposed at:
 * Learning to Rank: From Pairwise Approach to Listwise Approach, ICML 2007.
 * Zhe Cao, Tao Qin, Tie-Yan Liu, Ming-Feng Tsai, Hang Li. 
 * 
 * Only works for binary relevance levels (i.e., revelant vs non-revevant)
 * 
 * @author Vitor R. Carvalho
 */

public class ListNet extends BatchRankingLearner{

	private int numEpochs,maxNumBadSteps=6;

	private double learnRate;//magic parameter

	private double INITLearnRate=0.05;

	private final double minCEImprovement=0.005;

	private final double RELEVANT=1.0,NON_RELEVANT=-1.0;

//	private double selfEntropy =0;//initial cross entropy;
	private Dataset devData=null;//development set

	private double[] pz,py;//probability distributions

	public ListNet(){
		this(15,0.05);
	}

	public ListNet(int numEpochs){
		this(numEpochs,0.05);
	}

	public ListNet(int epochs,double rate){
		numEpochs=epochs;
		INITLearnRate=rate;
	}

	public void setDevData(Dataset data){
		devData=data;
	}

	@Override
	public Classifier batchTrain(Dataset data){
		double x=0,smallestCE=Double.MAX_VALUE;
		int outcount=0;
		Dataset traindata=(devData==null)?separateDevData(data):data;
		List<Hyperplane> ar=new ArrayList<Hyperplane>();
		Hyperplane w=new Hyperplane();
		ar.add(w);
//		Map queryMap = listsWithOneExampleEach( splitIntoRankings(traindata) );
		Map<String,List<Example>> queryMap=splitIntoRankings(traindata);
		ProgressCounter pc=
				new ProgressCounter("ListNet training","epoch",numEpochs);

		for(int e=0;e<numEpochs;e++){
			setLearnRate();
			learnStep(queryMap,w);
			double cur_ce=calculateLoss(splitIntoRankings(devData),w);
			x=smallestCE-cur_ce;
//			System.out.println(e+"\tCE/prevCE = "+cur_ce+"/"+smallestCE+"\tx="+x+"\t"+learnRate);

			if((e==0)||(x>0)){
//				System.out.println("SELF-entropy = "+selfEntropy);
				Hyperplane tmp=new Hyperplane();
				tmp.increment(w);
				ar.add(tmp);
				smallestCE=cur_ce;
			}

			if(x<minCEImprovement){
				outcount++;
				if(outcount>maxNumBadSteps)
					return ar.get(ar.size()-1);

				//try to find a new hypothesis by changing the learnRate
				int count=0;
				while((x<0)&&(count++<maxNumBadSteps)){
					learnRate=learnRate/5.0;
					Hyperplane hii=new Hyperplane();
					hii.increment(ar.get(ar.size()-1));
					learnStep(queryMap,hii);//it modifies hii
					cur_ce=calculateLoss(splitIntoRankings(devData),hii);
					x=smallestCE-cur_ce;
//					System.out.println("\tCE = "+cur_ce+"\tx="+x+"\t"+learnRate);
					if(x>0){
						w=hii;
						ar.add(hii);
						smallestCE=cur_ce;
					}
				}
			}else{
				outcount=0;
			}
			pc.progress();
		}
		pc.finished();
//		System.out.println(w);
		return ar.get(ar.size()-1);
	}

	private void learnStep(Map<String,List<Example>> queryMap,Hyperplane w){
		for(Iterator<String> i=queryMap.keySet().iterator();i.hasNext();){
			String subpop=i.next();
			List<Example> ranking=queryMap.get(subpop);
			batchTrainSubPop(w,ranking);
		}
	}

	// return the number of times h has been updated
	private void batchTrainSubPop(Hyperplane w,List<Example> ranking){
		//initialize normalizers and create prob distributions 
		initialize(ranking,w);

		//compute gradient: deltaW(m)
		Hyperplane deltaW=calculateGradient(ranking);

		//update hypothesis: w = w - deltaW*learningRate
		w.increment(deltaW,-learnRate);
	}

	/**
	 * calculates equation (6) from paper.
	 * @param list
	 * @return
	 */
	private Hyperplane calculateGradient(List<Example> list){
		Hyperplane hyp=new Hyperplane();
		//calculates first term
		for(int i=0;i<list.size();i++){
			Instance ins=list.get(i);
			//for each feature in this example
			for(Iterator<Feature> loop=ins.featureIterator();loop.hasNext();){
				Feature f=loop.next();
				double term1=py[i]*ins.getWeight(f);//first term of derivative
				hyp.increment(f,-term1);
				double term2=pz[i]*ins.getWeight(f);//second term of derivative
				hyp.increment(f,term2);
			}
		}
		return hyp;
	}

	/**
	 * Calculates Equation (3): cross entropy between a "base" distribution and another one.
	 */
	public double crossEntropy(double[] base,double[] b){
		if(base.length!=b.length){
			throw new IllegalArgumentException(
					"Probability distributions of different sizes!");
		}
		double sum=0;
		for(int i=0;i<base.length;i++){
			sum+=base[i]*Math.log(b[i]);
		}
		return -sum;
	}

	/**
	 * Initialize probability distributions.
	 * @param list
	 * @param w
	 */
	private void initialize(List<Example> list,Hyperplane w){
		//create new probability distribution
		pz=new double[list.size()];
		py=new double[list.size()];
		double sumY=0,sumZ=0; //normalizers
		for(int i=0;i<list.size();i++){
			Example ex=list.get(i);
			if(ex.getLabel().isPositive())
				sumY+=Math.exp(RELEVANT);
			else
				sumY+=Math.exp(NON_RELEVANT);
			sumZ+=Math.exp(w.score(ex));
		}
		for(int i=0;i<list.size();i++){
			Example ex=list.get(i);
			double tmp=
					(ex.getLabel().isPositive())?Math.exp(RELEVANT):Math
							.exp(NON_RELEVANT);
			;
			py[i]=tmp/sumY;
			pz[i]=Math.exp(w.score(ex))/sumZ;
		}
	}

	public void setLearnRate(){
		learnRate=INITLearnRate;
	}

	/*
	 * Separates 20% of the data for development tests
	 */
	private Dataset separateDevData(Dataset data){
		Dataset.Split split=data.split(new CrossValSplitter<Example>(5));
		devData=split.getTest(1);
		return split.getTrain(1);
	}

	/*
	 * returns the loss over dataset: sum of cross-entropies
	 */
	double calculateLoss(Map<String,List<Example>> queryMap,Hyperplane w){
		double ce=0;
//		selfEntropy = 0.0;
		for(Iterator<String> i=queryMap.keySet().iterator();i.hasNext();){
			String subpop=i.next();
			List<Example> ranking=queryMap.get(subpop);
			initialize(ranking,w);
			ce+=crossEntropy(py,pz);
//			selfEntropy += crossEntropy(py,py);
		}
		return ce;
	}
}