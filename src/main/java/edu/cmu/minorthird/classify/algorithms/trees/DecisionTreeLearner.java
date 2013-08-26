package edu.cmu.minorthird.classify.algorithms.trees;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.BatchBinaryClassifierLearner;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;

/**
 * A simple decision tree learning algorithm.
 * 
 * @author William Cohen
 */

public class DecisionTreeLearner extends BatchBinaryClassifierLearner{

	private static Logger log=Logger.getLogger(DecisionTreeLearner.class);

	private static final boolean DEBUG=
			log.getEffectiveLevel().isGreaterOrEqual(Level.DEBUG);

	private int maxDepth=5;

	private int minSplitCount=2;

	private double epsilon=0.001;

	/**
	 * @param maxDepth
	 *          maximum depth of the tree
	 * @param minSplitCount
	 *          minimum number of examples to have before considering a split
	 */
	public DecisionTreeLearner(int maxDepth,int minSplitCount){
		this.maxDepth=maxDepth;
		this.minSplitCount=minSplitCount;
	}

	public DecisionTreeLearner(){
		this(5,2);
	}

	public int getMaxDepth(){
		return maxDepth;
	}

	public void setMaxDepth(int d){
		this.maxDepth=d;
	}

	public int getMinSplitCount(){
		return minSplitCount;
	}

	public void setMinSplitCount(int c){
		this.minSplitCount=c;
	}

	@Override
	public Classifier batchTrain(Dataset dataset){
		epsilon=0.5/dataset.size();
		Classifier c=batchTrain(dataset,0);
		log.info("built tree: "+c);
		return c;
	}

	/**
	 * The real learning algorithm.
	 * 
	 * Top-down decision tree learning, no pruning except that required by
	 * maxDepth and minSplitCount, guided by the criterion for weak learners
	 * proposed by Schapire and Singer in Improved boosting algorithms using
	 * confidence-rated predictions. Machine Learning, 37(3):297-336, 1999.
	 */

