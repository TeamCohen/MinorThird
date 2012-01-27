package edu.cmu.minorthird.classify.algorithms.trees;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;

/** Implements a fast version of the simplest RandomTree learner possible. Avoids memory allocation
 * wheverever possible, with resulting speed up of about 30% 
 *
 * @author Alexander Friedman
 */

public class FastRandomTreeLearner extends RandomTreeLearner{

	private static Logger log=Logger.getLogger(FastRandomTreeLearner.class);

//	private double epsilon=0.001;

	private Random rand;

	// How many random vars to check for a split
	public int subsetSize;

	public FastRandomTreeLearner setRandomSeed(long seed){
		rand=new Random(seed);
		return this;
	}

	public FastRandomTreeLearner(){
		rand=new Random();
	}

	public FastRandomTreeLearner setSubsetSize(int subsetSize){
		this.subsetSize=subsetSize;
		return this;
	}

	@Override
	public Classifier batchTrain(List<Example> dataset,Vector<Feature> allFeatures){

		// COPIED FROM DecisionTreeLearner
//		epsilon=0.5/dataset.size();

		Classifier c=
				batchTrain(new Vector<Example>(dataset),0,allFeatures,allFeatures
						.size()-1,0,dataset.size());
		log.info("built tree: "+c);
		return c;
	}

	@Override
	public Classifier batchTrain(Dataset dataset){
		List<Example> newData=new LinkedList<Example>();
		for(Iterator<Example> it=dataset.iterator();it.hasNext();){
			newData.add(it.next());
		}

		return batchTrain(newData,RandomForests.getDatasetFeatures(dataset));
	}

	public Classifier batchTrain(Vector<Example> dataset,int depth,
			Vector<Feature> unusedFeatures,int lastFeature,int from,int to){
		CompactDecisionTree tree=new CompactDecisionTree();
		tree.setRoot(batchTrain(dataset,depth,unusedFeatures,lastFeature,from,to,
				tree));
		tree.compactStorage();
		return tree;

	}

	private Object[] getSplit(Vector<Example> dataset,int from,int to,
			Vector<Feature> unusedFeatures,int lastFeature,double posWeight,
			double negWeight){

		HashMap<Feature,Integer> features=new HashMap<Feature,Integer>();
		HashMap<Feature,NumericFeatureStats> stats=
				new HashMap<Feature,NumericFeatureStats>();

		// Choose some features
		for(int i=0;i<subsetSize;i++){
			int featureIndex=(int)Math.floor(rand.nextDouble()*lastFeature);
			Feature f=unusedFeatures.get(featureIndex);
			features.put(f,featureIndex);
			stats.put(f,new NumericFeatureStats());
		}

		// update the feature stats
		for(int i=from;i<to;i++){
			Example example=dataset.get(i);
			for(Feature f:features.keySet()){
				NumericFeatureStats s=stats.get(f);
				s.update(example,example.getWeight(f));
			}
		}

		double bestValue=Double.MAX_VALUE;
		double bestThreshold=-9999;

		Feature bestFeature=null;
		int bestFeatureIndex=-1;

		for(Feature f:features.keySet()){
			NumericFeatureStats s=stats.get(f);
			double v=s.value(posWeight,negWeight);
			double th=s.getBestThreshold();
			if(v<bestValue){
				bestValue=v;
				bestFeature=f;
				bestThreshold=th;
				bestFeatureIndex=features.get(f);
			}
		}

		// just pick any feature - it will be thrown out if it doesn't split the tree
		if(bestFeature==null){
			bestFeature=features.keySet().iterator().next();
			bestFeatureIndex=features.get(bestFeature);
		}

		return new Object[]{bestFeatureIndex,bestThreshold};
	}

//	private static int maxDepth=0;
//
//	private void check_depth(int depth){
//		if(depth>maxDepth){
//			maxDepth++;
//			log.warn("Max depth now: "+maxDepth);
//		}
//	}

