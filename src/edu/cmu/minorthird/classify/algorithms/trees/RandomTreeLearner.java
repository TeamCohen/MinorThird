package edu.cmu.minorthird.classify.algorithms.trees;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BatchBinaryClassifierLearner;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;

/** 
 * Implement a random decision tree to be used in the random forest learner.
 * Implements two tree splitters. Default one splits on random features. BestOfN splits on the b
 * best of N randomly chosen features.
 *
 * @author Alexander Friedman
 */

// TODO: ADD a few more tree splitting routines.
//  Try: Linear combination of m random features
public class RandomTreeLearner extends BatchBinaryClassifierLearner{

	private static Logger log=Logger.getLogger(RandomTreeLearner.class);

	// The builder that we will use to construct the tree
	private TreeSplitter splitter;

	// The interface used to split trees
	public interface TreeSplitter{

		// Yep, lets avoid that whole type system thing entirely
		// return [feature x threshold(double)] ||
		//        [feature x threshold(double) x trueset(dataset) x falseset(dataset)]
		// if your splitter is doing something clever/expensive/etc
		public Object[] getSplit(List<Example> dataset,int depth,
				Vector<Feature> unusedFeatures);
	}

	// Selects one random feature from the unused features, then selects a random split point
	// between the min and max values
	public static class RandomTreeSplitter implements TreeSplitter{

		@Override
		public Object[] getSplit(List<Example> dataset,int depth,
				Vector<Feature> unusedFeatures){

			// Now choose a random feature from the unused features (and consider it the best feature)
			int featureIndex=(int)Math.floor(Math.random()*unusedFeatures.size());
			// This is silly - we should be using Generics
			Feature bestFeature=unusedFeatures.get(featureIndex);

			// now get the min and max values of this feature
			double minValue=Double.MAX_VALUE;
			double maxValue=Double.MIN_VALUE;
			for(Example example:dataset){
				double val=example.getWeight(bestFeature);
				if(val<minValue)
					minValue=val;
				if(val>maxValue)
					maxValue=val;
			}

			double bestThreshold=Math.random()*(maxValue-minValue)+minValue;

			return new Object[]{bestFeature,new Double(bestThreshold)};
		}
	}

	public static class BestOfNRandomTreeSplitter implements TreeSplitter{

		public BestOfNRandomTreeSplitter(int fc){
			featureCount=fc;
		}

		int featureCount=1;

		@Override
		public Object[] getSplit(List<Example> dataset,int depth,
				Vector<Feature> unusedFeatures){

			Feature bestFeature=null;
			double bestEntropy=Double.MIN_VALUE;
			double bestThreshold=0;
			List<Example> bestTrueData=null;
			List<Example> bestFalseData=null;
			for(int i=0;i<featureCount&&i<unusedFeatures.size();i++){
				// Now choose a random feature from the unused features (and consider it the best feature)
				int featureIndex=(int)Math.floor(Math.random()*unusedFeatures.size());
				Feature f=unusedFeatures.get(featureIndex);
				List<Example> trueData=new LinkedList<Example>();
				List<Example> falseData=new LinkedList<Example>();

				// FIXME: copied from code above. should make a function to pick thresholds.
				double minValue=Double.MAX_VALUE;
				double maxValue=Double.MIN_VALUE;
				for(Example example:dataset){
					double val=example.getWeight(f);
					if(val<minValue)
						minValue=val;
					if(val>maxValue)
						maxValue=val;
				}

				double threshold=Math.random()*(maxValue-minValue)+minValue;

				for(Example example:dataset){
					if(example.getWeight(f)>=threshold)
						trueData.add(example);
					else
						falseData.add(example);
				}

				double i_gain=
						entropy(trueData.size(),falseData.size(),trueData.size(),falseData
								.size());
				// **FIXME** think about this entropy thing!
				if(i_gain>bestEntropy){
					bestEntropy=i_gain;
					bestTrueData=trueData;
					bestFalseData=falseData;
					bestFeature=f;
					bestThreshold=threshold;
				}
			}

			return new Object[]{bestFeature,new Double(bestThreshold),bestTrueData,
					bestFalseData};
		}
	}

	public RandomTreeLearner(){
		splitter=new RandomTreeSplitter();
	}

	public RandomTreeLearner(TreeSplitter b){
		splitter=b;
	}