	private DecisionTree batchTrain(Dataset dataset,int depth){
		// see how put the dataset is
		double posWeight=0,negWeight=0;
		for(Iterator<Example> i=dataset.iterator();i.hasNext();){
			Example example=i.next();
			if(example.getLabel().numericLabel()>0)
				posWeight+=example.getWeight();
			else
				negWeight+=example.getWeight();
		}

		log.info("build (sub)tree with posWeight: "+posWeight+" negWeight: "+
				negWeight);

		// if (DEBUG) log.debug(dataset);

		if((dataset.size()<minSplitCount)||(depth>=maxDepth)||(negWeight==0)||
				(posWeight==0)){

			log.debug("leaf");
			return new DecisionTree.Leaf(0.5*Math.log((posWeight+epsilon)/
					(negWeight+epsilon)));

		}else{

			// measure value of each possible split

			double totalPosWeight=0.0;
			double totalNegWeight=0.0;
			Map<Feature,BinaryFeatureStats> binaryMap=new TreeMap<Feature,BinaryFeatureStats>();
			Map<Feature,NumericFeatureStats> numericMap=new TreeMap<Feature,NumericFeatureStats>();
			for(Iterator<Example> i=dataset.iterator();i.hasNext();){
				Example example=i.next();
				if(example.getLabel().numericLabel()>0)
					totalPosWeight+=example.getWeight();
				else
					totalNegWeight+=example.getWeight();
				for(Iterator<Feature> j=example.binaryFeatureIterator();j.hasNext();){
					Feature f=j.next();
					BinaryFeatureStats s=binaryMap.get(f);
					if(s==null)
						binaryMap.put(f,s=new BinaryFeatureStats());
					s.update(example);
				}
				for(Iterator<Feature> j=example.numericFeatureIterator();j.hasNext();){
					Feature f=j.next();
					NumericFeatureStats s=numericMap.get(f);
					if(s==null)
						numericMap.put(f,s=new NumericFeatureStats());
					s.update(example,example.getWeight(f));
				}
			}

			// pick the best feature
			double bestValue=Double.MAX_VALUE;
			double bestThreshold=-9999;
			Feature bestFeature=null;
			for(Iterator<Feature> k=binaryMap.keySet().iterator();k.hasNext();){
				Feature f=k.next();
				BinaryFeatureStats s=binaryMap.get(f);
				double v=s.value(totalPosWeight,totalNegWeight);
				if(DEBUG)
					log.debug("feature "+f+" stats: "+s+" val: "+v);
				if(v<bestValue){
					bestValue=v;
					bestFeature=f;
					bestThreshold=0.5;
					if(DEBUG)
						log.debug(" ==> BEST");
				}
			}
			for(Iterator<Feature> k=numericMap.keySet().iterator();k.hasNext();){
				Feature f=k.next();
				NumericFeatureStats s=numericMap.get(f);
				double v=s.value(totalPosWeight,totalNegWeight);
				double th=s.getBestThreshold();
				if(DEBUG)
					log.debug("feature "+f+"<"+th+" stats: "+s+" val: "+v);
				if(v<bestValue){
					bestValue=v;
					bestFeature=f;
					bestThreshold=th;
					if(DEBUG)
						log.debug(" ==> BEST");
				}
			}

			if(bestFeature==null){
				// no useful split found
				log.debug("no good split found - leaf");
				return new DecisionTree.Leaf(0.5*Math.log((posWeight+epsilon)/
						(negWeight+epsilon)));
			}

			log.info("split on "+bestFeature+">"+bestThreshold);

			// split the data. No need to compress the examples again so
			// call the appropriate add method in Dataset.
			Dataset trueData=new BasicDataset();
			Dataset falseData=new BasicDataset();
			for(Iterator<Example> i=dataset.iterator();i.hasNext();){
				Example example=i.next();
				if(example.getWeight(bestFeature)>bestThreshold){
					trueData.add(example,false);
				}else{
					falseData.add(example,false);
				}
			}

			// recurse to build the subtrees
			DecisionTree trueBranch=batchTrain(trueData,depth+1);
			DecisionTree falseBranch=batchTrain(falseData,depth+1);
			return new DecisionTree.InternalNode(bestFeature,bestThreshold,
					trueBranch,falseBranch);

		}
	}

	/**
	 * Track the number of pos/neg examples such that the value of this feature is
	 * non-zero.
	 */
	private class BinaryFeatureStats{

		private double pos=0,neg=0;

		private BinaryFeatureStats(){
			;
		}

		// update stats for this example, assuming it's weight is nonzero
		public void update(Example example){
			if(example.getLabel().numericLabel()>0)
				pos+=example.getWeight();
			else
				neg+=example.getWeight();
		}

		// value of splitting on this feature
		public double value(double totalPosWeight,double totalNegWeight){
			return schapireSingerValue(pos,neg,totalPosWeight,totalNegWeight);
			// return entropy(pos,neg,totalPosWeight,totalNegWeight);
		}

		@Override
		public String toString(){
			return "[pos:"+pos+" neg:"+neg+"]";
		}
	}

	/**
	 * Track the pos/neg examples such that the value of this feature is non-zero,
	 * together with the associated feature weight for these examples.
	 */
	private class NumericFeatureStats{

		// maps feature values to BinaryFeatureStats for examples with exactly that
		// value
		private TreeMap<Double,BinaryFeatureStats> map;

		// total pos, neg weight of examples with non-zero weights
		private double posNonZero=0;

		private double negNonZero=0;

		private double bestThreshold;

		private double bestThresholdValue;

		public NumericFeatureStats(){
			map=new TreeMap<Double,BinaryFeatureStats>();
		}

		// update stats for this example
		public void update(Example example,double featureWeight){
			Double key=new Double(featureWeight);
			BinaryFeatureStats bfs=map.get(key);
			if(bfs==null)
				map.put(key,bfs=new BinaryFeatureStats());
			bfs.update(example);
			if(example.getLabel().numericLabel()>0)
				posNonZero+=example.getWeight();
			else
				negNonZero+=example.getWeight();
		}