	// return the index of the node created
	public int batchTrain(Vector<Example> dataset,int depth,
			Vector<Feature> unusedFeatures,int lastFeature,int from,int to,
			CompactDecisionTree tree){
		// check_depth(depth);

		double posWeight=0,negWeight=0;
		for(int i=from;i<to;i++){
			Example example=dataset.get(i);
			if(example.getLabel().numericLabel()>0)
				posWeight+=example.getWeight();
			else
				negWeight+=example.getWeight();
		}

		// We want our votes to be weighted, so lets try this instead:

		//       if      (posWeight  > negWeight) weight = posWeight;
		//       else                             weight = - negWeight;
		double weight=posWeight-negWeight;

		if((negWeight==0)||(posWeight==0)||lastFeature<0||depth>500){
			return tree.addLeafNode(weight);
//       return tree.addLeafNode( 0.5 * Math.log( (posWeight+epsilon)/(negWeight+epsilon)));
//       return tree.addLeafNode(Math.log( (posWeight+epsilon)/(negWeight+epsilon)));
		}

		Object result[]=
				getSplit(dataset,from,to,unusedFeatures,lastFeature,posWeight,negWeight);

		int featureIndex=(Integer)result[0];
		double bestThreshold=(Double)result[1];
		Feature bestFeature=unusedFeatures.get(featureIndex);
		if(depth>1000){
			log.warn("Pos Weight: "+posWeight);
			log.warn("Neg Weight: "+negWeight);
			log.warn("last Feature: "+lastFeature);
			log.warn("from: "+from+", to: "+to);
			log.warn("split on: "+bestFeature+" with threshold "+bestThreshold);
		}

		// Sort the data elements in place instead of creating two sub-arrays.
		// This avoids un-nessisary memory allocation and copying
		int storeIndex=from;
		Example tmp;
		for(int i=from;i<to;i++){
			if(dataset.get(i).getWeight(bestFeature)>=bestThreshold){
				tmp=dataset.get(storeIndex);
				dataset.setElementAt(dataset.get(i),storeIndex);
				dataset.setElementAt(tmp,i);
				storeIndex++;
			}
		}

		if(depth>1000){
			log.warn("Pos Weight: "+posWeight);
			log.warn("Neg Weight: "+negWeight);
			log.warn("last Feature: "+lastFeature);
			log.warn("from: "+from+", to: "+to);
			log.warn("storeIndex: "+storeIndex);
			log.warn("split on: "+bestFeature+" with threshold "+bestThreshold);
		}

		// swap the best feature with the last feature, again to avoid memory allocation for the
		// features. This optimization does almost nothing in terms of performance in practice
		unusedFeatures.setElementAt(unusedFeatures.get(lastFeature),featureIndex);
		unusedFeatures.setElementAt(bestFeature,lastFeature);

		// If this feature didn't split anything, recur, and don't build a useless node
		if(storeIndex==from||storeIndex==to){
			log.debug("didn't split data with this feature");
			return batchTrain(dataset,depth+1,unusedFeatures,lastFeature-1,from,to,
					tree);
		}

		//  This is 'without pruning' - features can be reselected
		int trueBranch=
				batchTrain(dataset,depth+1,unusedFeatures,lastFeature,from,storeIndex,
						tree);
		int falseBranch=
				batchTrain(dataset,depth+1,unusedFeatures,lastFeature,storeIndex,to,
						tree);

		return tree.addInternalNode(bestFeature,bestThreshold,trueBranch,
				falseBranch);
	}

	private static class BinaryFeatureStats{

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

		/*
		// value of splitting on this feature
		public double value(double totalPosWeight,double totalNegWeight){
			return schapireSingerValue(pos,neg,totalPosWeight,totalNegWeight);
			//return entropy(pos,neg,totalPosWeight,totalNegWeight);
		}
		*/

		@Override
		public String toString(){
			return "[pos:"+pos+" neg:"+neg+"]";
		}
	}

	private static class NumericFeatureStats{

		// maps feature values to BinaryFeatureStats for examples with exactly that value
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
			// incrementally track weight of pos,neg examples less than/greater than the threshold
			double posGT=totalPosWeight;
			double negGT=totalNegWeight;
//			double total=posGT+negGT;
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
					//double value = entropy( posGT, negGT, totalPosWeight, totalNegWeight );
					// // Gini value -- not sure if this is actually correct or not
					// double value =
//             1 - ((posGT * posGT) + (negGT * negGT)) / (total * total);
					//if(DEBUG)log.debug("last:"+lastKey+" key:"+key+" th:"+threshold+" p:"+posGT+" n:"+negGT+" v:"+value);
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
//		//return 2 * ( Math.sqrt((wp1)*(wp0)) + Math.sqrt((wn1)*(wn0)) );
//		return -w11*Math.log(w11)-w10*Math.log(w10)-w01*Math.log(w01)-w00*
//				Math.log(w00);
//	}

}
