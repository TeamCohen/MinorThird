package edu.cmu.minorthird.classify.algorithms.trees;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;

/** 
 * Implements a fast version of the simplest RandomTree learner possible. Avoids memory allocation
 * wheverever possible, with resulting speed up of about 30% 
 *
 * @author Alexander Friedman
 */

public class FastRandomTreeLearner extends RandomTreeLearner{

	private static Logger log=Logger.getLogger(FastRandomTreeLearner.class);

//	private static final boolean DEBUG=
//			log.getEffectiveLevel().isGreaterOrEqual(Level.DEBUG);

	public Classifier batchTrain(List<Example> dataset,Vector<Feature> allFeatures){

		Classifier c=
				batchTrain(new Vector<Example>(dataset),0,allFeatures,allFeatures
						.size()-1,0,dataset.size());
		log.info("built tree: "+c);
		return c;
	}

	public Classifier batchTrain(Dataset dataset){
		List<Example> newData=new LinkedList<Example>();
		for(Iterator<Example> it=dataset.iterator();it.hasNext();){
			newData.add(it.next());
		}

		return batchTrain(newData,RandomForests.getDatasetFeatures(dataset));
	}

	public DecisionTree batchTrain(Vector<Example> dataset,int depth,
			Vector<Feature> unusedFeatures,int lastFeature,int from,int to){

		int featureIndex=(int)Math.floor(Math.random()*lastFeature);
		Feature bestFeature=featureIndex>=0?unusedFeatures.get(featureIndex):null;

		// now get the min and max values of this feature
		double minValue=Double.MAX_VALUE;
		double maxValue=Double.MIN_VALUE;

		double posWeight=0,negWeight=0;

		double bestThreshold=.5;

		for(int i=from;i<to;i++){
			Example example=dataset.get(i);

			if(bestFeature!=null){
				double val=example.getWeight(bestFeature);
				if(val<minValue)
					minValue=val;
				if(val>maxValue)
					maxValue=val;
			}

			if(example.getLabel().numericLabel()>0)
				posWeight+=example.getWeight();
			else
				negWeight+=example.getWeight();
		}

		bestThreshold=Math.random()*(maxValue-minValue)+minValue;

		log.debug("build (sub)tree with posWeight: "+posWeight+" negWeight: "+
				negWeight);

		// Random Forests will use voting to determine the outcome, so we will use +/- 1 for the weights
		if((negWeight==0)||(posWeight==0)||lastFeature<0){
			int weight=0;

			if(posWeight>negWeight)
				weight=1;
			else if(posWeight==negWeight)
				weight=0;
			else
				weight=-1;

			return new DecisionTree.Leaf(weight);
		}

		log.debug("split on: "+bestFeature+" with threshold "+bestThreshold);

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

		// swap the best feature with the last feature, again to avoid memory allocation for the 
		// features. This optimization does almost nothing in terms of performance in practice
		unusedFeatures.setElementAt(unusedFeatures.get(lastFeature),featureIndex);
		unusedFeatures.setElementAt(bestFeature,lastFeature);

		// If this feature didn't split anything, recur, and don't build a useless node
		if(storeIndex==from||storeIndex==to){
			log.debug("didn't split data with this feature");
			return batchTrain(dataset,depth,unusedFeatures,lastFeature-1,from,to);
		}

		DecisionTree trueBranch=
				batchTrain(dataset,depth+1,unusedFeatures,lastFeature-1,from,storeIndex);
		DecisionTree falseBranch=
				batchTrain(dataset,depth+1,unusedFeatures,lastFeature-1,storeIndex,to);

		return new DecisionTree.InternalNode(bestFeature,bestThreshold,trueBranch,
				falseBranch);
	}
}