		// value of this split, optimized over all thresholds
		public double value(double totalPosWeight,double totalNegWeight){
			// insert keys for the zero-weight case, if needed
			if(totalPosWeight+totalPosWeight>posNonZero+negNonZero){
				BinaryFeatureStats s=new BinaryFeatureStats();
				s.pos=totalPosWeight-posNonZero;
				s.neg=totalNegWeight-negNonZero;
				map.put(new Double(0),s);
			}

			Double lastKey=null;
			// incrementally track weight of pos,neg examples less than/greater than
			// the threshold
			double posGT=totalPosWeight;
			double negGT=totalNegWeight;
			bestThresholdValue=Double.MAX_VALUE;
			for(Iterator<Double> i=map.keySet().iterator();i.hasNext();){
				Double key=i.next();
				double threshold=-1; // initialize to something lt zero
				if(lastKey!=null){
					// the threshold is half-way between this value and the previous value
					// note that it will be vacuous the first time around
					threshold=
							lastKey.doubleValue()+0.5*
									(key.doubleValue()-lastKey.doubleValue());
					double value=
							schapireSingerValue(posGT,negGT,totalPosWeight,totalNegWeight);
					// double value = entropy( posGT, negGT, totalPosWeight,
					// totalNegWeight );
					// if(DEBUG)log.debug("last:"+lastKey+" key:"+key+" th:"+threshold+"
					// p:"+posGT+" n:"+negGT+" v:"+value);
					if(value<bestThresholdValue){
						bestThreshold=threshold;
						bestThresholdValue=value;
					}
				}
				lastKey=key;
				// update counts
				BinaryFeatureStats bfs=map.get(key);
				posGT-=bfs.pos;
				negGT-=bfs.neg;
			}
			return bestThresholdValue;
		}

		// threshold that gives the optimized value
		double getBestThreshold(){
			return bestThreshold;
		}

		@Override
		public String toString(){
			return "[pos: "+posNonZero+" neg: "+negNonZero+" map: "+map+"]";
		}
	}

	private static double schapireSingerValue(double pos,double neg,
			double totalPos,double totalNeg){
		double totalWeight=totalPos+totalNeg;
		// wpj = S&S's W_+^j, wnj = W_-^j, for j=0,1
		// block j=1 is condition true (pos,neg weights)
		// block j=0 is condition false (totalPos-pos,totalNeg-neg weights)
		// W_+ is positive class (pos,totalPos-pos), W_- is negative
		double wp1=pos/totalWeight;
		double wp0=(totalPos-pos)/totalWeight;
		double wn1=neg/totalWeight;
		double wn0=(totalNeg-neg)/totalWeight;
		log.debug("pos, neg, total = "+pos+", "+neg+", "+totalWeight);
		log.debug("wp1,wp0,wn1,wn0 = "+wp1+","+wp0+","+wn1+","+wn0);
		return 2*(Math.sqrt(wp1*wn1)+Math.sqrt(wp0*wn0));
	}

//	private static double entropy(double pos,double neg,double totalPos,
//			double totalNeg){
//		// wij = feature=i,class=j
//		double tot=totalPos+totalNeg;
//		double epsilon=0.1/tot;
//		double w11=pos/tot+epsilon;
//		double w10=neg/tot+epsilon;
//		double w01=(tot-pos)/tot+epsilon;
//		double w00=(tot-neg)/tot+epsilon;
//		log.debug("pos, neg, total = "+pos+", "+neg+", "+tot);
//		log.debug("w11,w10,w01,w00 = "+w11+","+w10+","+w01+","+w00);
//		// return 2 * ( Math.sqrt((wp1)*(wp0)) + Math.sqrt((wn1)*(wn0)) );
//		return -w11*Math.log(w11)-w10*Math.log(w10)-w01*Math.log(w01)-w00*
//				Math.log(w00);
//	}

}
