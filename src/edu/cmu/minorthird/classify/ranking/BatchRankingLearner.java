/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.cmu.minorthird.classify.BatchBinaryClassifierLearner;
import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Instance;

/**
 * Learns to rank sets of examples, rather than to classify individual examples.
 *
 * Data is presented to a BatchRankingLearner as an ordinary binary
 * dataset.  Examples from the same subpopulation are comparable, and
 * should be ranked so that positive examples have a higher score than
 * negative examples.
 *
 * @author William Cohen
 */

public abstract class BatchRankingLearner extends BatchBinaryClassifierLearner{

	/** Sort a dataset into 'rankings'.  Each ranking is a List of
	 * Examples such that all positive examples in the list should be
	 * ranked above all negative examples. Returns a map so that
	 * map.get(key) is an ArrayList of examples.
	 */
	static public Map<String,List<Example>> splitIntoRankings(Dataset data){
		Map<String,List<Example>> map=new HashMap<String,List<Example>>();
		for(Iterator<Example> i=data.iterator();i.hasNext();){
			Example ex=i.next();
			List<Example> list=map.get(ex.getSubpopulationId());
			if(list==null)
				map.put(ex.getSubpopulationId(),(list=new ArrayList<Example>()));
			list.add(ex);
		}
		return map;
	}

	/**
	 * Split a Map output by splitIntoRankings into lists that contain
	 * exactly one positive example each.
	 */
	static public Map<String,List<Example>> listsWithOneExampleEach(Map<String,List<Example>> rankingLists){
		Map<String,List<Example>> map1=new HashMap<String,List<Example>>();
		for(Iterator<String> i=rankingLists.keySet().iterator();i.hasNext();){
			String key=i.next();
			List<Example> posExamples=new ArrayList<Example>();
			List<Example> negExamples=new ArrayList<Example>();
			List<Example> ranking=rankingLists.get(key);
			for(int j=0;j<ranking.size();j++){
				Example exi=ranking.get(j);
				if(exi.getLabel().isPositive()){
					posExamples.add(exi);
				}else{
					negExamples.add(exi);
				}
			}
			for(int j=0;j<posExamples.size();j++){
				Example exi=posExamples.get(j);
				List<Example> ranking1=new ArrayList<Example>();
				ranking1.addAll(negExamples);
				ranking1.add(exi);
				map1.put(key+"."+j,ranking1);
			}
		}
		return map1;
	}

	/** Sort a List of Instances by score according to the classifier. */
	static public void sortByScore(final BinaryClassifier c,List<Example> data){
		Collections.sort(data,new Comparator<Example>(){
			@Override
			public int compare(Example instA,Example instB){
				double diff=c.score(instB)-c.score(instA);
				int cmp=diff>0?+1:(diff<0?-1:0);
				if(cmp!=0)
					return cmp;
				// rather than be random, sort negative examples
				// above positive if scores are the same
				if((instA instanceof Example)&&(instB instanceof Example)){
					Example exA=instA;
					Example exB=instB;
					return (int)(exA.getLabel().numericLabel()-exB.getLabel()
							.numericLabel());
				}else{
					return 0;
				}
			}
		});
	}
}