	public Classifier batchTrain(List<Example> dataset,Vector<Feature> allFeatures){
		Classifier c=batchTrain(dataset,0,allFeatures);
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

	public DecisionTree batchTrain(List<Example> dataset,int depth,
			Vector<Feature> unusedFeatures){
		double posWeight=0,negWeight=0;
		for(Example example:dataset){
			if(example.getLabel().numericLabel()>0)
				posWeight+=example.getWeight();
			else
				negWeight+=example.getWeight();
		}

		log.debug("build (sub)tree with posWeight: "+posWeight+" negWeight: "+
				negWeight);

		// Random Forests will use voting to determine the outcome, 
		// so we will use +/- 1 for the weights, or 0 if unknown
		if((negWeight==0)||(posWeight==0)||unusedFeatures.size()==0){
			int weight=0;

			if(posWeight>negWeight)
				weight=1;
			else if(posWeight==negWeight)
				weight=0;
			else
				weight=-1;

			log.debug("leaf");
			return new DecisionTree.Leaf(weight);
		}

		Object[] result=splitter.getSplit(dataset,depth,unusedFeatures);

		Feature bestFeature=(Feature)result[0];
		double bestThreshold=((Double)result[1]).doubleValue(); // java sucks

		List<Example> trueData=null;
		List<Example> falseData=null;

		if(result.length==4){ // The splitter did the splitting, just use the result
			trueData=(List<Example>)result[2]; // Sorry Java, you need a better type system
			falseData=(List<Example>)result[3];
		}else{
			trueData=new LinkedList<Example>();
			falseData=new LinkedList<Example>();

			for(Example example:dataset){
				if(example.getWeight(bestFeature)>=bestThreshold)
					trueData.add(example);
				else
					falseData.add(example);
			}
		}

		log.debug("split on: "+bestFeature+" with threshold "+bestThreshold);

		log.debug("trueData size: "+trueData.size()+" falseData size: "+
				falseData.size());
		Vector<Feature> newUnusedFeatures=new Vector<Feature>(unusedFeatures);
		newUnusedFeatures.removeElement(bestFeature);

		// If this feature didn't split anything, recur, and don't build a useless node
		if(falseData.size()==0||trueData.size()==0){
			log.debug("didn't split data with this feature");
			return batchTrain(dataset,depth,newUnusedFeatures);
		}

		// recur to build the subtrees
		DecisionTree trueBranch=batchTrain(trueData,depth+1,newUnusedFeatures);
		DecisionTree falseBranch=batchTrain(falseData,depth+1,newUnusedFeatures);

		return new DecisionTree.InternalNode(bestFeature,bestThreshold,trueBranch,
				falseBranch);

	}

	// FIXME -- these are copied directly from DecisionTreeLearner
	// I would like to call these as utility functions, but they are private :~(

//	private static double schapireSingerValue(double pos,double neg,
//			double totalPos,double totalNeg){
//		double totalWeight=totalPos+totalNeg;
//		// wpj = S&S's W_+^j, wnj = W_-^j, for j=0,1
//		// block j=1 is condition true (pos,neg weights)
//		// block j=0 is condition false (totalPos-pos,totalNeg-neg weights)
//		// W_+ is positive class (pos,totalPos-pos), W_- is negative
//		double wp1=pos/totalWeight;
//		double wp0=(totalPos-pos)/totalWeight;
//		double wn1=neg/totalWeight;
//		double wn0=(totalNeg-neg)/totalWeight;
//		log.debug("pos, neg, total = "+pos+", "+neg+", "+totalWeight);
//		log.debug("wp1,wp0,wn1,wn0 = "+wp1+","+wp0+","+wn1+","+wn0);
//		return 2*(Math.sqrt(wp1*wn1)+Math.sqrt(wp0*wn0));
//	}

	private static double entropy(double pos,double neg,double totalPos,
			double totalNeg){
		// wij = feature=i,class=j
		double tot=totalPos+totalNeg;
		double epsilon=0.1/tot;
		double w11=pos/tot+epsilon;
		double w10=neg/tot+epsilon;
		double w01=(tot-pos)/tot+epsilon;
		double w00=(tot-neg)/tot+epsilon;
		log.debug("pos, neg, total = "+pos+", "+neg+", "+tot);
		log.debug("w11,w10,w01,w00 = "+w11+","+w10+","+w01+","+w00);
		//return 2 * ( Math.sqrt((wp1)*(wp0)) + Math.sqrt((wn1)*(wn0)) );
		return -w11*Math.log(w11)-w10*Math.log(w10)-w01*Math.log(w01)-w00*
				Math.log(w00);
	}

}
